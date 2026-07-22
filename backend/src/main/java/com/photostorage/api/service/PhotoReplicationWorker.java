package com.photostorage.api.service;

import com.photostorage.api.model.Photo;
import com.photostorage.api.model.ReplicationJob;
import com.photostorage.api.repository.ReplicationJobRepository;
import com.photostorage.api.storage.PhotoStorageRegistry;
import com.photostorage.api.storage.PhotoStorageService;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
    name = "storage.replication.enabled",
    havingValue = "true"
)
public class PhotoReplicationWorker {

    private final ReplicationJobRepository replicationJobRepository;
    private final PhotoStorageRegistry storageRegistry;

    public PhotoReplicationWorker(
        ReplicationJobRepository replicationJobRepository,
        PhotoStorageRegistry storageRegistry
    ) {
        this.replicationJobRepository = replicationJobRepository;
        this.storageRegistry = storageRegistry;
    }

    @Scheduled(
        fixedDelayString =
            "${storage.replication.poll-interval-ms:10000}"
    )
    @Transactional
    public void processReadyJobs() {
        List<ReplicationJob> jobs =
            replicationJobRepository.findReadyJobs(Instant.now());

        for (ReplicationJob job : jobs) {
            process(job);
        }
    }

    private void process(ReplicationJob job) {
        Photo photo = job.getPhoto();

        try {
            job.markProcessing();

            PhotoStorageService sourceStorage =
                storageRegistry.get(job.getSourceProvider());

            PhotoStorageService targetStorage =
                storageRegistry.get(job.getTargetProvider());

            Resource sourceObject =
                sourceStorage.load(photo.getObjectKey());

            byte[] content = sourceObject.getContentAsByteArray();

            targetStorage.storeCopy(
                photo.getObjectKey(),
                content,
                photo.getContentType()
            );

            photo.markReplicationCompleted();
            job.markCompleted();
        } catch (Exception exception) {
            photo.markReplicationFailed();
            job.markFailed(errorMessage(exception));
        }
    }

    private static String errorMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        return message.length() <= 1000
            ? message
            : message.substring(0, 1000);
    }
}