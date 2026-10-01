package com.example.cv.cv;

import java.util.List;
import java.util.UUID;

public record CvSkillGroupResponse(UUID id, String name, int sortOrder, List<CvSkillResponse> skills) {
}