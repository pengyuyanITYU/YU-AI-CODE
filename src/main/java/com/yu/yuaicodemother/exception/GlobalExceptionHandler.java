package com.yu.yuaicodemother.exception;

import cn.hutool.json.JSONUtil;
import com.yu.yuaicodemother.common.BaseResponse;
import com.yu.yuaicodemother.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Map;

// 假设的导入类
// import io.swagger.v3.oas.annotations.Hidden;
// import cn.hutool.json.JSONUtil;

/**
 * 全局异常处理器
 * <p>
 * 作用：拦截 Controller 层抛出的所有异常，统一处理响应格式。
 * 特性：能够自动识别普通 HTTP 请求和 SSE (Server-Sent Events) 流式请求，
 * 并分别返回 JSON 格式或流式 Event 格式的错误信息。
 */
@Hidden // 在 Swagger/Knif4j 接口文档中隐藏此类，避免作为接口展示
@RestControllerAdvice // 声明这是一个全局异常处理切面，且默认返回 JSON (@ResponseBody)
@Slf4j // 自动注入 log 对象用于日志记录
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常 (BusinessException)
     * <p>
     * 业务异常通常包含明确的错误码 (code) 和错误提示 (message)。
     *
     * @param e 捕获到的业务异常对象
     * @return 如果是普通请求返回 BaseResponse，如果是 SSE 请求返回 null (由内部手动处理响应)
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        // 1. 记录错误堆栈日志，方便排查问题
        log.error("BusinessException", e);

        // 2. 尝试作为 SSE 流式请求处理
        // 如果 handleSseError 返回 true，说明这是一个 SSE 请求，并且错误信息已经写入 Response 流中
        if (handleSseError(e.getCode(), e.getMessage())) {
            return null; // 返回 null 告诉 Spring MVC：“响应我已经处理完了，你不需要再做任何序列化操作”
        }

        // 3. 如果是普通 HTTP 请求，返回标准的 JSON 响应结构
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 捕获运行时异常 (RuntimeException)
     * <p>
     * 这是一个兜底方法，处理所有未被特定 Handler 捕获的未知异常。
     * 为了安全起见，统一屏蔽具体的报错细节，只返回 "系统错误"。
     *
     * @param e 捕获到的运行时异常
     * @return 同上
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        // 记录未知的系统异常，这通常是需要修复的 Bug
        log.error("RuntimeException", e);

        // 尝试作为 SSE 请求处理，使用统一的系统错误码
        if (handleSseError(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误")) {
            return null;
        }

        // 普通请求返回统一的系统错误提示
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 核心处理逻辑：识别并处理 SSE 请求的错误响应
     * <p>
     * 如果是 SSE 请求，不能直接返回 JSON，否则前端的 EventSource 会连接中断或报错。
     * 必须按照 SSE 协议格式 (data: ...) 推送错误事件。
     *
     * @param errorCode    错误码
     * @param errorMessage 错误描述
     * @return true: 表示当前请求是 SSE 请求并已处理完毕; false: 表示是普通请求
     */
    private boolean handleSseError(int errorCode, String errorMessage) {
        // 1. 获取当前的 HttpServletRequest 和 HttpServletResponse 对象
        // RequestContextHolder 使用 ThreadLocal 存储请求上下文，确保线程安全
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false; // 非 Web 上下文（如定时任务），直接跳过
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        // 2. 判断当前请求是否为 SSE 流式请求
        // 依据 A: 请求头 Accept 包含 text/event-stream (标准做法)
        // 依据 B: 请求路径包含 /chat/gen/code (特定业务接口兜底，防止前端漏传 Header)
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();

        if ((accept != null && accept.contains("text/event-stream")) ||
                uri.contains("/chat/gen/code")) {

            try {
                // 3. 设置 SSE 专用的响应头
                // 必须设置，否则浏览器可能不会将其识别为流，或者会缓存响应
                response.setContentType("text/event-stream");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Cache-Control", "no-cache"); // 禁止缓存
                response.setHeader("Connection", "keep-alive");  // 保持长连接

                // 4. 构造错误数据的 Payload
                Map<String, Object> errorData = Map.of(
                        "error", true,        // 标识这是一个错误包
                        "code", errorCode,
                        "message", errorMessage
                );
                String errorJson = JSONUtil.toJsonStr(errorData);

                // 5. 推送自定义错误事件 (Event: business-error)
                // 格式说明：
                // event: 事件名\n
                // data: JSON数据\n\n (两个换行符代表一条消息结束)
                String sseData = "event: business-error\ndata: " + errorJson + "\n\n";
                response.getWriter().write(sseData);
                response.getWriter().flush(); // 强制刷入缓冲区，确保前端立即收到

                // 6. 推送结束事件 (Event: done)
                // 告诉前端：“流传输结束了，你可以关闭连接了”，防止前端一直 Pending 或重试
                response.getWriter().write("event: done\ndata: {}\n\n");
                response.getWriter().flush();

                // 返回 true，表示 SSE 请求处理成功
                return true;

            } catch (IOException ioException) {
                // 如果在写入流的过程中发生 IO 异常（例如客户端断开连接），记录日志
                log.error("Failed to write SSE error response", ioException);
                // 即使写入失败，也应视为 SSE 请求处理逻辑已执行，避免再次返回 JSON 导致二次报错
                return true;
            }
        }

        // 返回 false，表示这不是 SSE 请求，应交由外部逻辑按普通 JSON 处理
        return false;
    }
}