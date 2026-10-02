package com.example.cv.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path directory;

    @Test
    void uploadsDownloadsAndDeletesFiles() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(directory.toString());
        byte[] content = "stored file".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        StoredFile storedFile = storage.upload("exports/cv.pdf", new ByteArrayInputStream(content), "application/pdf");

        assertThat(storedFile.path()).isEqualTo("exports/cv.pdf");
        assertThat(storedFile.size()).isEqualTo(content.length);
        assertThat(storedFile.contentType()).isEqualTo("application/pdf");
        assertThat(storage.exists("exports/cv.pdf")).isTrue();
        try (var downloaded = storage.download("exports/cv.pdf")) {
            assertThat(downloaded.readAllBytes()).containsExactly(content);
        }

        storage.delete("exports/cv.pdf");

        assertThat(storage.exists("exports/cv.pdf")).isFalse();
    }

    @Test
    void rejectsPathsOutsideTheConfiguredDirectory() {
        LocalFileStorage storage = new LocalFileStorage(directory.toString());

        assertThatThrownBy(() -> storage.exists("../outside.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}