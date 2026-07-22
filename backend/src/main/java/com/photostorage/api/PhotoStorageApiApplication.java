package com.photostorage.api;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
public class PhotoStorageApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotoStorageApiApplication.class, args);
    }
}
