package com.photostorage.api.controller;

import com.photostorage.api.dto.StoredPhoto;
import com.photostorage.api.service.ShareLinkService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    private final ShareLinkService shareLinkService;

    public ShareController(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<Resource> download(@PathVariable String token) {
        StoredPhoto storedPhoto = shareLinkService.download(token);
        return PhotoController.fileResponse(storedPhoto);
    }
}
