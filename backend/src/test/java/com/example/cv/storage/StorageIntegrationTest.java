package com.example.cv.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StorageIntegrationTest {

    @TempDir
    Path directory;

    @Test
    void localStorageSupportsTheCompleteFileLifecycle() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(directory.toString());
        String key = UUID.randomUUID() + ".pdf";
        byte[] content = "%PDF-integration".getBytes(StandardCharsets.US_ASCII);

        storage.upload(key, new ByteArrayInputStream(content), "application/pdf");

        assertThat(storage.exists(key)).isTrue();
        try (var downloaded = storage.download(key)) {
            assertThat(downloaded.readAllBytes()).containsExactly(content);
        }
        storage.delete(key);
        assertThat(storage.exists(key)).isFalse();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "STORAGE_S3_ENDPOINT", matches = "https?://.+")
    void s3CompatibleStorageSupportsTheCompleteFileLifecycle() {
        String endpoint = System.getenv("STORAGE_S3_ENDPOINT");
        String bucket = environmentOrDefault("STORAGE_S3_BUCKET", "cv-files");
        String region = environmentOrDefault("STORAGE_S3_REGION", "us-east-1");
        String accessKey = environmentOrDefault("STORAGE_S3_ACCESS_KEY", "");
        String secretKey = environmentOrDefault("STORAGE_S3_SECRET_KEY", "");
        String key = "integration/" + UUID.randomUUID() + ".pdf";
        byte[] content = "%PDF-s3-integration".getBytes(StandardCharsets.US_ASCII);

        new ApplicationContextRunner()
                .withUserConfiguration(StorageConfiguration.class)
                .withPropertyValues("storage.type=s3", "storage.s3.bucket=" + bucket,
                        "storage.s3.region=" + region, "storage.s3.endpoint=" + endpoint,
                        "storage.s3.path-style-access=true", "storage.s3.access-key=" + accessKey,
                        "storage.s3.secret-key=" + secretKey)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FileStorage storage = context.getBean(FileStorage.class);
                    storage.upload(key, new ByteArrayInputStream(content), "application/pdf");
                    assertThat(storage.exists(key)).isTrue();
                    try (var downloaded = storage.download(key)) {
                        assertThat(downloaded.readAllBytes()).containsExactly(content);
                    }
                    storage.delete(key);
                    assertThat(storage.exists(key)).isFalse();
                });
    }

    private String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}