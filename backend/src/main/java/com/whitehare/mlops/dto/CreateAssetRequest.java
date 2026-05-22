package com.whitehare.mlops.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAssetRequest(
        @NotBlank String assetName,
        @NotBlank String scriptPath,
        String description
) {
}

