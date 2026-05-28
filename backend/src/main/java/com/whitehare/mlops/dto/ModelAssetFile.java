package com.whitehare.mlops.dto;

public record ModelAssetFile(
        String filename,
        String contentType,
        byte[] data
) {
}
