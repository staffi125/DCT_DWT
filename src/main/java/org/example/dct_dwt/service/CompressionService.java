package org.example.dct_dwt.service;

import org.example.dct_dwt.codec.CoefficientKeep;
import org.example.dct_dwt.codec.Dct;
import org.example.dct_dwt.codec.HaarDwt;
import org.example.dct_dwt.codec.Psnr;
import org.example.dct_dwt.image.Images;
import org.example.dct_dwt.web.dto.CompareResponse;
import org.example.dct_dwt.web.dto.ExperimentResponse;
import org.example.dct_dwt.web.dto.MethodMetrics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CompressionService {

    public static final double[] EXPERIMENT_KEEP_FRACTIONS = {0.05, 0.15, 0.40};

    public CompareResponse compress(double[][] original, double keepFraction) {
        int height = original.length;
        int width = original[0].length;
        double[][] work = padForTransforms(original);

        MethodResult dct = compressDct(work, original, keepFraction);
        MethodResult dwt = compressDwt(work, original, keepFraction);

        return new CompareResponse(
                width,
                height,
                keepFraction * 100.0,
                Images.toPngBase64(original),
                Images.toPngBase64(dct.restored),
                Images.toPngBase64(dwt.restored),
                dct.metrics,
                dwt.metrics
        );
    }

    public ExperimentResponse experiments(double[][] original) {
        List<ExperimentResponse.Level> levels = new ArrayList<>();
        for (double fraction : EXPERIMENT_KEEP_FRACTIONS) {
            CompareResponse result = compress(original, fraction);
            levels.add(new ExperimentResponse.Level(
                    fraction * 100.0,
                    result.dct(),
                    result.dwt(),
                    result.originalPng(),
                    result.dctPng(),
                    result.dwtPng()
            ));
        }
        return new ExperimentResponse(original[0].length, original.length, levels, conclusions(levels));
    }

    private MethodResult compressDct(double[][] work, double[][] original, double keepFraction) {
        double[][] coeffs = Dct.forwardImage(work);
        CoefficientKeep.Kept kept = CoefficientKeep.keepLargest(coeffs, keepFraction);
        double[][] restoredPadded = Dct.inverseImage(kept.coefficients());
        double[][] restored = Images.crop(restoredPadded, original.length, original[0].length);
        return new MethodResult(restored, toMetrics("DCT", kept, original, restored));
    }

    private MethodResult compressDwt(double[][] work, double[][] original, double keepFraction) {
        HaarDwt.Result forward = HaarDwt.forward(work);
        CoefficientKeep.Kept kept = CoefficientKeep.keepLargest(forward.coefficients(), keepFraction);
        double[][] restoredPadded = HaarDwt.inverse(kept.coefficients(), forward.levels());
        double[][] restored = Images.crop(restoredPadded, original.length, original[0].length);
        return new MethodResult(restored, toMetrics("DWT-Haar", kept, original, restored));
    }

    private MethodMetrics toMetrics(String method, CoefficientKeep.Kept kept,
                                   double[][] original, double[][] restored) {
        double psnr = Psnr.of(original, restored);
        if (!Double.isFinite(psnr)) {
            psnr = 99.99;
        }
        return new MethodMetrics(
                method,
                kept.keptSlots(),
                kept.nonZero(),
                kept.total(),
                kept.keptPercent(),
                kept.coefficientRatio(),
                psnr
        );
    }

    static double[][] padForTransforms(double[][] original) {
        int height = Images.nextPowerOfTwo(original.length);
        int width = Images.nextPowerOfTwo(original[0].length);
        height = Math.max(height, Dct.BLOCK);
        width = Math.max(width, Dct.BLOCK);
        if (height == original.length && width == original[0].length) {
            return original;
        }
        return Images.padTo(original, height, width);
    }

    static String conclusions(List<ExperimentResponse.Level> levels) {
        StringBuilder text = new StringBuilder();
        int dctWins = 0;
        int dwtWins = 0;
        for (ExperimentResponse.Level level : levels) {
            double dct = level.dct().psnr();
            double dwt = level.dwt().psnr();
            if (dct > dwt) {
                dctWins++;
            } else if (dwt > dct) {
                dwtWins++;
            }
        }
        text.append("Спосіб стиснення: збереження заданої частки найбільших за модулем коефіцієнтів; ")
                .append("коефіцієнт стиснення = N_пікселів / K_збережених (це не розмір файлу).\n");
        if (dctWins > dwtWins) {
            text.append("На цьому зображенні DCT дає вищий PSNR на більшості рівнів: блокове косинусне ")
                    .append("перетворення добре описує гладкі ділянки. При сильному відкиданні коефіцієнтів ")
                    .append("з’являються блокові артефакти 8×8.\n");
        } else if (dwtWins > dctWins) {
            text.append("На цьому зображенні Haar DWT дає вищий PSNR на більшості рівнів: вейвлет краще ")
                    .append("локалізує різкі перепади. Артефакти виглядають як сходинки/розмиття на межах, ")
                    .append("без класичної DCT-сітки блоків.\n");
        } else {
            text.append("PSNR DCT і DWT близькі: якість залежить від вмісту кадру (гладкі області vs краї).\n");
        }
        text.append("За великої частки збережених коефіцієнтів обидва методи майже без втрат (PSNR високий). ")
                .append("За малої частки виграш дає той базис, який компактніше представляє енергію зображення.");
        return text.toString();
    }

    private record MethodResult(double[][] restored, MethodMetrics metrics) {
    }
}
