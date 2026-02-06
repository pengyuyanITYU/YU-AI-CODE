package com.yu.yuaicodemother.core.file.processor;

import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WordFileProcessor implements FileContentProcessor {

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder content = new StringBuilder();

            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    String text = paragraph.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        content.append(text).append("\n");
                    }
                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    content.append(extractTable(table));
                }
            }

            String text = content.toString().trim();

            if (text.isEmpty()) {
                return FileProcessResult.builder()
                        .fileType(FileTypeEnum.DOCUMENT.getValue())
                        .url(fileUrl)
                        .status(ProcessStatusEnum.EMPTY.getValue())
                        .errorMessage("Word文档内容为空")
                        .build();
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sizeKB", file.length() / 1024);

            log.info("Word文档处理成功: {}, 内容长度: {}", file.getName(), text.length());

            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.DOCUMENT.getValue())
                    .url(fileUrl)
                    .content(text)
                    .status(ProcessStatusEnum.SUCCESS.getValue())
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("Word文档处理失败: {}", file.getName(), e);
            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.DOCUMENT.getValue())
                    .url(fileUrl)
                    .status(ProcessStatusEnum.FAILED.getValue())
                    .errorMessage("Word文档处理失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean support(String extension) {
        return "docx".equalsIgnoreCase(extension);
    }

    private String extractTable(XWPFTable table) {
        StringBuilder md = new StringBuilder("\n");
        List<XWPFTableRow> rows = table.getRows();

        if (rows.isEmpty()) {
            return "";
        }

        XWPFTableRow headerRow = rows.get(0);
        md.append("|");
        for (XWPFTableCell cell : headerRow.getTableCells()) {
            md.append(" ").append(cell.getText().trim()).append(" |");
        }
        md.append("\n|");

        for (int i = 0; i < headerRow.getTableCells().size(); i++) {
            md.append("-----|");
        }
        md.append("\n");

        for (int i = 1; i < rows.size(); i++) {
            md.append("|");
            for (XWPFTableCell cell : rows.get(i).getTableCells()) {
                md.append(" ").append(cell.getText().trim()).append(" |");
            }
            md.append("\n");
        }

        md.append("\n");
        return md.toString();
    }
}
