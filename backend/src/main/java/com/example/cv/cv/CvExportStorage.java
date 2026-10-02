package com.example.cv.cv;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class CvExportStorage {

    private final Path directory;

    public CvExportStorage(@Value("${app.exports.directory:./data/exports}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(UUID exportId, byte[] content) {
        String storageKey = exportId + ".pdf";
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(storageKey), content);
            return storageKey;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store CV PDF", exception);
        }
    }

    public byte[] read(String storageKey) {
        if (storageKey == null || !storageKey.matches("[0-9a-fA-F-]{36}\\.pdf")) {
            throw new IllegalStateException("Invalid CV export storage key");
        }
        try {
            return Files.readAllBytes(directory.resolve(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read CV PDF", exception);
        }
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(directory.resolve(storageKey));
        } catch (IOException ignored) {
        }
    }
}