package com.whitehare.mlops.dto;

import java.util.List;
import java.util.Map;

public record CsvTableResponse(
        List<String> headers,
        List<Map<String, String>> rows
) {
}

