# 手动中止 AI 生成功能的实现经验总结

## 1. 背景需求
在 AI 代码生成场景中，生成过程（尤其是流式输出）可能耗时较长。为了节省 Token 成本并提升用户体验，用户需要能够在发现 AI 生成方向偏离预期时手动中止生成。

## 2. 技术方案
采用 **SSE (Server-Sent Events)** + **Project Reactor (Flux)** + **LangChain4j (TokenStream)** 的全链路响应式方案。

### 2.1 前端：主动断开连接
- **实现方式**：调用 `EventSource.close()`。
- **关键点**：关闭 SSE 连接会触发后端 HTTP 连接的断开，从而向响应式流发送取消信号。

### 2.2 后端：捕获信号与异常中断
- **信号捕获**：在 `Flux.create` 中通过 `sink.onCancel()` 监听取消信号。
- **状态同步**：使用 `AtomicBoolean` 维护 `cancelled` 状态。
- **硬中断**：在 `TokenStream` 的各个回调（`onPartialResponse` 等）中检查状态，若已取消则抛出自定义 `RuntimeException("CANCELLED_BY_USER")`。
- **异常捕获**：在 `onError` 回调中识别该特定异常，执行 `sink.complete()` 而非 `sink.error()`，确保流平稳结束。

## 3. 核心代码实践
```java
sink.onCancel(() -> {
    cancelled.set(true);
    log.info("AI 生成被用户取消");
});

tokenStream.onPartialResponse(partial -> {
    if (cancelled.get()) {
        throw new RuntimeException("CANCELLED_BY_USER"); // 强制中断底层连接
    }
    sink.next(partial);
});
```

## 4. 经验教训
1. **底层中断机制**：LangChain4j 的 `TokenStream` 本身未直接暴露 `stop()` 方法。通过在回调中**抛出异常**是中断底层 OkHttp/Netty 连接的最有效手段。
2. **状态原子性**：必须使用 `AtomicBoolean` 确保在 SSE 取消线程与 AI 回调线程之间的状态同步安全。
3. **区分错误类型**：需要严格区分“用户主动取消”和“系统生成异常”。在日志和前端反馈中，应对取消操作进行静默处理，避免误导用户。
4. **单元测试验证**：由于涉及异步流和连接断开，通过 `AtomicBoolean` 模拟取消信号并验证回调是否抛出预期异常是验证此类逻辑的有效方式。

## 5. 待改进点
- 目前采用抛异常方式中断，未来可关注 LangChain4j 是否会提供原生基于 `Context` 或 `Thread.interrupt()` 的更优雅的中断支持。
