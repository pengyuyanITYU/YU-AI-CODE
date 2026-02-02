package com.yu.yuaicodemother.core;

import com.yu.yuaicodemother.ai.AiCodeGeneratorService;
import com.yu.yuaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.yu.yuaicodemother.ai.model.HtmlCodeResult;
import com.yu.yuaicodemother.core.processor.AiStreamProcessor;
import com.yu.yuaicodemother.core.processor.SimpleStreamProcessor;
import com.yu.yuaicodemother.core.processor.VueProjectStreamProcessor;
import com.yu.yuaicodemother.exception.BusinessException;
import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AiCodeGeneratorFacadeTest {

    @InjectMocks
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Mock
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Mock
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Spy
    private List<AiStreamProcessor> streamProcessors = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 实例化真实的处理器或 Mock
        SimpleStreamProcessor simpleProcessor = new SimpleStreamProcessor();
        // 因为 VueProcessor 涉及 Builder 注入，这里 Mock 它
        VueProjectStreamProcessor vueProcessor = mock(VueProjectStreamProcessor.class);
        when(vueProcessor.supports(CodeGenTypeEnum.VUE_PROJECT)).thenReturn(true);
        
        streamProcessors.add(simpleProcessor);
        streamProcessors.add(vueProcessor);

        when(aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(anyLong(), any())).thenReturn(aiCodeGeneratorService);
    }

    @Test
    public void testGenerateAndSaveCodeStream_HTML() {
        String message = "test html";
        Long appId = 1L;
        Flux<String> mockFlux = Flux.just("<html>", "</html>");
        
        when(aiCodeGeneratorService.generateHTMLCodeStream(message)).thenReturn(mockFlux);

        Flux<String> result = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, CodeGenTypeEnum.HTML, appId);

        StepVerifier.create(result)
                .expectNext("<html>")
                .expectNext("</html>")
                .verifyComplete();
    }

    @Test
    public void testGenerateAndSaveCodeStream_MultiFile() {
        String message = "test multi";
        Long appId = 1L;
        Flux<String> mockFlux = Flux.just("file1", "file2");
        
        when(aiCodeGeneratorService.generateMultiFileCodeStream(message)).thenReturn(mockFlux);

        Flux<String> result = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, CodeGenTypeEnum.MULTI_FILE, appId);

        StepVerifier.create(result)
                .expectNext("file1")
                .expectNext("file2")
                .verifyComplete();
    }

    @Test
    public void testGenerateAndSaveCodeStream_VueProject() {
        String message = "test vue";
        Long appId = 1L;
        TokenStream mockTokenStream = mock(TokenStream.class);
        
        when(aiCodeGeneratorService.generateVueProjectCode(eq(appId), eq(message))).thenReturn(mockTokenStream);

        aiCodeGeneratorFacade.generateAndSaveCodeStream(message, CodeGenTypeEnum.VUE_PROJECT, appId);
        
        // 验证 Mock 处理器是否被查找
        verify(streamProcessors.get(1)).supports(CodeGenTypeEnum.VUE_PROJECT);
    }

    @Test
    public void testGenerateAndSaveCodeStream_UnsupportedType() {
        // 测试不支持的类型抛出异常
        assertThrows(BusinessException.class, () -> {
            aiCodeGeneratorFacade.generateAndSaveCodeStream("test", null, 1L);
        });
    }

    @Test
    public void testGenerateAndSaveCode_HTML() {
        String message = "test html";
        Long appId = 1L;
        HtmlCodeResult mockResult = new HtmlCodeResult();
        
        when(aiCodeGeneratorService.generateHTMLCode(message)).thenReturn(mockResult);

        aiCodeGeneratorFacade.generateAndSaveCode(message, CodeGenTypeEnum.HTML, appId);
        
        verify(aiCodeGeneratorService).generateHTMLCode(message);
    }

    @Test
    public void testGenerateAndSaveCode_UnsupportedType() {
        assertThrows(BusinessException.class, () -> {
            aiCodeGeneratorFacade.generateAndSaveCode("test", null, 1L);
        });
    }
}
