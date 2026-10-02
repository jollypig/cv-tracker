package com.example.cv.storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;

public class S3FileStorage implements FileStorage {

    private final S3Client client;
    private final String bucket;

    public S3FileStorage(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public StoredFile upload(String path, InputStream content, String contentType) {
        try {
            byte[] bytes = content.readAllBytes();
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(path)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes));
            return new StoredFile(path, bytes.length, contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read file for S3 upload", exception);
        } catch (S3Exception exception) {
            throw new IllegalStateException("Could not store file in S3", exception);
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            return client.getObject(GetObjectRequest.builder().bucket(bucket).key(path).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new IllegalStateException("Could not read file", new IOException("Stored file not found"));
            }
            throw new IllegalStateException("Could not read file from S3", exception);
        }
    }

    @Override
    public void delete(String path) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(path).build());
        } catch (S3Exception exception) {
            throw new IllegalStateException("Could not delete file from S3", exception);
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(path).build());
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw new IllegalStateException("Could not check file in S3", exception);
        }
    }
}