package com.yu.yuaicodemother.core.file.processor;

import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class PptFileProcessor implements FileContentProcessor {

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            StringBuilder content = new StringBuilder();
            int slideNumber = 1;

            for (XSLFSlide slide : ppt.getSlides()) {
                content.append("## 第").append(slideNumber).append("页\n\n");

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            content.append(text).append("\n");
                        }
                    }
                }

                content.append("\n");
                slideNumber++;
            }

            String text = content.toString().trim();

            if (text.isEmpty()) {
                return FileProcessResult.builder()
                        .fileType(FileTypeEnum.DOCUMENT.getValue())
                        .url(fileUrl)
                        .status(ProcessStatusEnum.EMPTY.getValue())
                        .errorMessage("PPT文档内容为空")
                        .build();
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sizeKB", file.length() / 1024);
            metadata.put("slides", ppt.getSlides().size());

            log.info("PPT文档处理成功: {}, 页数: {}, 内容长度: {}",
                    file.getName(), ppt.getSlides().size(), text.length());

            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.DOCUMENT.getValue())
                    .url(fileUrl)
                    .content(text)
                    .status(ProcessStatusEnum.SUCCESS.getValue())
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("PPT文档处理失败: {}", file.getName(), e);
            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.DOCUMENT.getValue())
                    .url(fileUrl)
                    .status(ProcessStatusEnum.FAILED.getValue())
                    .errorMessage("PPT文档处理失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean support(String extension) {
        return "pptx".equalsIgnoreCase(extension) || "ppt".equalsIgnoreCase(extension);
    }
}
