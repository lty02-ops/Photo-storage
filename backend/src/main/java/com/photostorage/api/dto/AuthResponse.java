package com.photostorage.api.dto;

public record AuthResponse(String token, UserResponse user) {
}
