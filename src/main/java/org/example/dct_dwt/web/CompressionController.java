package org.example.dct_dwt.web;

import org.example.dct_dwt.image.Images;
import org.example.dct_dwt.service.CompressionService;
import org.example.dct_dwt.web.dto.CompareResponse;
import org.example.dct_dwt.web.dto.ExperimentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class CompressionController {

    private final CompressionService compressionService;

    public CompressionController(CompressionService compressionService) {
        this.compressionService = compressionService;
    }

    @PostMapping(path = "/compress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompareResponse compress(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "keepPercent", defaultValue = "15") double keepPercent
    ) throws IOException {
        return compressionService.compress(loadImage(file), toFraction(keepPercent));
    }

    @PostMapping(path = "/experiments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExperimentResponse experiments(
            @RequestParam(value = "file", required = false) MultipartFile file
    ) throws IOException {
        return compressionService.experiments(loadImage(file));
    }

    private static double[][] loadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Images.synthetic512();
        }
        return Images.loadGrayscale(file.getInputStream());
    }

    private static double toFraction(double keepPercent) {
        if (keepPercent < 0.1) {
            return 0.001;
        }
        if (keepPercent > 100.0) {
            return 1.0;
        }
        return keepPercent / 100.0;
    }
}
