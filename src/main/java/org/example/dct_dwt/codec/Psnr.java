package org.example.dct_dwt.codec;

public final class Psnr {

    private Psnr() {
    }

    public static double of(double[][] original, double[][] restored) {
        int height = original.length;
        int width = original[0].length;
        double mse = 0.0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double delta = original[y][x] - restored[y][x];
                mse += delta * delta;
            }
        }
        mse /= (double) height * width;
        if (mse < 1e-12) {
            return Double.POSITIVE_INFINITY;
        }
        return 10.0 * Math.log10((255.0 * 255.0) / mse);
    }
}
