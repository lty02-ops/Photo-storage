package com.photostorage.api.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.photostorage.api.model.AppUser;
import com.photostorage.api.model.Photo;
import com.photostorage.api.model.ReplicationJob;
import com.photostorage.api.model.ReplicationJobStatus;
import com.photostorage.api.model.ReplicationStatus;
import com.photostorage.api.model.UserRole;
import com.photostorage.api.repository.ReplicationJobRepository;
import com.photostorage.api.storage.PhotoStorageRegistry;
import com.photostorage.api.storage.PhotoStorageService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

@ExtendWith(MockitoExtension.class)
class PhotoReplicationWorkerTest {

    @Mock
    private ReplicationJobRepository replicationJobRepository;

    @Mock
    private PhotoStorageRegistry storageRegistry;

    @Mock
    private PhotoStorageService sourceStorage;

    @Mock
    private PhotoStorageService targetStorage;

    private PhotoReplicationWorker worker;

    @BeforeEach
    void setUp() {
        worker = new PhotoReplicationWorker(
            replicationJobRepository,
            storageRegistry
        );
    }

    @Test
    void successfulReplicationCompletesPhotoAndJob()
        throws Exception {

        byte[] content = new byte[] { 1, 2, 3 };
        Photo photo = testPhoto();
        ReplicationJob job = testJob(photo);

        when(replicationJobRepository.findReadyJobs(any()))
            .thenReturn(List.of(job));
        when(storageRegistry.get("s3"))
            .thenReturn(sourceStorage);
        when(storageRegistry.get("gcs"))
            .thenReturn(targetStorage);
        when(sourceStorage.load("photo.webp"))
            .thenReturn(new ByteArrayResource(content));

        worker.processReadyJobs();

        assertEquals(
            ReplicationStatus.COMPLETED,
            photo.getReplicationStatus()
        );
        assertEquals(
            ReplicationJobStatus.COMPLETED,
            job.getStatus()
        );
        assertEquals(1, job.getAttempts());
        assertNotNull(photo.getReplicatedAt());
        assertNotNull(job.getCompletedAt());

        ArgumentCaptor<byte[]> contentCaptor =
            ArgumentCaptor.forClass(byte[].class);

        verify(targetStorage).storeCopy(
            eq("photo.webp"),
            contentCaptor.capture(),
            eq("image/webp")
        );

        assertArrayEquals(content, contentCaptor.getValue());
    }

    @Test
    void failedReplicationMarksPhotoAndJobAsFailed()
        throws Exception {

        Photo photo = testPhoto();
        ReplicationJob job = testJob(photo);
        Instant beforeProcessing = Instant.now();

        when(replicationJobRepository.findReadyJobs(any()))
            .thenReturn(List.of(job));
        when(storageRegistry.get("s3"))
            .thenReturn(sourceStorage);
        when(storageRegistry.get("gcs"))
            .thenReturn(targetStorage);
        when(sourceStorage.load("photo.webp"))
            .thenReturn(
                new ByteArrayResource(new byte[] { 1, 2, 3 })
            );

        doThrow(new IOException("GCS upload failed"))
            .when(targetStorage)
            .storeCopy(
                eq("photo.webp"),
                any(byte[].class),
                eq("image/webp")
            );

        worker.processReadyJobs();

        assertEquals(
            ReplicationStatus.FAILED,
            photo.getReplicationStatus()
        );
        assertEquals(
            ReplicationJobStatus.FAILED,
            job.getStatus()
        );
        assertEquals(1, job.getAttempts());
        assertEquals("GCS upload failed", job.getLastError());
        assertTrue(
            job.getNextAttemptAt().isAfter(beforeProcessing)
        );
    }

    private static Photo testPhoto() {
        AppUser user = new AppUser(
            "test@example.com",
            "password-hash",
            UserRole.USER
        );

        return new Photo(
            user,
            "photo.webp",
            "image/webp",
            3,
            "s3",
            "photo.webp"
        );
    }

    private static ReplicationJob testJob(Photo photo) {
        return new ReplicationJob(
            photo,
            "s3",
            "gcs"
        );
    }
}