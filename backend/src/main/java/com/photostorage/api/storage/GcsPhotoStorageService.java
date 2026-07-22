package com.photostorage.api.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnExpression(
    "'${storage.provider:local}' == 'gcs' or " +
    "('${storage.replication.enabled:false}' == 'true' and " +
    "'${storage.replication.target-provider:}' == 'gcs')"
)
public class GcsPhotoStorageService implements PhotoStorageService {

    private final Storage storage;
    private final String bucketName;

    public GcsPhotoStorageService(@Value("${storage.gcs.bucket-name}") String bucketName) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucketName = bucketName;
    }

    @Override
    public StoredObject store(MultipartFile file) throws IOException {
        String objectKey = StorageObjectKeys.fromFilename(file.getOriginalFilename());
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectKey))
                .setContentType(file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            storage.createFrom(blobInfo, inputStream);
            return new StoredObject(objectKey);
        } catch (StorageException exception) {
            throw new IOException("Failed to store object in GCS.", exception);
        }
    }

    @Override
    public Resource load(String objectKey) throws IOException {
        try {
            Blob blob = storage.get(BlobId.of(bucketName, objectKey));
            if (blob == null || !blob.exists()) {
                throw new IOException("Stored object does not exist.");
            }
            return resource(blob.getContent(), objectKey);
        } catch (StorageException exception) {
            throw new IOException("Failed to load object from GCS.", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            storage.delete(BlobId.of(bucketName, objectKey));
        } catch (StorageException ignored) {
        }
    }

    @Override
    public String provider() {
        return "gcs";
    }

    private static Resource resource(byte[] content, String objectKey) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return objectKey;
            }
        };
    }

    @Override
    public void storeCopy(
            String objectKey,
            byte[] content,
            String contentType) throws IOException {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(
                    BlobId.of(bucketName, objectKey))
                    .setContentType(contentType)
                    .build();

            storage.create(blobInfo, content);
        } catch (StorageException exception) {
            throw new IOException("Failed to replicate object to GCS.", exception);
        }
    }
}
