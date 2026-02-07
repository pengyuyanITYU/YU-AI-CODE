package com.yu.yuaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yu.yuaicodemother.ai.AiCodeGenTypeRoutingService;
import com.yu.yuaicodemother.ai.AiCodeGenerateAppNameService;
import com.yu.yuaicodemother.ai.MultiModalMessageBuilder;
import com.yu.yuaicodemother.ai.model.CodeGenTypeRoutingResult;
import com.yu.yuaicodemother.constant.AppConstant;
import com.yu.yuaicodemother.core.AiCodeGeneratorFacade;
import com.yu.yuaicodemother.core.builder.VueProjectBuilder;
import com.yu.yuaicodemother.core.handler.StreamHandlerExecutor;
import com.yu.yuaicodemother.exception.BusinessException;
import com.yu.yuaicodemother.exception.ErrorCode;
import com.yu.yuaicodemother.exception.ThrowUtils;
import com.yu.yuaicodemother.mapper.AppMapper;
import com.yu.yuaicodemother.model.dto.app.*;
import com.yu.yuaicodemother.model.entity.App;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.enums.*;
import com.yu.yuaicodemother.model.vo.app.AppVO;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import com.yu.yuaicodemother.model.vo.user.UserVO;
import com.yu.yuaicodemother.monitor.MonitorContext;
import com.yu.yuaicodemother.monitor.MonitorContextHolder;
import com.yu.yuaicodemother.service.*;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author 鱼🐟
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private FileService fileService;

    @Resource
    private MultiModalMessageBuilder multiModalMessageBuilder;

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Resource
    private AiCodeGenerateAppNameService aiCodeGenerateAppNameService;

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        List<AppChatFile> fileList = appAddRequest.getFileList();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");

        // 1. 处理文件列表，收集处理结果
        List<FileProcessResult> processedFiles = new ArrayList<>();
        if (CollUtil.isNotEmpty(fileList)) {
            for (AppChatFile appChatFile : fileList) {
                try {
                    FileProcessResult result = fileService.processFile(appChatFile.getUrl(), appChatFile.getFileName());
                    if (ProcessStatusEnum.SUCCESS.getValue().equals(result.getStatus())) {
                        processedFiles.add(result);
                    }
                } catch (Exception e) {
                    log.error("文件处理失败: {}", appChatFile.getFileName(), e);
                }
            }
        }

        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        app.setVisualRange(true);
        // 初始化状态
        app.setDeployStatus(AppDeployStatusEnum.NOT_DEPLOYED.getValue());
        app.setGenStatus(AppGenStatusEnum.NOT_STARTED.getValue());
        String appName = null;
        try {
            appName = aiCodeGenerateAppNameService.generateAppName(initPrompt);
        } catch (Exception e) {
            log.error("应用名称生成失败");
        }
        if (appName == null) {
            // 如果ai生成结果为null,应用名称为initPrompt 前 15 位
            app.setAppName(StrUtil.sub(initPrompt, 0, 15));
        } else {
            // 截断 AI 生成的名称，确保不超过 15 位
            app.setAppName(StrUtil.sub(appName, 0, 15));
        }

        // 构建多模态消息用于路由选择
        UserMessage multimodalMessage = multiModalMessageBuilder.buildMessage(initPrompt, processedFiles);
        // 使用 AI 智能选择代码生成类型
        CodeGenTypeRoutingResult result = aiCodeGenTypeRoutingService.routeCodeGenType(multimodalMessage);
        CodeGenTypeEnum selectedCodeGenType = result.getType();
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean resultSave = this.save(app);
        ThrowUtils.throwIf(!resultSave, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    @Override
    public Flux<String> chatToGenCode(AppChatRequest appChatRequest, User loginUser) {
        Long appId = appChatRequest.getAppId();
        String message = appChatRequest.getMessage();
        List<AppChatFile> fileList = appChatRequest.getFileList();

        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        
        // 4. 处理文件列表，收集处理结果
        List<FileProcessResult> processedFiles = new ArrayList<>();
        if (CollUtil.isNotEmpty(fileList)) {
            for (AppChatFile appChatFile : fileList) {
                try {
                    FileProcessResult result = fileService.processFile(appChatFile.getUrl(), appChatFile.getFileName());
                    if (ProcessStatusEnum.SUCCESS.getValue().equals(result.getStatus())) {
                        processedFiles.add(result);
                    }
                } catch (Exception e) {
                    log.error("文件处理失败: {}", appChatFile.getFileName(), e);
                }
            }
        }

        // 5. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        
        // 6. 通过校验后,添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, message, processedFiles, ChatHistoryMessageTypeEnum.USER.getValue(),
                loginUser.getId());
        MonitorContextHolder.setContext(MonitorContext.builder()
                .appId(appId.toString())
                .userId(loginUser.getId().toString())
                .build());
        
        // 7. 更新生成状态为"生成中"
        updateGenStatus(appId, AppGenStatusEnum.GENERATING.getValue());
        
        // 8. 调用 AI 生成代码（流式）
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, processedFiles, codeGenTypeEnum, appId);
        
        // 9. 收集AI响应内容并在完成后记录到对话历史
        Flux<String> result = streamHandlerExecutor
                .doExecute(contentFlux, chatHistoryService, appId, loginUser, codeGenTypeEnum)
                .doOnComplete(() -> {
                    // 流正常完成，更新状态为生成成功
                    updateGenStatus(appId, AppGenStatusEnum.GENERATED_SUCCESS.getValue());
                })
                .doOnError(error -> {
                    // 流发生错误，更新状态为生成失败
                    log.error("应用生成失败: {}", error.getMessage());
                    updateGenStatus(appId, AppGenStatusEnum.GENERATED_FAILED.getValue());
                })
                .doFinally(signalType ->
                // 流结束后清理 无论成功/失败/取消
                MonitorContextHolder.clearContext());
        return result;
    }

    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用目录下生成的文件
        App app = getById(appId);
        try {
            String output = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + app.getCodeGenType() + "_" + appId;
            FileUtil.del(output);
        } catch (Exception e) {
            // 删除失败，记录日志但不阻止应用删除
            log.error("删除应用输出目录失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 删除部署目录下生成的文件
        String deployKey = app.getDeployKey();
        if (deployKey == null || deployKey.isEmpty()) {
            return super.removeById(id);
        }
        try {
            String deploy = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
            FileUtil.del(deploy);
        } catch (Exception e) {
            log.error("删除应用部署目录失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 删除应用截图
        screenshotService.deleteByAppId(appId);
        // 删除应用
        return super.removeById(id);
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 第一次部署逻辑
        if (StrUtil.isBlank(deployKey)) {
            // 如果第一次部署但状态已经是上线，报错（数据异常）
            if (Integer.valueOf(AppDeployStatusEnum.ONLINE.getValue()).equals(app.getDeployStatus())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署状态异常，请联系管理员");
            }
            // 生成 6 位 deployKey（大小写字母 + 数字）
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 9. 更新应用的 deployKey、部署时间和部署状态
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        updateApp.setDeployStatus(AppDeployStatusEnum.ONLINE.getValue());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 返回可访问的 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineApp(Long appId) {
        // 1. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        String deployKey = app.getDeployKey();
        // 如果没有部署过，无需下线
        if (StrUtil.isBlank(deployKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用未部署，无需下线");
        }

        // 2. 更新应用状态为"已下线"（先改状态，确保即使文件删除失败，访问也会被拦截）
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployStatus(AppDeployStatusEnum.OFFLINE.getValue());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用状态失败");

        // 3. 尝试删除部署目录（下线）
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            File deployDir = new File(deployDirPath);
            if (deployDir.exists()) {
                FileUtil.del(deployDir);
                log.info("应用已下线，删除部署目录: {}", deployDirPath);
            }
        } catch (Exception e) {
            // 记录日志但不影响数据库状态变更的完成
            log.error("下线应用时删除部署目录失败 (appId: {}): {}", appId, e.getMessage());
        }
    }

    @Override
    public void updateGenStatus(Long appId, Integer genStatus) {
        if (appId == null || appId <= 0 || genStatus == null) {
            return;
        }
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setGenStatus(genStatus);
        this.updateById(updateApp);
    }

    @Override
    public boolean applyForFeatured(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可申请
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 已经是申请中或已精选，不处理
        if (AppFeaturedStatusEnum.PENDING.getValue() == app.getFeaturedStatus()
                || AppFeaturedStatusEnum.FEATURED.getValue() == app.getFeaturedStatus()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已在申请中或已精选");
        }
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setFeaturedStatus(AppFeaturedStatusEnum.PENDING.getValue());
        // 重新申请时清空上次审核备注
        updateApp.setReviewMessage("");
        return this.updateById(updateApp);
    }


    @Override
    public boolean updateMyPriority(Long appId, Integer userPriority, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可更新个人优先级
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setUserPriority(userPriority);
        return this.updateById(updateApp);
    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Integer featuredStatus = appQueryRequest.getFeaturedStatus();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("featured_status", featuredStatus)
                .eq("userId", userId);

        // 设置排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认排序
            if (userId != null) {
                // 个人工作台：用户优先级 -> 创建时间
                queryWrapper.orderBy("user_priority", false);
                queryWrapper.orderBy("createTime", false);
            } else {
                // 公共列表/精选列表：全局优先级 -> 创建时间
                queryWrapper.orderBy("priority", false);
                queryWrapper.orderBy("createTime", false);
            }
        }
        return queryWrapper;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean reviewApp(AppReviewRequest appReviewRequest) {
        ThrowUtils.throwIf(appReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = appReviewRequest.getId();
        Integer featuredStatus = appReviewRequest.getFeaturedStatus();
        String reviewMessage = appReviewRequest.getReviewMessage();

        // 校验
        AppFeaturedStatusEnum enumByValue = AppFeaturedStatusEnum.getEnumByValue(featuredStatus);
        if (id == null || enumByValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 如果是拒绝，必须填写原因
        if (AppFeaturedStatusEnum.REJECTED.equals(enumByValue) && StrUtil.isBlank(reviewMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请填写拒绝原因");
        }
        // 判断是否存在
        App oldApp = this.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 状态检查：只有 PENDING 状态的应用可以被审核（通过或拒绝）
        if (!Integer.valueOf(AppFeaturedStatusEnum.PENDING.getValue()).equals(oldApp.getFeaturedStatus())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该应用当前不在审核队列中");
        }

        App app = new App();
        app.setId(id);
        app.setFeaturedStatus(featuredStatus);
        app.setReviewMessage(reviewMessage);
        return this.updateById(app);
    }
}
