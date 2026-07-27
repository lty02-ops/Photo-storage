package com.photostorage.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.photostorage.api.model.AppUser;
import com.photostorage.api.model.Photo;
import com.photostorage.api.model.ReplicationJob;
import com.photostorage.api.model.ReplicationStatus;
import com.photostorage.api.model.UserRole;
import com.photostorage.api.repository.PhotoRepository;
import com.photostorage.api.repository.ReplicationJobRepository;
import com.photostorage.api.storage.PhotoStorageRegistry;
import com.photostorage.api.storage.PhotoStorageService;
import com.photostorage.api.storage.StoredObject;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private ReplicationJobRepository replicationJobRepository;

    @Mock
    private PhotoStorageRegistry storageRegistry;

    @Mock
    private PhotoStorageService storageService;

    @Test
    void localUploadDoesNotCreateReplicationJob() throws IOException {
        PhotoService photoService = createPhotoService(
            "local",
            false,
            ""
        );

        when(storageRegistry.get("local"))
            .thenReturn(storageService);
        when(storageService.provider())
            .thenReturn("local");
        when(storageService.store(any()))
            .thenReturn(new StoredObject("photo.webp"));
        when(photoRepository.save(any(Photo.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Photo result = photoService.upload(
            testUser(),
            testFile()
        );

        assertEquals(
            ReplicationStatus.NOT_REQUIRED,
            result.getReplicationStatus()
        );

        verify(
            replicationJobRepository,
            never()
        ).save(any(ReplicationJob.class));
    }

    @Test
    void awsUploadCreatesS3ToGcsReplicationJob()
        throws IOException {

        PhotoService photoService = createPhotoService(
            "s3",
            true,
            "gcs"
        );

        when(storageRegistry.get("s3"))
            .thenReturn(storageService);
        when(storageService.provider())
            .thenReturn("s3");
        when(storageService.store(any()))
            .thenReturn(new StoredObject("photo.webp"));
        when(photoRepository.save(any(Photo.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(replicationJobRepository.save(any(ReplicationJob.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Photo result = photoService.upload(
            testUser(),
            testFile()
        );

        assertEquals(
            ReplicationStatus.PENDING,
            result.getReplicationStatus()
        );

        ArgumentCaptor<ReplicationJob> captor =
            ArgumentCaptor.forClass(ReplicationJob.class);

        verify(replicationJobRepository).save(captor.capture());

        ReplicationJob savedJob = captor.getValue();

        assertEquals("s3", savedJob.getSourceProvider());
        assertEquals("gcs", savedJob.getTargetProvider());
    }

    @Test
    void downloadFallsBackToReplicatedStorageWhenPrimaryFails()
        throws IOException {
        PhotoService photoService = createPhotoService(
            "s3",
            true,
            "gcs"
        );

        AppUser user = testUser();
        Photo photo = new Photo(
            user,
            "photo.webp",
            "image/webp",
            3,
            "s3",
            "photo.webp"
        );
        ReflectionTestUtils.setField(photo, "id", 1L);
        photo.markReplicationCompleted();

        ReplicationJob replicationJob = new ReplicationJob(
            photo,
            "s3",
            "gcs"
        );
        replicationJob.markCompleted();

        when(photoRepository.findByIdAndUserAndDeletedAtIsNull(1L, user))
            .thenReturn(Optional.of(photo));
        when(replicationJobRepository.findByPhoto_Id(1L))
            .thenReturn(Optional.of(replicationJob));
        when(storageRegistry.get("s3"))
            .thenReturn(storageService);
        when(storageRegistry.get("gcs"))
            .thenReturn(storageService);
        when(storageService.load("photo.webp"))
            .thenThrow(new IOException("primary unavailable"))
            .thenReturn(new ByteArrayResource(new byte[] { 1, 2, 3 }));

        var result = photoService.downloadOwnedPhoto(user, 1L);

        assertNotNull(result);
        assertEquals("photo.webp", result.filename());
        verify(storageRegistry).get("s3");
        verify(storageRegistry).get("gcs");
    }

    private PhotoService createPhotoService(
        String provider,
        boolean replicationEnabled,
        String targetProvider
    ) {
        return new PhotoService(
            photoRepository,
            replicationJobRepository,
            storageRegistry,
            provider,
            replicationEnabled,
            targetProvider,
            10
        );
    }

    private static AppUser testUser() {
        return new AppUser(
            "test@example.com",
            "password-hash",
            UserRole.USER
        );
    }

    private static MockMultipartFile testFile() {
        return new MockMultipartFile(
            "file",
            "photo.webp",
            "image/webp",
            new byte[] { 1, 2, 3 }
        );
    }
}
