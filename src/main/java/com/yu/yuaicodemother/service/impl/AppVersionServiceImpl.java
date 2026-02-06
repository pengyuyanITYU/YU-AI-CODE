package com.yu.yuaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yu.yuaicodemother.constant.AppConstant;
import com.yu.yuaicodemother.exception.BusinessException;
import com.yu.yuaicodemother.exception.ErrorCode;
import com.yu.yuaicodemother.exception.ThrowUtils;
import com.yu.yuaicodemother.mapper.AppVersionMapper;
import com.yu.yuaicodemother.model.entity.App;
import com.yu.yuaicodemother.model.entity.AppVersion;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.vo.app.AppVersionDiffVO;
import com.yu.yuaicodemother.model.vo.app.AppVersionVO;
import com.yu.yuaicodemother.service.AppService;
import com.yu.yuaicodemother.service.AppVersionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion> implements AppVersionService {

    @Resource
    @Lazy
    private AppService appService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppVersion createVersion(Long appId, String changeLog) {
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        Integer currentVersion = app.getCurrentVersion();
        if (currentVersion == null || currentVersion <= 0) {
            currentVersion = 0;
        }
        int newVersion = currentVersion + 1;

        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            log.warn("应用代码目录不存在，跳过版本创建: {}", sourceDirPath);
            return null;
        }

        String versionDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "versions" + File.separator + appId
                + File.separator + "v" + newVersion;
        try {
            FileUtil.copyContent(sourceDir, new File(versionDirPath), true);
        } catch (Exception e) {
            log.error("版本文件复制失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "版本创建失败");
        }

        AppVersion appVersion = AppVersion.builder()
                .appId(appId)
                .version(newVersion)
                .sourceCodePath(versionDirPath)
                .changeLog(changeLog)
                .createTime(LocalDateTime.now())
                .build();
        boolean saved = this.save(appVersion);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "版本保存失败");

        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setCurrentVersion(newVersion);
        appService.updateById(updateApp);

        log.info("应用版本创建成功: appId={}, version={}", appId, newVersion);
        return appVersion;
    }

    @Override
    public List<AppVersionVO> listVersions(Long appId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("version", false);
        List<AppVersion> versions = this.list(queryWrapper);
        return versions.stream().map(v -> {
            AppVersionVO vo = new AppVersionVO();
            BeanUtil.copyProperties(v, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackToVersion(Long appId, Integer version, User loginUser) {
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该应用");
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .eq("version", version);
        AppVersion targetVersion = this.getOne(queryWrapper);
        ThrowUtils.throwIf(targetVersion == null, ErrorCode.NOT_FOUND_ERROR, "目标版本不存在");

        String versionDirPath = targetVersion.getSourceCodePath();
        File versionDir = new File(versionDirPath);
        if (!versionDir.exists() || !versionDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "版本文件不存在");
        }

        String codeGenType = app.getCodeGenType();
        String targetDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + codeGenType + "_" + appId;

        try {
            FileUtil.del(targetDirPath);
            FileUtil.copyContent(versionDir, new File(targetDirPath), true);
        } catch (Exception e) {
            log.error("版本回退失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "版本回退失败");
        }

        log.info("应用回退成功: appId={}, 回退到版本={}", appId, version);
        return true;
    }

    @Override
    public AppVersionDiffVO compareVersions(Long appId, Integer v1, Integer v2) {
        QueryWrapper q1 = QueryWrapper.create().eq("appId", appId).eq("version", v1);
        QueryWrapper q2 = QueryWrapper.create().eq("appId", appId).eq("version", v2);
        AppVersion version1 = this.getOne(q1);
        AppVersion version2 = this.getOne(q2);

        ThrowUtils.throwIf(version1 == null || version2 == null, ErrorCode.NOT_FOUND_ERROR, "版本不存在");

        // 安全校验: 确保版本确实属于该应用，防止越权读取
        ThrowUtils.throwIf(!appId.equals(version1.getAppId()) || !appId.equals(version2.getAppId()),
                ErrorCode.NO_AUTH_ERROR, "该版本不属于该应用");

        File dir1 = new File(version1.getSourceCodePath());
        File dir2 = new File(version2.getSourceCodePath());

        if (!dir1.exists() || !dir2.exists()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "版本文件不存在");
        }

        List<AppVersionDiffVO.FileDiff> diffs = new ArrayList<>();
        List<File> files1 = FileUtil.loopFiles(dir1, file -> file.isFile() && isTextFile(file.getName()));

        for (File file1 : files1) {
            String relativePath = file1.getAbsolutePath().substring(dir1.getAbsolutePath().length() + 1);
            File file2 = new File(dir2, relativePath);

            String content1 = FileUtil.readString(file1, StandardCharsets.UTF_8);
            String content2 = file2.exists() ? FileUtil.readString(file2, StandardCharsets.UTF_8) : "";

            List<String> lines1 = Arrays.asList(content1.split("\n"));
            List<String> lines2 = Arrays.asList(content2.split("\n"));

            Patch<String> patch = DiffUtils.diff(lines1, lines2);
            List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                    "v" + v1 + "/" + relativePath,
                    "v" + v2 + "/" + relativePath,
                    lines1, patch, 3);

            if (!unifiedDiff.isEmpty()) {
                diffs.add(AppVersionDiffVO.FileDiff.builder()
                        .fileName(relativePath)
                        .oldContent(content1)
                        .newContent(content2)
                        .diffLines(unifiedDiff)
                        .build());
            }
        }

        return AppVersionDiffVO.builder()
                .oldVersion(v1)
                .newVersion(v2)
                .diffs(diffs)
                .build();
    }

    private boolean isTextFile(String fileName) {
        String[] textExtensions = { ".html", ".css", ".js", ".vue", ".ts", ".tsx", ".jsx", ".json", ".md", ".txt",
                ".xml", ".yaml", ".yml" };
        for (String ext : textExtensions) {
            if (fileName.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
