package com.example.cv.cv;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CvExportFileNameTest {

    @Test
    void generatesDeterministicVersionedFileNames() {
        assertThat(CvExportFileName.generate("John", "Smith", "Java Backend", 3))
                .isEqualTo("John_Smith_Java_Backend_v3.pdf");
    }

    @Test
    void removesPathAndPlatformSpecificFilenameCharacters() {
        assertThat(CvExportFileName.generate("Jane/Jo", "Smith", "Backend: Java", 1))
                .isEqualTo("Jane_Jo_Smith_Backend_Java_v1.pdf");
    }
}