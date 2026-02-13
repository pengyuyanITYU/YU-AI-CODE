package com.yu.yuaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yu.yuaicodemother.model.dto.app.AppAddRequest;
import com.yu.yuaicodemother.model.dto.app.AppChatRequest;
import com.yu.yuaicodemother.model.dto.app.AppQueryRequest;
import com.yu.yuaicodemother.model.dto.app.AppReviewRequest;
import com.yu.yuaicodemother.model.entity.App;

import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.vo.app.AppVO;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.util.List;

/**
 * 应用 服务层。
 *
 * @author 鱼🐟
 */
public interface AppService extends IService<App> {


    AppVO getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 申请精选
     *
     * @param appId
     * @param loginUser
     * @return
     */
    boolean applyForFeatured(Long appId, User loginUser);

    /**
     * 更新用户个人优先级
     *
     * @param appId
     * @param userPriority
     * @param loginUser
     * @return
     */
    boolean updateMyPriority(Long appId, Integer userPriority, User loginUser);

    List<AppVO> getAppVOList(List<App> appList);

    Flux<String> chatToGenCode(AppChatRequest appChatRequest, User loginUser);

    /**
     * 部署应用（上线）
     * 整合了初次部署和重新上线的逻辑
     *
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 部署URL
     */
    String deployApp(Long appId, User loginUser);

    boolean removeById(Serializable id);

    void generateAppScreenshotAsync(Long appId, String appUrl);

    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 下线应用
     *
     * @param appId 应用ID
     */
    void offlineApp(Long appId);

    /**
     * 更新应用生成状态
     *
     * @param appId 应用ID
     * @param genStatus 生成状态（0=未开始，1=生成中，2=生成成功，3=生成失败）
     */
    void updateGenStatus(Long appId, Integer genStatus);

    /**
     * 审核应用精选状态
     *
     * @param appReviewRequest
     * @return
     */
    boolean reviewApp(AppReviewRequest appReviewRequest);

    /**
     * 累加应用 Token 消耗
     *
     * @param appId         应用ID
     * @param inputTokens   输入Token数
     * @param outputTokens  输出Token数
     * @param totalTokens   总Token数
     */
    void incrementTokenUsage(Long appId, long inputTokens, long outputTokens, long totalTokens);
}

