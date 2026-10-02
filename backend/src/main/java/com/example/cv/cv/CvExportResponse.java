package com.example.cv.cv;

import java.time.Instant;
import java.util.UUID;

public record CvExportResponse(
        UUID id,
        UUID versionId,
        int versionNumber,
        String fileName,
        long fileSize,
        Instant createdAt,
        String downloadUrl) {

    static CvExportResponse from(CvExport export) {
        CvVersion version = export.getVersion();
        return new CvExportResponse(export.getId(), version.getId(), version.getVersionNumber(),
                export.getFileName(), export.getFileSize(), export.getCreatedAt(),
                "/api/v1/exports/" + export.getId() + "/download");
    }
}