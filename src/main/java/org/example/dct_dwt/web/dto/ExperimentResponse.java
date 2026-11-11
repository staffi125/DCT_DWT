package org.example.dct_dwt.web.dto;

import java.util.List;

public record ExperimentResponse(
        int width,
        int height,
        List<Level> levels,
        String conclusions
) {
    public record Level(
            double keepPercent,
            MethodMetrics dct,
            MethodMetrics dwt,
            String originalPng,
            String dctPng,
            String dwtPng
    ) {
    }
}
