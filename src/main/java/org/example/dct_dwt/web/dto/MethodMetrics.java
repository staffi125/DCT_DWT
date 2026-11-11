package org.example.dct_dwt.web.dto;

public record MethodMetrics(
        String method,
        int keptSlots,
        int nonZero,
        int totalCoefficients,
        double keptPercent,
        double coefficientRatio,
        double psnr
) {
}
