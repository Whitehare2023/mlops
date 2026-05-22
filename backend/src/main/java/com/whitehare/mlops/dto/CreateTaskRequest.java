package com.whitehare.mlops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank String taskName,
        @NotNull LocalDate targetDate,
        @NotNull Double maskMin,
        @NotNull Double maskMax
) {
}

