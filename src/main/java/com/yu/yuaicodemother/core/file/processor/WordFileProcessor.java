package com.yu.yuaicodemother.core.file.processor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WordFileProcessor implements FileContentProcessor {

    private static final int MAX_CHARS = 30000;
    private static final String TRUNCATION_NOTE = "[System Note: Word content truncated due to length limit.]";

    private final Tika tika = new Tika();

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        String extension = FileUtil.extName(file.getName()).toLowerCase();

        try {
            ExtractionResult extractionResult = extractByPoi(file, extension);
            if (StrUtil.isBlank(extractionResult.text())) {
                extractionResult = extractByTika(file);
            }

            if (StrUtil.isBlank(extractionResult.text())) {
                return FileProcessResult.builder()
                        .fileType(FileTypeEnum.DOCUMENT.getValue())
                        .url(fileUrl)
                        .status(ProcessStatusEnum.EMPTY.getValue())
                        .errorMessage("Word file content is empty")
                        .metadata(buildMetadata(file, "empty", 0, false))
                        .build();
            }

            DocumentTextNormalizer.NormalizeResult normalizedResult =
                    DocumentTextNormalizer.normalizeAndLimit(extractionResult.text(), MAX_CHARS, TRUNCATION_NOTE);

            Map<String, Object> metadata = buildMetadata(
                    file,
                    extractionResult.parseMethod(),
                    normalizedResult.originalLength(),
                    normalizedResult.truncated()
            );

            log.info("Word file processed successfully: {}, parseMethod={}, length={}",
                    file.getName(), extractionResult.parseMethod(), normalizedResult.text().length());

            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.DOCUMENT.getValue())
                    .url(fileUrl)
                    .content(normalizedResult.text())
                    .status(ProcessStatusEnum.SUCCESS.getValue())
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("Word file processing failed: {}", file.getName(), e);
            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.DOCUMENT.getValue())
                    .url(fileUrl)
                    .status(ProcessStatusEnum.FAILED.getValue())
                    .errorMessage("Word file processing failed: " + e.getMessage())
                    .metadata(buildMetadata(file, "failed", 0, false))
                    .build();
        }
    }

    @Override
    public boolean support(String extension) {
        return "doc".equalsIgnoreCase(extension) || "docx".equalsIgnoreCase(extension);
    }

    private ExtractionResult extractByPoi(File file, String extension) {
        try {
            if ("docx".equalsIgnoreCase(extension)) {
                return new ExtractionResult(extractDocx(file), "poi-docx");
            }
            if ("doc".equalsIgnoreCase(extension)) {
                return new ExtractionResult(extractDoc(file), "poi-doc");
            }
        } catch (Exception e) {
            log.warn("POI extraction failed for Word file: {}", file.getName(), e);
        }
        return new ExtractionResult("", "poi-empty");
    }

    private ExtractionResult extractByTika(File file) {
        try {
            tika.setMaxStringLength(MAX_CHARS + 2000);
            String text = tika.parseToString(file);
            return new ExtractionResult(text, "tika-fallback");
        } catch (Exception e) {
            log.warn("Tika fallback extraction failed for Word file: {}", file.getName(), e);
            return new ExtractionResult("", "tika-failed");
        }
    }

    private String extractDocx(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder content = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (StrUtil.isNotBlank(text)) {
                        content.append(text).append("\n");
                    }
                } else if (element instanceof XWPFTable table) {
                    content.append(extractTable(table));
                }
            }
            return content.toString();
        }
    }

    private String extractDoc(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private Map<String, Object> buildMetadata(File file, String parseMethod, int charCount, boolean truncated) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parseMethod", parseMethod);
        metadata.put("charCount", charCount);
        metadata.put("truncated", truncated);
        metadata.put("fileSizeKB", file.length() / 1024);
        return metadata;
    }

    private String extractTable(XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return "";
        }

        int columnSize = rows.stream()
                .mapToInt(row -> row.getTableCells().size())
                .max()
                .orElse(0);

        if (columnSize <= 0) {
            return "";
        }

        StringBuilder markdown = new StringBuilder("\n");
        appendRow(markdown, rows.get(0), columnSize);
        markdown.append("|");
        for (int i = 0; i < columnSize; i++) {
            markdown.append(" --- |");
        }
        markdown.append("\n");

        for (int i = 1; i < rows.size(); i++) {
            appendRow(markdown, rows.get(i), columnSize);
        }

        markdown.append("\n");
        return markdown.toString();
    }

    private void appendRow(StringBuilder markdown, XWPFTableRow row, int columnSize) {
        markdown.append("|");
        for (int i = 0; i < columnSize; i++) {
            String cellText = "";
            if (i < row.getTableCells().size()) {
                XWPFTableCell cell = row.getTableCells().get(i);
                String rawCellText = cell.getText();
                cellText = (rawCellText == null ? "" : rawCellText).replace("\n", " ").trim();
            }
            markdown.append(" ").append(cellText).append(" |");
        }
        markdown.append("\n");
    }

    private record ExtractionResult(String text, String parseMethod) {
    }
}


