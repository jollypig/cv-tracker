package com.example.cv.importing;

public interface CvAiExtractor {

    ParsedCv extract(NormalizedCvDocument document);
}
