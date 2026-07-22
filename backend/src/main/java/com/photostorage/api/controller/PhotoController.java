package com.photostorage.api.controller;

import com.photostorage.api.common.CurrentUser;
import com.photostorage.api.dto.PhotoResponse;
import com.photostorage.api.dto.ShareResponse;
import com.photostorage.api.dto.StoredPhoto;
import com.photostorage.api.model.AppUser;
import com.photostorage.api.service.PhotoService;
import com.photostorage.api.service.ShareLinkService;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoService photoService;
    private final ShareLinkService shareLinkService;

    public PhotoController(PhotoService photoService, ShareLinkService shareLinkService) {
        this.photoService = photoService;
        this.shareLinkService = shareLinkService;
    }

    @GetMapping
    public List<PhotoResponse> list(@CurrentUser AppUser user) {
        return photoService.list(user).stream().map(PhotoResponse::from).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhotoResponse upload(@CurrentUser AppUser user, @RequestParam("file") MultipartFile file) {
        return PhotoResponse.from(photoService.upload(user, file));
    }

    @GetMapping("/{photoId}/download")
    public ResponseEntity<Resource> download(@CurrentUser AppUser user, @PathVariable Long photoId) {
        StoredPhoto storedPhoto = photoService.downloadOwnedPhoto(user, photoId);
        return fileResponse(storedPhoto);
    }

    @PostMapping("/{photoId}/share")
    public ShareResponse share(@CurrentUser AppUser user, @PathVariable Long photoId) {
        String token = shareLinkService.create(user, photoId);
        return new ShareResponse(token, "/api/share/" + token);
    }

    @DeleteMapping("/{photoId}")
    public void delete(@CurrentUser AppUser user, @PathVariable Long photoId) {
        photoService.delete(user, photoId);
    }

    public static ResponseEntity<Resource> fileResponse(StoredPhoto storedPhoto) {
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(storedPhoto.filename())
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(storedPhoto.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentLength(storedPhoto.sizeBytes())
            .body(storedPhoto.resource());
    }

}
