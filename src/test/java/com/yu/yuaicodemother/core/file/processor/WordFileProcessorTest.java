package com.yu.yuaicodemother.core.file.processor;

import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordFileProcessorTest {

    private final WordFileProcessor processor = new WordFileProcessor();

    @TempDir
    Path tempDir;

    @Test
    void shouldProcessDocxWithParagraphAndTable() throws Exception {
        File file = tempDir.resolve("sample.docx").toFile();
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("This is a test paragraph");

            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("Header1");
            table.getRow(0).getCell(1).setText("Header2");
            table.getRow(1).getCell(0).setText("Cell1");
            table.getRow(1).getCell(1).setText("Cell2");

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                document.write(outputStream);
            }
        }

        FileProcessResult result = processor.process(file, "https://example.com/sample.docx");

        assertEquals(ProcessStatusEnum.SUCCESS.getValue(), result.getStatus());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("This is a test paragraph"));
        assertTrue(result.getContent().contains("Header1"));
        assertNotNull(result.getMetadata());
        assertEquals("poi-docx", result.getMetadata().get("parseMethod"));
    }

    @Test
    void shouldReturnEmptyStatusForEmptyDocx() throws Exception {
        File file = tempDir.resolve("empty.docx").toFile();
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream outputStream = new FileOutputStream(file)) {
            document.write(outputStream);
        }

        FileProcessResult result = processor.process(file, "https://example.com/empty.docx");

        assertEquals(ProcessStatusEnum.EMPTY.getValue(), result.getStatus());
        assertNotNull(result.getMetadata());
    }

    @Test
    void shouldTruncateTooLongWordContent() throws Exception {
        File file = tempDir.resolve("long.docx").toFile();
        String longText = "A".repeat(32000);

        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText(longText);
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                document.write(outputStream);
            }
        }

        FileProcessResult result = processor.process(file, "https://example.com/long.docx");

        assertEquals(ProcessStatusEnum.SUCCESS.getValue(), result.getStatus());
        assertTrue(Boolean.TRUE.equals(result.getMetadata().get("truncated")));
        assertTrue(result.getContent().contains("truncated"));
    }

    @Test
    void shouldSupportDocAndDocxExtension() {
        assertTrue(processor.support("doc"));
        assertTrue(processor.support("docx"));
    }
}
