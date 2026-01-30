package com.yu.yuaicodemother.core.handler;

import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yu.yuaicodemother.service.AppVersionService;
import com.yu.yuaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器
 * 根据代码生成类型创建合适的流处理器：
 * 1. 传统的 Flux<String> 流（HTML、MULTI_FILE） -> SimpleTextStreamHandler
 * 2. TokenStream 格式的复杂流（VUE_PROJECT） -> JsonMessageStreamHandler
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    @Resource
    private AppVersionService appVersionService;

    /**
     * 创建流处理器并处理聊天历史记录
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @param codeGenType        代码生成类型
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
            ChatHistoryService chatHistoryService,
            long appId, User loginUser, CodeGenTypeEnum codeGenType) {
        Flux<String> resultFlux = switch (codeGenType) {
            case VUE_PROJECT ->
                jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            case HTML, MULTI_FILE ->
                new SimpleTextStreamHandler().handle(originFlux, chatHistoryService, appId, loginUser);
        };
        return resultFlux.doOnComplete(() -> {
            try {
                appVersionService.createVersion(appId, "AI生成代码");
            } catch (Exception e) {
                log.error("版本创建失败: appId={}, error={}", appId, e.getMessage());
            }
        });
    }
}
