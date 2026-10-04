package com.example.cv.importing;

public interface CvAiExtractor {

    default ModelMetadata modelMetadata() {
        return new ModelMetadata("unknown", "unknown");
    }

    DetectedCvSections detectSections(NormalizedCvDocument document);

    ParsedCv extract(NormalizedCvDocument document);

    record ModelMetadata(String name, String version) {
    }
}
