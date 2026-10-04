package com.example.cv.importing;

public class PdfCvParserException extends RuntimeException {

    private final Reason reason;

    public PdfCvParserException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public PdfCvParserException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        UNSUPPORTED_MEDIA_TYPE,
        FILE_TOO_LARGE,
        INVALID_PDF,
        OCR_REQUIRED
    }
}