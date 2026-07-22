package com.photostorage.api.service;

import com.photostorage.api.common.ApiException;
import com.photostorage.api.dto.StoredPhoto;
import com.photostorage.api.storage.PhotoStorageRegistry;
import com.photostorage.api.storage.PhotoStorageService;
import com.photostorage.api.storage.StoredObject;
import com.photostorage.api.model.AppUser;
import com.photostorage.api.model.Photo;
import com.photostorage.api.model.ReplicationJob;
import com.photostorage.api.repository.PhotoRepository;
import com.photostorage.api.repository.ReplicationJobRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final ReplicationJobRepository replicationJobRepository;
    private final PhotoStorageRegistry storageRegistry;
    private final String primaryStorageProvider;
    private final boolean replicationEnabled;
    private final String replicationTargetProvider;
    private final long maxUploadSizeBytes;

    public PhotoService(
            PhotoRepository photoRepository,
            ReplicationJobRepository replicationJobRepository,
            PhotoStorageRegistry storageRegistry,
            @Value("${storage.provider:local}") String primaryStorageProvider,
            @Value("${storage.replication.enabled:false}") boolean replicationEnabled,
            @Value("${storage.replication.target-provider:}") String replicationTargetProvider,
            @Value("${storage.max-upload-size-mb}") long maxUploadSizeMb) {
        this.photoRepository = photoRepository;
        this.replicationJobRepository = replicationJobRepository;
        this.storageRegistry = storageRegistry;
        this.primaryStorageProvider = primaryStorageProvider;
        this.replicationEnabled = replicationEnabled;
        this.replicationTargetProvider = replicationTargetProvider;
        this.maxUploadSizeBytes = maxUploadSizeMb * 1024 * 1024;
    }

    @Transactional(readOnly = true)
    public List<Photo> list(AppUser user) {
        return photoRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user);
    }

    @Transactional
    public Photo upload(AppUser user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Photo file is required.");
        }
        if (file.getSize() > maxUploadSizeBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Photo exceeds upload size limit.");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only image files are allowed.");
        }

        try {
            PhotoStorageService storageService = storageRegistry.get(primaryStorageProvider);
            validateReplicationTarget(storageService.provider());
            StoredObject storedObject = storageService.store(file);
            Photo photo = new Photo(
                    user,
                    safeFilename(file.getOriginalFilename()),
                    file.getContentType(),
                    file.getSize(),
                    storageService.provider(),
                    storedObject.objectKey());

            if (!replicationEnabled) {
                photo.markReplicationNotRequired();
            }

            Photo savedPhoto = photoRepository.save(photo);

            if (replicationEnabled) {
                ReplicationJob replicationJob = new ReplicationJob(
                        savedPhoto,
                        storageService.provider(),
                        replicationTargetProvider);
                replicationJobRepository.save(replicationJob);
            }

            return savedPhoto;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store photo.");
        }
    }

    @Transactional(readOnly = true)
    public StoredPhoto downloadOwnedPhoto(AppUser user, Long photoId) {
        Photo photo = findOwnedPhoto(user, photoId);
        return load(photo);
    }

    @Transactional(readOnly = true)
    public Photo findOwnedPhoto(AppUser user, Long photoId) {
        return photoRepository.findByIdAndUserAndDeletedAtIsNull(photoId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Photo not found."));
    }

    @Transactional(readOnly = true)
    public StoredPhoto downloadSharedPhoto(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .filter(foundPhoto -> foundPhoto.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Photo not found."));
        return load(photo);
    }

    @Transactional
    public void delete(AppUser user, Long photoId) {
        Photo photo = findOwnedPhoto(user, photoId);
        PhotoStorageService storageService = storageRegistry.get(photo.getStorageProvider());
        photo.markDeleted();
        photoRepository.save(photo);
        storageService.delete(photo.getObjectKey());
    }

    private StoredPhoto load(Photo photo) {
        PhotoStorageService storageService = storageRegistry.get(photo.getStorageProvider());
        try {
            return new StoredPhoto(
                    storageService.load(photo.getObjectKey()),
                    photo.getOriginalFilename(),
                    photo.getContentType(),
                    photo.getSizeBytes());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load photo.");
        }
    }

    private void validateReplicationTarget(String sourceProvider) {
        if (!replicationEnabled) {
            return;
        }
        if (replicationTargetProvider == null || replicationTargetProvider.isBlank()) {
            throw new IllegalStateException("Replication target provider is required.");
        }
        if (sourceProvider.equalsIgnoreCase(replicationTargetProvider)) {
            throw new IllegalStateException(
                    "Source and target storage providers must be different.");
        }
    }

    private static String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo";
        }
        return filename.replaceAll("[\\\\/]", "_");
    }
}
