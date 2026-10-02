package com.example.cv.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "local", matchIfMissing = true)
    FileStorage localFileStorage(StorageProperties properties) {
        String basePath = properties.local() == null ? "./data/exports" : properties.local().basePath();
        if (basePath == null || basePath.isBlank()) {
            basePath = "./data/exports";
        }
        return new LocalFileStorage(basePath);
    }

    @Bean
    @ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "s3")
    S3Client s3Client(StorageProperties properties) {
        StorageProperties.S3 s3 = properties.s3();
        if (s3 == null || s3.bucket() == null || s3.bucket().isBlank()) {
            throw new IllegalStateException("storage.s3.bucket must be configured when storage.type=s3");
        }

        AwsCredentialsProvider credentials = credentialsProvider(s3);
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3.region() == null || s3.region().isBlank() ? "us-east-1" : s3.region()))
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3.pathStyleAccess())
                        .build());
        if (s3.endpoint() != null && !s3.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.endpoint()));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "s3")
    FileStorage s3FileStorage(StorageProperties properties, S3Client s3Client) {
        return new S3FileStorage(s3Client, properties.s3().bucket());
    }

    private AwsCredentialsProvider credentialsProvider(StorageProperties.S3 properties) {
        boolean accessKeySet = properties.accessKey() != null && !properties.accessKey().isBlank();
        boolean secretKeySet = properties.secretKey() != null && !properties.secretKey().isBlank();
        if (accessKeySet != secretKeySet) {
            throw new IllegalStateException("Both S3 access key and secret key must be configured together");
        }
        if (accessKeySet) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}