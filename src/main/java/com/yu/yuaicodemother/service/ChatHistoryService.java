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
}
