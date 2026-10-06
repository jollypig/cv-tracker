package com.example.cv.cv;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CvMergeRequest(@NotEmpty @Size(max = 100) List<@NotNull UUID> sourceCvIds) {
}