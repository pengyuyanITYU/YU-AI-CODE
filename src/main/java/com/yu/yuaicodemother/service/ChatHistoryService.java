package com.yu.yuaicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yu.yuaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yu.yuaicodemother.model.entity.ChatHistory;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author 鱼🐟
 */
public interface ChatHistoryService extends IService<ChatHistory> {


    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    boolean addChatMessage(Long appId, String message, List<FileProcessResult> fileList, String messageType, Long userId);


    boolean deleteByAppId(Long appId);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 导出对话历史为 Markdown 格式
     *
     * @param appId    应用ID
     * @param loginUser 当前登录用户
     * @return Markdown 格式的对话内容
     */
    String exportChatHistoryToMarkdown(Long appId, User loginUser);

    /**
     * 【智能记忆支持】获取指定ID之后的未总结消息 - 支持增量总结
     *
     * <p>功能说明：
     * 为智能记忆系统(SmartMemoryService)提供增量查询能力。
     * 只返回上次总结覆盖范围之后的新消息，避免重复处理已总结的历史。</p>
     *
     * <p>使用场景：
     * - SmartMemoryServiceImpl.triggerSummaryIfNeeded() 触发总结时调用
     * - 根据上次SHORT摘要的coveredTo字段，查询之后的新消息
     * - 支持首次总结(afterId=null)和增量总结(afterId=lastCoveredTo)</p>
     *
     * <p>查询逻辑：
     * SELECT * FROM chat_history
     * WHERE appId = ? AND id > afterId
     * ORDER BY createTime ASC</p>
     *
     * @param appId 应用ID，标识查询哪个应用的对话
     * @param afterId 起始消息ID（不包含），null表示从第一条开始查询
     * @return 未总结的消息列表，按时间正序排列（旧消息在前）
     * @see com.yu.yuaicodemother.service.impl.SmartMemoryServiceImpl#triggerSummaryIfNeeded
     * @see com.yu.yuaicodemother.service.impl.SmartMemoryServiceImpl#getUnsummarizedMessages
     */
    List<ChatHistory> getUnsummarizedMessages(Long appId, Long afterId);
}
