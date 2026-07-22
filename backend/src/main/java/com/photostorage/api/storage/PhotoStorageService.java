package com.photostorage.api.storage;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorageService {

    StoredObject store(MultipartFile file) throws IOException;

    void storeCopy(
        String objectKey,
        byte[] content,
        String contentType
    ) throws IOException;

    Resource load(String objectKey) throws IOException;

    void delete(String objectKey);

    String provider();
}