package com.yu.yuaicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yu.yuaicodemother.model.entity.ChatHistory;
import com.yu.yuaicodemother.mapper.ChatHistoryMapper;
import com.yu.yuaicodemother.service.ChatHistoryService;
import org.springframework.stereotype.Service;

/**
 * 对话历史 服务层实现。
 *
 * @author 鱼🐟
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

}
