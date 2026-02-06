package com.yu.yuaicodemother.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yu.yuaicodemother.common.BaseResponse;
import com.yu.yuaicodemother.common.ResultUtils;
import com.yu.yuaicodemother.exception.ErrorCode;
import com.yu.yuaicodemother.exception.ThrowUtils;
import com.yu.yuaicodemother.manager.CosManager;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import com.yu.yuaicodemother.model.vo.file.FileUploadResponse;
import com.yu.yuaicodemother.service.FileService;
import com.yu.yuaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private FileService fileService;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "doc", "docx", "md", "txt",
            "html", "css", "vue",
            "jpg", "jpeg", "png",
            "pdf", "ppt", "pptx"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @PostMapping("/upload")
    public BaseResponse<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizType", defaultValue = "userPrompts") String bizType,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");

        String originalFilename = file.getOriginalFilename();
        ThrowUtils.throwIf(StrUtil.isBlank(originalFilename), ErrorCode.PARAMS_ERROR, "文件名不能为空");

        String extension = FileUtil.extName(originalFilename).toLowerCase();
        ThrowUtils.throwIf(!ALLOWED_EXTENSIONS.contains(extension),
                ErrorCode.PARAMS_ERROR,
                "不支持的文件类型,仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));

        long fileSize = file.getSize();
        ThrowUtils.throwIf(fileSize > MAX_FILE_SIZE,
                ErrorCode.PARAMS_ERROR,
                "文件大小不能超过 10MB");

        try {
            String cosPath = generateCosPath(loginUser.getId(), originalFilename, bizType);

            File tempFile = File.createTempFile("upload_", "_" + originalFilename);
            file.transferTo(tempFile);

            String fileUrl = cosManager.uploadFile(cosPath, tempFile);
            ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.SYSTEM_ERROR, "文件上传失败");

            FileUtil.del(tempFile);

            FileUploadResponse response = FileUploadResponse.builder()
                    .url(fileUrl)
                    .fileName(originalFilename)
                    .fileSize(fileSize)
                    .build();

            log.info("用户 {} 上传文件成功: {} -> {}", loginUser.getId(), originalFilename, fileUrl);
            return ResultUtils.success(response);

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/process")
    public BaseResponse<FileProcessResult> processFile(
            @RequestParam("url") String fileUrl,
            @RequestParam("fileName") String fileName,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件URL不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(fileName), ErrorCode.PARAMS_ERROR, "文件名不能为空");

        FileProcessResult result = fileService.processFile(fileUrl, fileName);

        return ResultUtils.success(result);
    }

    private String generateCosPath(Long userId, String fileName, String bizType) {
        LocalDateTime now = LocalDateTime.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));
        String day = now.format(DateTimeFormatter.ofPattern("dd"));

        return String.format("%s/%d/%s/%s/%s/%s",
                bizType, userId, year, month, day, fileName);
    }
}
