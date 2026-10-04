package com.example.cv.cv;

public record CvExportFile(String fileName, byte[] content, String contentType) {

	public CvExportFile(String fileName, byte[] content) {
		this(fileName, content, "application/pdf");
	}
}