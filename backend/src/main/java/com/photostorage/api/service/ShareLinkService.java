package com.photostorage.api.service;

import com.photostorage.api.common.ApiException;
import com.photostorage.api.dto.StoredPhoto;
import com.photostorage.api.model.AppUser;
import com.photostorage.api.model.Photo;
import com.photostorage.api.model.ShareLink;
import com.photostorage.api.repository.ShareLinkRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final PhotoService photoService;

    public ShareLinkService(ShareLinkRepository shareLinkRepository, PhotoService photoService) {
        this.shareLinkRepository = shareLinkRepository;
        this.photoService = photoService;
    }

    @Transactional
    public String create(AppUser user, Long photoId) {
        Photo photo = photoService.findOwnedPhoto(user, photoId);
        ShareLink shareLink = new ShareLink(photo, UUID.randomUUID().toString(), Instant.now().plus(7, ChronoUnit.DAYS));
        return shareLinkRepository.save(shareLink).getToken();
    }

    @Transactional(readOnly = true)
    public StoredPhoto download(String token) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Share link not found."));

        if (shareLink.getRevokedAt() != null || shareLink.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.GONE, "Share link is no longer available.");
        }

        return photoService.downloadSharedPhoto(shareLink.getPhoto().getId());
    }
}
