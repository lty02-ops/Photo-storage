package com.photostorage.api.dto;

import com.photostorage.api.model.Photo;

public record PhotoResponse(
    Long id,
    String originalFilename,
    String contentType,
    long sizeBytes,
    String storageProvider,
    String replicationStatus,
    String replicatedAt,
    String createdAt
) {
    public static PhotoResponse from(Photo photo) {
        return new PhotoResponse(
            photo.getId(),
            photo.getOriginalFilename(),
            photo.getContentType(),
            photo.getSizeBytes(),
            photo.getStorageProvider(),
            photo.getReplicationStatus().name(),
            photo.getReplicatedAt() == null ? null : photo.getReplicatedAt().toString(),
            photo.getCreatedAt().toString()
        );
    }
}
