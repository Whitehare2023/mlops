package com.whitehare.mlops.dto;

import com.whitehare.mlops.domain.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String taskName,
        LocalDate targetDate,
        Double maskMin,
        Double maskMax,
        TaskStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        ResultResponse result
) {
}

