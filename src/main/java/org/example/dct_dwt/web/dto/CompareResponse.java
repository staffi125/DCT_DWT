package org.example.dct_dwt.web.dto;

public record CompareResponse(
        int width,
        int height,
        double keepPercent,
        String originalPng,
        String dctPng,
        String dwtPng,
        MethodMetrics dct,
        MethodMetrics dwt
) {
}
