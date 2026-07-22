package com.photostorage.api.storage;

import java.util.UUID;

final class StorageObjectKeys {

    private StorageObjectKeys() {
    }

    static String fromFilename(String filename) {
        return UUID.randomUUID() + extension(filename);
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex);
    }
}
