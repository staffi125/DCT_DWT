package org.example.dct_dwt.codec;

/**
 * Власна реалізація ортонормованого вейвлета Хаара (DWT / IDWT).
 * На кожному рівні: 1D-перетворення рядків, потім стовпців (пряме);
 * обернене — стовпці, потім рядки. Апроксимація та деталі:
 * a = (x₀ + x₁)/√2, d = (x₀ − x₁)/√2.
 */
public final class HaarDwt {

    private static final double SQRT2 = Math.sqrt(2.0);

    private HaarDwt() {
    }

    public static Result forward(double[][] pixels) {
        double[][] coeffs = Matrix.copy(pixels);
        int rows = coeffs.length;
        int cols = coeffs[0].length;
        int levels = 0;
        int currentRows = rows;
        int currentCols = cols;
        while (currentRows >= 2 && currentCols >= 2) {
            forwardLevel(coeffs, currentRows, currentCols);
            currentRows /= 2;
            currentCols /= 2;
            levels++;
        }
        return new Result(coeffs, levels);
    }

    public static double[][] inverse(double[][] coefficients, int levels) {
        double[][] pixels = Matrix.copy(coefficients);
        int rows = pixels.length;
        int cols = pixels[0].length;
        for (int level = levels - 1; level >= 0; level--) {
            int currentRows = rows >> level;
            int currentCols = cols >> level;
            inverseLevel(pixels, currentRows, currentCols);
        }
        return pixels;
    }
    private static void forwardLevel(double[][] data, int rows, int cols) {
        double[] input = new double[Math.max(rows, cols)];
        double[] output = new double[Math.max(rows, cols)];

        for (int y = 0; y < rows; y++) {
            System.arraycopy(data[y], 0, input, 0, cols);
            haarForward1d(input, cols, output);
            System.arraycopy(output, 0, data[y], 0, cols);
        }

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                input[y] = data[y][x];
            }
            haarForward1d(input, rows, output);
            for (int y = 0; y < rows; y++) {
                data[y][x] = output[y];
            }
        }
    }
    private static void inverseLevel(double[][] data, int rows, int cols) {
        double[] input = new double[Math.max(rows, cols)];
        double[] output = new double[Math.max(rows, cols)];

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                input[y] = data[y][x];
            }
            haarInverse1d(input, rows, output);
            for (int y = 0; y < rows; y++) {
                data[y][x] = output[y];
            }
        }

        for (int y = 0; y < rows; y++) {
            System.arraycopy(data[y], 0, input, 0, cols);
            haarInverse1d(input, cols, output);
            System.arraycopy(output, 0, data[y], 0, cols);
        }
    }

    private static void haarForward1d(double[] in, int n, double[] out) {
        int half = n / 2;
        for (int i = 0; i < half; i++) {
            double a = in[2 * i];
            double b = in[2 * i + 1];
            out[i] = (a + b) / SQRT2;
            out[half + i] = (a - b) / SQRT2;
        }
    }

    private static void haarInverse1d(double[] in, int n, double[] out) {
        int half = n / 2;
        for (int i = 0; i < half; i++) {
            double approx = in[i];
            double detail = in[half + i];
            out[2 * i] = (approx + detail) / SQRT2;
            out[2 * i + 1] = (approx - detail) / SQRT2;
        }
    }

    public record Result(double[][] coefficients, int levels) {
    }
}
