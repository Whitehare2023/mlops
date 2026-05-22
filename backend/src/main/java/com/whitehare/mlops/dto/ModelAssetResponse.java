package com.whitehare.mlops.dto;

import java.time.LocalDateTime;

public record ModelAssetResponse(
        Long id,
        String assetName,
        String scriptPath,
        String description,
        LocalDateTime createdAt
) {
}

