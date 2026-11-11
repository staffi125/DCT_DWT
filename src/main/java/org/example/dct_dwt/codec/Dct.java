package org.example.dct_dwt.codec;

/**
 * Власна реалізація двовимірного DCT-II / IDCT через матричне представлення.
 * Зображення обробляється блоками 8×8: Y = C · X · Cᵀ, X = Cᵀ · Y · C.
 */
public final class Dct {

    public static final int BLOCK = 8;

    private static final double[][] C = buildTransformMatrix();
    private static final double[][] CT = Matrix.transpose(C);

    private Dct() {
    }

    /**
     * Матриця DCT-II розміру 8×8:
     * Cᵢⱼ = αᵢ · cos((2j+1)·i·π / 16),
     * α₀ = √(1/8), αᵢ = √(2/8) для i &gt; 0.
     */
    static double[][] transformMatrix() {
        return Matrix.copy(C);
    }

    public static double[][] forwardImage(double[][] pixels) {
        return transformImage(pixels, true);
    }

    public static double[][] inverseImage(double[][] coefficients) {
        return transformImage(coefficients, false);
    }

    public static double[][] forwardBlock(double[][] block) {
        return Matrix.multiply(Matrix.multiply(C, block), CT);
    }

    public static double[][] inverseBlock(double[][] coefficients) {
        return Matrix.multiply(Matrix.multiply(CT, coefficients), C);
    }

    private static double[][] transformImage(double[][] source, boolean forward) {
        int height = source.length;
        int width = source[0].length;
        if (height % BLOCK != 0 || width % BLOCK != 0) {
            throw new IllegalArgumentException("DCT потребує розміри, кратні 8");
        }
        double[][] result = new double[height][width];
        double[][] block = new double[BLOCK][BLOCK];
        for (int y = 0; y < height; y += BLOCK) {
            for (int x = 0; x < width; x += BLOCK) {
                copyBlock(source, y, x, block);
                double[][] transformed = forward ? forwardBlock(block) : inverseBlock(block);
                writeBlock(transformed, result, y, x);
            }
        }
        return result;
    }

    private static double[][] buildTransformMatrix() {
        double[][] matrix = new double[BLOCK][BLOCK];
        double scale0 = Math.sqrt(1.0 / BLOCK);
        double scale = Math.sqrt(2.0 / BLOCK);
        for (int i = 0; i < BLOCK; i++) {
            double alpha = (i == 0) ? scale0 : scale;
            for (int j = 0; j < BLOCK; j++) {
                matrix[i][j] = alpha * Math.cos((2.0 * j + 1.0) * i * Math.PI / (2.0 * BLOCK));
            }
        }
        return matrix;
    }

    private static void copyBlock(double[][] source, int y0, int x0, double[][] block) {
        for (int y = 0; y < BLOCK; y++) {
            System.arraycopy(source[y0 + y], x0, block[y], 0, BLOCK);
        }
    }

    private static void writeBlock(double[][] block, double[][] dest, int y0, int x0) {
        for (int y = 0; y < BLOCK; y++) {
            System.arraycopy(block[y], 0, dest[y0 + y], x0, BLOCK);
        }
    }
}
