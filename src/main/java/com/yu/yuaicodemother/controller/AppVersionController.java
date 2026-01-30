package com.yu.yuaicodemother.controller;

import com.yu.yuaicodemother.common.BaseResponse;
import com.yu.yuaicodemother.common.ResultUtils;
import com.yu.yuaicodemother.exception.BusinessException;
import com.yu.yuaicodemother.exception.ErrorCode;
import com.yu.yuaicodemother.exception.ThrowUtils;
import com.yu.yuaicodemother.model.entity.App;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.vo.app.AppVersionDiffVO;
import com.yu.yuaicodemother.model.vo.app.AppVersionVO;
import com.yu.yuaicodemother.service.AppService;
import com.yu.yuaicodemother.service.AppVersionService;
import com.yu.yuaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/version")
@Slf4j
public class AppVersionController {

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @GetMapping("/list")
    public BaseResponse<List<AppVersionVO>> listVersions(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用版本");
        }
        List<AppVersionVO> versions = appVersionService.listVersions(appId);
        return ResultUtils.success(versions);
    }

    @PostMapping("/rollback")
    public BaseResponse<Boolean> rollbackVersion(@RequestParam Long appId,
            @RequestParam Integer version,
            HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(version == null || version <= 0, ErrorCode.PARAMS_ERROR, "版本号无效");
        User loginUser = userService.getLoginUser(request);
        boolean result = appVersionService.rollbackToVersion(appId, version, loginUser);
        return ResultUtils.success(result);
    }

    @GetMapping("/diff")
    public BaseResponse<AppVersionDiffVO> compareVersions(@RequestParam Long appId,
            @RequestParam Integer v1,
            @RequestParam Integer v2,
            HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(v1 == null || v1 <= 0 || v2 == null || v2 <= 0, ErrorCode.PARAMS_ERROR, "版本号无效");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用版本");
        }
        AppVersionDiffVO diff = appVersionService.compareVersions(appId, v1, v2);
        return ResultUtils.success(diff);
    }
}
