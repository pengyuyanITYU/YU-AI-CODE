package com.yu.yuaicodemother.ai;

import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiModalMessageBuilderTest {

    private final MultiModalMessageBuilder builder = new MultiModalMessageBuilder();

    @Test
    void shouldUseImageBase64ContentBeforeUrl() {
        FileProcessResult imageResult = FileProcessResult.builder()
                .fileType(FileTypeEnum.IMAGE.getValue())
                .fileName("sample.png")
                .url("https://example.com/sample.png")
                .content("data:image/png;base64,AAAA")
                .status(ProcessStatusEnum.SUCCESS.getValue())
                .build();

        List<Content> contents = builder.buildMessage("describe image", List.of(imageResult));

        assertEquals(2, contents.size());
        assertTrue(contents.get(1) instanceof ImageContent);
        assertTrue(contents.get(1).toString().contains("data:image/png;base64,AAAA"));
    }

    @Test
    void shouldBuildDocumentContextWithMetadata() {
        FileProcessResult documentResult = FileProcessResult.builder()
                .fileType(FileTypeEnum.DOCUMENT.getValue())
                .fileName("spec.pdf")
                .content("document body")
                .status(ProcessStatusEnum.SUCCESS.getValue())
                .metadata(Map.of("parseMethod", "pdfbox", "truncated", true))
                .build();

        List<Content> contents = builder.buildMessage("read document", List.of(documentResult));

        assertEquals(2, contents.size());
        assertTrue(contents.get(1) instanceof TextContent);
        String text = contents.get(1).toString();
        assertTrue(text.contains("parseMethod: pdfbox"));
        assertTrue(text.contains("content truncated"));
    }

    @Test
    void shouldSkipFailedAttachment() {
        FileProcessResult failedResult = FileProcessResult.builder()
                .fileType(FileTypeEnum.DOCUMENT.getValue())
                .fileName("broken.pdf")
                .content("cannot use")
                .status(ProcessStatusEnum.FAILED.getValue())
                .build();

        List<Content> contents = builder.buildMessage("hello", List.of(failedResult));

        assertEquals(1, contents.size());
        assertTrue(contents.get(0) instanceof TextContent);
    }
}
