package com.photostorage.api.controller;

import com.photostorage.api.common.ApiException;
import com.photostorage.api.common.CurrentUser;
import com.photostorage.api.dto.AdminStats;
import com.photostorage.api.model.AppUser;
import com.photostorage.api.model.UserRole;
import com.photostorage.api.repository.PhotoRepository;
import com.photostorage.api.repository.ShareLinkRepository;
import com.photostorage.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    private final ShareLinkRepository shareLinkRepository;

    public AdminController(
        UserRepository userRepository,
        PhotoRepository photoRepository,
        ShareLinkRepository shareLinkRepository
    ) {
        this.userRepository = userRepository;
        this.photoRepository = photoRepository;
        this.shareLinkRepository = shareLinkRepository;
    }

    @GetMapping("/stats")
    public AdminStats stats(@CurrentUser AppUser user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin role is required.");
        }
        return new AdminStats(
            userRepository.count(),
            photoRepository.countByDeletedAtIsNull(),
            shareLinkRepository.count()
        );
    }
}
