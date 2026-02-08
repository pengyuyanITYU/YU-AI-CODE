package com.yu.yuaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yu.yuaicodemother.constant.UserConstant;
import com.yu.yuaicodemother.exception.ErrorCode;
import com.yu.yuaicodemother.exception.ThrowUtils;
import com.yu.yuaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yu.yuaicodemother.model.dto.chathistory.MultiModalContent;
import com.yu.yuaicodemother.model.entity.App;
import com.yu.yuaicodemother.model.entity.ChatHistory;
import com.yu.yuaicodemother.mapper.ChatHistoryMapper;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import com.yu.yuaicodemother.service.AppService;
import com.yu.yuaicodemother.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史 服务层实现。
 *
 * @author 鱼🐟
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{


    @Lazy
    @Resource
    private AppService appService;

    @Resource
    private com.yu.yuaicodemother.service.FileService fileService;

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                String rawMsg = history.getMessage();
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    // 尝试作为多模态 JSON 解析
                    if (JSONUtil.isTypeJSON(rawMsg)) {
                        try {
                            MultiModalContent mmContent = JSONUtil.toBean(rawMsg, MultiModalContent.class);
                            List<Content> contents = new ArrayList<>();
                            if (StrUtil.isNotBlank(mmContent.getText())) {
                                contents.add(TextContent.from(mmContent.getText()));
                            }
                            if (CollUtil.isNotEmpty(mmContent.getAttachments())) {
                                for (MultiModalContent.AttachmentInfo attachment : mmContent.getAttachments()) {
                                    if (FileTypeEnum.IMAGE.getValue().equalsIgnoreCase(attachment.getType())) {
                                        String imageContent = attachment.getContent();
                                        if (StrUtil.isBlank(imageContent) && StrUtil.isNotBlank(attachment.getUrl())) {
                                            FileProcessResult reloadResult = fileService.processFile(attachment.getUrl(), attachment.getFileName());
                                            if (ProcessStatusEnum.SUCCESS.getValue().equals(reloadResult.getStatus())
                                                    && StrUtil.isNotBlank(reloadResult.getContent())) {
                                                imageContent = reloadResult.getContent();
                                            }
                                        }
                                        if (StrUtil.isBlank(imageContent)) {
                                            imageContent = attachment.getUrl();
                                        }
                                        if (StrUtil.isNotBlank(imageContent)) {
                                            contents.add(ImageContent.from(imageContent));
                                        }
                                    } else {
                                        // 文档内容按需解析（Lazy Loading）
                                        String docContent = attachment.getContent();
                                        if (StrUtil.isBlank(docContent)) {
                                            FileProcessResult reloadResult = fileService.processFile(attachment.getUrl(), attachment.getFileName());
                                            if (ProcessStatusEnum.SUCCESS.getValue().equals(reloadResult.getStatus())) {
                                                docContent = reloadResult.getContent();
                                            }
                                        }

                                        if (StrUtil.isNotBlank(docContent)) {
                                            contents.add(TextContent.from(String.format(
                                                    "\n\nUser previously uploaded file \"%s\". Content:\n<file_content>\n%s\n</file_content>\n",
                                                    attachment.getFileName(),
                                                    docContent
                                            )));
                                        } else {
                                            // AI 侧隐式告知：文件损坏或不可达 (AI Note)
                                            contents.add(TextContent.from(String.format(
                                                    "\n\n[System Note: File \"%s\" was found in history but is currently inaccessible/corrupted. Please proceed without its content.]\n",
                                                    attachment.getFileName()
                                            )));
                                        }
                                    }
                                }
                            }
                            chatMemory.add(UserMessage.from(contents));
                            loadedCount++;
                            continue;
                        } catch (Exception e) {
                            log.warn("解析多模态消息 JSON 失败，按普通文本处理: {}", history.getId());
                        }
                    }

                    // 兼容旧的 UserMessage {...} 格式或普通文本
                    String cleanMsg = rawMsg;
                    if (rawMsg.startsWith("UserMessage") && rawMsg.contains("contents = [")) {
                        // 简单正则或截取还原
                        cleanMsg = StrUtil.subBetween(rawMsg, "text = \"", "\"");
                        if (cleanMsg == null) cleanMsg = rawMsg;
                    }
                    chatMemory.add(UserMessage.from(cleanMsg));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(rawMsg));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }


    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }


    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        return addChatMessage(appId, message, null, messageType, userId);
    }

    @Override
    public boolean addChatMessage(Long appId, String message, List<FileProcessResult> fileList, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);

        String finalMessage = message;
        // 如果是用户消息且包含附件，构造 MultiModalContent JSON
        if (ChatHistoryMessageTypeEnum.USER.getValue().equals(messageType) && CollUtil.isNotEmpty(fileList)) {
            MultiModalContent mmContent = new MultiModalContent();
            mmContent.setText(message);
            List<MultiModalContent.AttachmentInfo> attachments = new ArrayList<>();
            for (FileProcessResult file : fileList) {
                // Lean Storage: 持久化时不存储 content，只存元数据
                attachments.add(new MultiModalContent.AttachmentInfo(
                        null,
                        file.getFileName(),
                        file.getFileType(),
                        file.getUrl(),
                        null // 强制置空内容，由 loadChatHistoryToMemory 按需加载
                ));
            }
            mmContent.setAttachments(attachments);
            finalMessage = JSONUtil.toJsonStr(mmContent);
        }

        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(finalMessage)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }


    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);


        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

}
