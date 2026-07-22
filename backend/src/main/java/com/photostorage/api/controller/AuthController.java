package com.photostorage.api.controller;

import com.photostorage.api.common.CurrentUser;
import com.photostorage.api.dto.AuthResponse;
import com.photostorage.api.dto.LoginRequest;
import com.photostorage.api.dto.SignupRequest;
import com.photostorage.api.dto.UserResponse;
import com.photostorage.api.model.AppUser;
import com.photostorage.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request.email(), request.password());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @GetMapping("/me")
    public UserResponse me(@CurrentUser AppUser user) {
        return UserResponse.from(user);
    }
}
