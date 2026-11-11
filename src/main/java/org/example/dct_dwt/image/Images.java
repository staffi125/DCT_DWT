package org.example.dct_dwt.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public final class Images {

    private Images() {
    }

    public static double[][] loadGrayscale(InputStream input) throws IOException {
        BufferedImage image = ImageIO.read(input);
        if (image == null) {
            throw new IOException("Не вдалося прочитати зображення");
        }
        return toGrayscale(image);
    }

    public static double[][] toGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] gray = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[y][x] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        return gray;
    }

    public static BufferedImage fromGrayscale(double[][] gray) {
        int height = gray.length;
        int width = gray[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = clampToByte(gray[y][x]);
                int rgb = (value << 16) | (value << 8) | value;
                image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        return image;
    }

    public static String toPngBase64(double[][] gray) {
        try {
            BufferedImage image = fromGrayscale(gray);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            return Base64.getEncoder().encodeToString(buffer.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Не вдалося закодувати PNG", e);
        }
    }

    public static double[][] crop(double[][] source, int height, int width) {
        double[][] cropped = new double[height][width];
        for (int y = 0; y < height; y++) {
            System.arraycopy(source[y], 0, cropped[y], 0, width);
        }
        return cropped;
    }

    public static double[][] padTo(double[][] source, int height, int width) {
        double[][] padded = new double[height][width];
        for (int y = 0; y < source.length; y++) {
            System.arraycopy(source[y], 0, padded[y], 0, source[y].length);
        }
        return padded;
    }

    public static int nextPowerOfTwo(int value) {
        int power = 1;
        while (power < value) {
            power <<= 1;
        }
        return power;
    }

    public static int clampToByte(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return (int) Math.round(value);
    }

    /**
     * Синтетичне тестове зображення 512×512 з різними ділянками для демонстрації артефактів:
     * - дрібна шахова текстура (складна для DCT/DWT → видно артефакти)
     * - різкі краї (блокові артефакти DCT vs ступінчасті DWT)
     * - гладкий градієнт (обидва методи справляються добре)
     * - діагональні смуги (висока частота → помітні при стисненні)
     */
    public static double[][] synthetic512() {
        int size = 512;
        double[][] image = new double[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double nx = x / 511.0;
                double ny = y / 511.0;

                // Фонова яскравість — плавний градієнт
                double base = 30 + 60 * nx + 40 * ny;

                // Квадрант 1 (верх-ліво): дрібна шахова текстура 4×4 — найскладніша для DCT
                double checker4 = 0;
                if (x < 256 && y < 256) {
                    checker4 = ((x / 4 + y / 4) % 2 == 0) ? 120 : 0;
                }

                // Квадрант 2 (верх-право): чисті горизонтальні смуги 2px
                double hStripes = 0;
                if (x >= 256 && y < 256) {
                    hStripes = ((y / 2) % 2 == 0) ? 160 : 20;
                }

                // Квадрант 3 (низ-ліво): гладкий радіальний градієнт
                double radial = 0;
                if (x < 256 && y >= 256) {
                    double dist = Math.hypot(x - 128, y - 384) / 180.0;
                    radial = 200 * Math.exp(-dist * dist * 4.0);
                }

                // Квадрант 4 (низ-право): синусоїдальна текстура різних частот
                double sinTex = 0;
                if (x >= 256 && y >= 256) {
                    double fx = 2 * Math.PI * (x - 256) / 16.0;  // 16px період
                    double fy = 2 * Math.PI * (y - 256) / 12.0;  // 12px період
                    sinTex = 80 + 70 * Math.sin(fx) * Math.cos(fy);
                }

                // Чіткий кордон — вертикальна лінія для демонстрації артефактів краю
                double edgeLine = (x == 255 || x == 256) ? 255 : 0;

                // Коло з різким краєм у центрі
                double circle = 0;
                if (Math.hypot(x - 256, y - 256) < 60) {
                    circle = 180;
                }

                image[y][x] = clampRange(base + checker4 + hStripes + radial + sinTex + edgeLine + circle);
            }
        }
        return image;
    }

    private static double clampRange(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return value;
    }
}
