package com.yu.yuaicodemother.core.file.processor;

import com.yu.yuaicodemother.manager.TencentOcrManager;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PdfFileProcessorTest {

    @TempDir
    Path tempDir = System.;

    @Test
    void shouldExtractPdfTextWithoutOcr() throws Exception {
        File file = tempDir.resolve("text.pdf").toFile();
        createPdfWithText(file, "This PDF has enough plain text content to skip OCR fallback in processor.");

        TencentOcrManager ocrManager = mock(TencentOcrManager.class);
        PdfFileProcessor processor = new PdfFileProcessor();
        ReflectionTestUtils.setField(processor, "tencentOcrManager", ocrManager);

        FileProcessResult result = processor.process(file, "https://example.com/text.pdf");

        assertEquals(ProcessStatusEnum.SUCCESS.getValue(), result.getStatus());
        assertTrue(result.getContent().contains("enough plain text"));
        assertEquals("pdfbox", result.getMetadata().get("parseMethod"));
        verifyNoInteractions(ocrManager);
    }

    @Test
    void shouldFallbackToOcrWhenPdfTextIsEmpty() throws Exception {
        File file = tempDir.resolve("scan.pdf").toFile();
        createBlankPdf(file);

        TencentOcrManager ocrManager = mock(TencentOcrManager.class);
        when(ocrManager.recognizePdf(any(File.class))).thenReturn("Recognized by OCR from scan PDF");

        PdfFileProcessor processor = new PdfFileProcessor();
        ReflectionTestUtils.setField(processor, "tencentOcrManager", ocrManager);

        FileProcessResult result = processor.process(file, "https://example.com/scan.pdf");

        assertEquals(ProcessStatusEnum.SUCCESS.getValue(), result.getStatus());
        assertTrue(result.getContent().contains("Recognized by OCR"));
        assertEquals("ocr", result.getMetadata().get("parseMethod"));
        assertTrue(Boolean.TRUE.equals(result.getMetadata().get("ocrUsed")));
        verify(ocrManager).recognizePdf(any(File.class));
    }

    @Test
    void shouldReturnFailedWhenOcrAlsoFails() throws Exception {
        File file = tempDir.resolve("broken.pdf").toFile();
        createBlankPdf(file);

        TencentOcrManager ocrManager = mock(TencentOcrManager.class);
        when(ocrManager.recognizePdf(any(File.class))).thenThrow(new RuntimeException("ocr error"));

        PdfFileProcessor processor = new PdfFileProcessor();
        ReflectionTestUtils.setField(processor, "tencentOcrManager", ocrManager);

        FileProcessResult result = processor.process(file, "https://example.com/broken.pdf");

        assertEquals(ProcessStatusEnum.FAILED.getValue(), result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("OCR"));
    }

    private void createPdfWithText(File file, String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(60, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(file);
        }
    }

    private void createBlankPdf(File file) throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(file);
        }
    }
}
