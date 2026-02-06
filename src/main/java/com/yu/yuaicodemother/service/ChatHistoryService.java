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
}
