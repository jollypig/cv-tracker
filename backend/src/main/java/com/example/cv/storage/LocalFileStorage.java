package com.example.cv.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LocalFileStorage implements FileStorage {

    private final Path basePath;

    public LocalFileStorage(String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile upload(String path, InputStream content, String contentType) {
        Path destination = resolve(path);
        try {
            Files.createDirectories(destination.getParent());
            long size = Files.copy(content, destination, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(path, size, contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store file", exception);
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            return Files.newInputStream(resolve(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read file", exception);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(resolve(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete file", exception);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.isRegularFile(resolve(path));
    }

    private Path resolve(String path) {
        Path resolved = basePath.resolve(path).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("Storage path must stay within the configured base path");
        }
        return resolved;
    }
}