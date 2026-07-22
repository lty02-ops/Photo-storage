package com.photostorage.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

    private final String cloudProvider;
    private final String podName;

    public SystemInfoController(
        @Value("${app.cloud-provider:local}") String cloudProvider,
        @Value("${POD_NAME:${HOSTNAME:unknown}}") String podName
    ) {
        this.cloudProvider = cloudProvider;
        this.podName = podName;
    }

    @GetMapping("/info")
    public SystemInfoResponse info() {
        return new SystemInfoResponse(cloudProvider, podName);
    }

    public record SystemInfoResponse(
        String cloudProvider,
        String podName
    ) {
    }
}