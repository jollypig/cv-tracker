package com.example.cv.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigurationTest {

    @TempDir
    Path directory;

    @Test
    void selectsLocalStorageWhenConfigured() {
        new ApplicationContextRunner()
                .withUserConfiguration(StorageConfiguration.class)
                .withPropertyValues("storage.type=local", "storage.local.base-path=" + directory)
                .run(context -> {
                    assertThat(context).hasSingleBean(FileStorage.class);
                    assertThat(context.getBean(FileStorage.class)).isInstanceOf(LocalFileStorage.class);
                    assertThat(context).doesNotHaveBean(S3Client.class);
                });
    }

    @Test
    void selectsS3StorageWhenConfigured() {
        new ApplicationContextRunner()
                .withUserConfiguration(StorageConfiguration.class)
                .withPropertyValues("storage.type=s3", "storage.s3.bucket=cv-files",
                        "storage.s3.region=us-east-1", "storage.s3.endpoint=http://localhost:9000",
                        "storage.s3.path-style-access=true", "storage.s3.access-key=test-access",
                        "storage.s3.secret-key=test-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(FileStorage.class);
                    assertThat(context.getBean(FileStorage.class)).isInstanceOf(S3FileStorage.class);
                    assertThat(context).hasSingleBean(S3Client.class);
                });
    }
}