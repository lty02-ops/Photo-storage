package com.photostorage.api.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalPhotoStorageService implements PhotoStorageService {

    private final Path storageRoot;

    public LocalPhotoStorageService(@Value("${storage.local-dir}") String localDir) throws IOException {
        this.storageRoot = Path.of(localDir).toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
    }

    @Override
    public StoredObject store(MultipartFile file) throws IOException {
        String objectKey = StorageObjectKeys.fromFilename(file.getOriginalFilename());
        Path target = storageRoot.resolve(objectKey).normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target);
        }
        return new StoredObject(objectKey);
    }

    @Override
    public Resource load(String objectKey) throws IOException {
        Path target = storageRoot.resolve(objectKey).normalize();
        Resource resource = new UrlResource(target.toUri());
        if (!target.startsWith(storageRoot) || !resource.exists()) {
            throw new IOException("Stored object does not exist.");
        }
        return resource;
    }

    @Override
    public void delete(String objectKey) {
        try {
            Path target = storageRoot.resolve(objectKey).normalize();
            if (target.startsWith(storageRoot)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public String provider() {
        return "local";
    }

    @Override
    public void storeCopy(
            String objectKey,
            byte[] content,
            String contentType) throws IOException {
        Path target = storageRoot.resolve(objectKey).normalize();


        if (!target.startsWith(storageRoot)) {
            throw new IOException("Invalid object key.");
        }

        Files.createDirectories(target.getParent());

        Files.write(target, content);
    }

}
