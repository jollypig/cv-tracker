package com.example.cv.cv;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class OpenHtmlToPdfRenderer implements PdfRenderer {

    @Override
    public byte[] render(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not render CV PDF", exception);
        }
    }
}