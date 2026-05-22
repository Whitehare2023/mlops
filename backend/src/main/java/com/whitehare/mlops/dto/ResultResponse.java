package com.whitehare.mlops.dto;

public record ResultResponse(
        Long id,
        Long taskId,
        String csvPath,
        String trendChartPath,
        String anomalyMapPath
) {
}

