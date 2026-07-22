package com.photostorage.api.dto;

import org.springframework.core.io.Resource;

public record StoredPhoto(Resource resource, String filename, String contentType, long sizeBytes) {
}
