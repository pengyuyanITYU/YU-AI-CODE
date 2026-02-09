package com.yu.yuaicodemother;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * LangChain4j 多模态流式响应演示类 (适配 0.35.0+ 版本)
 * <p>
 * 功能描述：
 * 使用新的 chat() 方法和 StreamingChatResponseHandler 处理流式输出。
 * 这种方式是 LangChain4j 当前推荐的响应式处理标准。
 * </p>
 */
public class MultimodalStreamingFix {

    public static void main(String[] args) {
        // 1. 配置参数
        String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        String apiKey = "YOUR_API_KEY"; // 请替换为实际的 API Key

        // 2. 构建流式模型实例 (StreamingChatModel)
        StreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName("qwen3-omni-flash-2025-12-01") // 使用全模态模型
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .build();

        // 3. 准备测试数据
        String imageUrl = "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg";
        UserMessage message = UserMessage.from(
                TextContent.from("请简要描述这张图，并预测画面外可能发生了什么？"),
                ImageContent.from(imageUrl)
        );

        // 4. 调用并订阅 Flux
        System.out.println("\n=== 开始接收流式响应 ===");

        getChatFlux(streamingModel, message)
                .doOnNext(System.out::print) // 实时打印每个字符
                .doOnComplete(() -> System.out.println("\n\n=== 传输完成 ==="))
                .doOnError(e -> System.err.println("\n流异常: " + e.getMessage()))
                .blockLast(); // 仅在演示中阻塞
    }

    /**
     * 将 LangChain4j 的回调式流转为 Project Reactor 的 Flux<String>
     * 适配 LangChain4j 0.35.0+ API
     *
     * @param model 异步模型
     * @param userMessage 用户消息
     * @return 字符流
     */
    public static Flux<String> getChatFlux(StreamingChatModel model, UserMessage userMessage) {
        return Flux.create(sink -> {
            List<ChatMessage> messages = Collections.singletonList(userMessage);

            // 使用最新的 chat 方法
            model.chat(messages, new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    // 对应以前的 onNext，处理增量文本
                    sink.next(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    // 对应以前的 onComplete，传输完成
                    sink.complete();
                }

                @Override
                public void onError(Throwable error) {
                    // 异常处理
                    sink.error(error);
                }
            });
        });
    }
}