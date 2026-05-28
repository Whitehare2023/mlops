package com.whitehare.mlops.dto;

import java.time.LocalDateTime;

public record ModelAssetResponse(
        Long id,
        String assetName,
        String originalFilename,
        String contentType,
        Long fileSize,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
