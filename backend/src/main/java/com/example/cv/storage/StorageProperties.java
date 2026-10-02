package com.example.cv.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String type, Local local, S3 s3) {

    public record Local(String basePath) {
    }

    public record S3(String bucket, String region, String endpoint, boolean pathStyleAccess,
            String accessKey, String secretKey) {
    }
}