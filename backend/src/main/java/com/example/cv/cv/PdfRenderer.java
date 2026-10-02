package com.example.cv.cv;

public interface PdfRenderer {
    byte[] render(String html);
}