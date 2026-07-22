package com.photostorage.api.storage;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@ConditionalOnExpression(
    "'${storage.provider:local}' == 's3' or " +
    "('${storage.replication.enabled:false}' == 'true' and " +
    "'${storage.replication.target-provider:}' == 's3')"
)
public class S3PhotoStorageService implements PhotoStorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public S3PhotoStorageService(@Value("${storage.s3.bucket-name}") String bucketName) {
        this.s3Client = S3Client.builder().build();
        this.bucketName = bucketName;
    }

    @Override
    public StoredObject store(MultipartFile file) throws IOException {
        String objectKey = StorageObjectKeys.fromFilename(file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            return new StoredObject(objectKey);
        } catch (S3Exception exception) {
            throw new IOException("Failed to store object in S3.", exception);
        }
    }

    @Override
    public Resource load(String objectKey) throws IOException {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(request);
            return resource(object.asByteArray(), objectKey);
        } catch (NoSuchKeyException exception) {
            throw new IOException("Stored object does not exist.", exception);
        } catch (S3Exception exception) {
            throw new IOException("Failed to load object from S3.", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(request -> request.bucket(bucketName).key(objectKey));
        } catch (S3Exception ignored) {
        }
    }

    @Override
    public String provider() {
        return "s3";
    }

    @PreDestroy
    void close() {
        s3Client.close();
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
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (S3Exception exception) {
            throw new IOException("Failed to replicate object to S3.", exception);
        }
    }
}
