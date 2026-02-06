package com.yu.yuaicodemother.manager;

import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRResponse;
import com.tencentcloudapi.ocr.v20181119.models.TextDetection;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

@Component
@Slf4j
public class TencentOcrManager {

    @Resource
    private OcrClient ocrClient;

    public String recognizePdf(File pdfFile) throws Exception {
        PDDocument document = Loader.loadPDF(pdfFile);
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder result = new StringBuilder();

        int pageCount = document.getNumberOfPages();
        int maxPages = Math.min(pageCount, 10);

        log.info("开始OCR识别PDF: {}, 共{}页, 识别前{}页", pdfFile.getName(), pageCount, maxPages);

        for (int i = 0; i < maxPages; i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, 300);

            String base64 = imageToBase64(image);

            GeneralBasicOCRRequest req = new GeneralBasicOCRRequest();
            req.setImageBase64(base64);

            GeneralBasicOCRResponse resp = ocrClient.GeneralBasicOCR(req);

            result.append("--- 第").append(i + 1).append("页 ---\n");
            for (TextDetection text : resp.getTextDetections()) {
                result.append(text.getDetectedText()).append("\n");
            }
            result.append("\n");

            log.info("PDF第{}页OCR识别完成", i + 1);
        }

        document.close();

        if (pageCount > maxPages) {
            result.append("\n[注: 文档共").append(pageCount)
                    .append("页,仅识别了前").append(maxPages).append("页]\n");
        }

        return result.toString();
    }

    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
