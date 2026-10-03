package com.example.cv.cv;

import java.util.UUID;

public record CvSkillResponse(UUID id, String name, String level, int sortOrder, boolean visible,
							  CvSkillDetails details) {
	public CvSkillResponse(UUID id, String name, String level, int sortOrder, boolean visible) {
		this(id, name, level, sortOrder, visible, null);
	}
}