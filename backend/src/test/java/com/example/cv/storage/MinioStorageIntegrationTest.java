package com.example.cv.storage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class MinioStorageIntegrationTest {

    private static final String BUCKET = "cv-files";
    private static final String ACCESS_KEY = "cv_minio_user";
    private static final String SECRET_KEY = "cv_minio_password";

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> minio = new GenericContainer<>(
            DockerImageName.parse("bitnamilegacy/minio:2025.7.23-debian-12-r5"))
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

    @BeforeAll
    static void createBucket() {
        try (S3Client client = newS3Client()) {
            client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    @Test
    void storesDownloadsAndDeletesFilesThroughTheS3Adapter() throws Exception {
        String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
        try (S3Client client = newS3Client(endpoint)) {
            FileStorage storage = new S3FileStorage(client, BUCKET);
            String key = "integration/" + UUID.randomUUID() + ".pdf";
            byte[] content = "%PDF-minio-integration".getBytes(StandardCharsets.US_ASCII);

            storage.upload(key, new ByteArrayInputStream(content), "application/pdf");

            assertThat(storage.exists(key)).isTrue();
            try (var downloaded = storage.download(key)) {
                assertThat(downloaded.readAllBytes()).containsExactly(content);
            }
            storage.delete(key);
            assertThat(storage.exists(key)).isFalse();
        }
    }

    private static S3Client newS3Client() {
        return newS3Client("http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
    }

    private static S3Client newS3Client(String endpoint) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}