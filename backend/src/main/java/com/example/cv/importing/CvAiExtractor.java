package com.example.cv.importing;

public interface CvAiExtractor {

    DetectedCvSections detectSections(NormalizedCvDocument document);

    ParsedCv extract(NormalizedCvDocument document);
}
