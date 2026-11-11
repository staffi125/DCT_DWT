package org.example.dct_dwt.codec;

import java.util.Arrays;

/**
 * Стиснення: збереження заданої частки найбільших за модулем коефіцієнтів,
 * решта обнулюються (keep-K / hard thresholding за рангом).
 */
public final class CoefficientKeep {

    private CoefficientKeep() {
    }

    public static Kept keepLargest(double[][] coefficients, double keepFraction) {
        int height = coefficients.length;
        int width = coefficients[0].length;
        int total = height * width;
        int keep = (int) Math.round(total * keepFraction);
        keep = Math.max(1, Math.min(total, keep));

        Coeff[] ranked = new Coeff[total];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ranked[index++] = new Coeff(y, x, Math.abs(coefficients[y][x]));
            }
        }
        Arrays.sort(ranked, (a, b) -> Double.compare(b.magnitude, a.magnitude));

        boolean[][] mask = new boolean[height][width];
        for (int i = 0; i < keep; i++) {
            mask[ranked[i].y][ranked[i].x] = true;
        }

        double[][] kept = new double[height][width];
        int nonZero = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask[y][x]) {
                    kept[y][x] = coefficients[y][x];
                    if (kept[y][x] != 0.0) {
                        nonZero++;
                    }
                }
            }
        }
        return new Kept(kept, keep, nonZero, total);
    }

    private record Coeff(int y, int x, double magnitude) {
    }

    public record Kept(double[][] coefficients, int keptSlots, int nonZero, int total) {
        public double keptPercent() {
            return 100.0 * keptSlots / total;
        }

        /**
         * Коефіцієнт стиснення як total / кількість збережених слотів.
         * Це не розмір закодованого файлу, а відношення кількості коефіцієнтів.
         */
        public double coefficientRatio() {
            return keptSlots == 0 ? Double.POSITIVE_INFINITY : (double) total / keptSlots;
        }
    }
}
