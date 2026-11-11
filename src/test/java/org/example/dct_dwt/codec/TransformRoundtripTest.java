package org.example.dct_dwt.codec;

import org.example.dct_dwt.image.Images;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformRoundtripTest {

    @Test
    void dctMatrixIsOrthonormalEnoughForRoundtrip() {
        double[][] block = sampleBlock();
        double[][] restored = Dct.inverseBlock(Dct.forwardBlock(block));
        assertTrue(maxAbsError(block, restored) < 1e-10);
    }

    @Test
    void haarRoundtripOnPowerOfTwoImage() {
        double[][] image = Images.synthetic512();
        HaarDwt.Result forward = HaarDwt.forward(image);
        double[][] restored = HaarDwt.inverse(forward.coefficients(), forward.levels());
        assertTrue(maxAbsError(image, restored) < 1e-8);
        assertEquals(9, forward.levels());
    }

    @Test
    void fullKeepYieldsVeryHighPsnr() {
        double[][] image = Images.synthetic512();
        double[][] dctCoeffs = Dct.forwardImage(image);
        CoefficientKeep.Kept kept = CoefficientKeep.keepLargest(dctCoeffs, 1.0);
        double[][] restored = Dct.inverseImage(kept.coefficients());
        assertTrue(Psnr.of(image, restored) > 80);
    }

    private static double[][] sampleBlock() {
        double[][] block = new double[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                block[y][x] = (y * 13 + x * 7) % 256;
            }
        }
        return block;
    }

    private static double maxAbsError(double[][] a, double[][] b) {
        double max = 0;
        for (int y = 0; y < a.length; y++) {
            for (int x = 0; x < a[y].length; x++) {
                max = Math.max(max, Math.abs(a[y][x] - b[y][x]));
            }
        }
        return max;
    }
}
