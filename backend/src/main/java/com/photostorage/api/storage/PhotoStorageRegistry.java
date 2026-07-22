package com.photostorage.api.storage;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PhotoStorageRegistry {

    private final Map<String, PhotoStorageService> storageServices;

    public PhotoStorageRegistry(List<PhotoStorageService> storageServices) {
        this.storageServices = storageServices.stream()
            .collect(Collectors.toUnmodifiableMap(
                service -> normalize(service.provider()),
                Function.identity()
            ));
    }

    public PhotoStorageService get(String provider) {
        PhotoStorageService storageService =
            storageServices.get(normalize(provider));

        if (storageService == null) {
            throw new IllegalArgumentException(
                "Unsupported storage provider: " + provider
            );
        }

        return storageService;
    }

    public boolean contains(String provider) {
        return storageServices.containsKey(normalize(provider));
    }

    private static String normalize(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                "Storage provider is required."
            );
        }

        return provider.trim().toLowerCase(Locale.ROOT);
    }
}