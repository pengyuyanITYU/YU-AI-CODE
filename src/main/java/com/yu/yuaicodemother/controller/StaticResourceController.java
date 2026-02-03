package com.yu.yuaicodemother.controller;


import cn.hutool.core.util.StrUtil;
import com.yu.yuaicodemother.constant.AppConstant;
import com.yu.yuaicodemother.model.entity.App;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.enums.AppDeployStatusEnum;
import com.yu.yuaicodemother.service.AppService;
import com.yu.yuaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;

@RestController
@RequestMapping("/static")
public class StaticResourceController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<org.springframework.core.io.Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());
            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }

            // 统一获取应用信息并校验状态
            App app = null;
            boolean isPreview = false;

            // 1. 尝试作为预览 key ({type}_{appId}) 解析
            if (deployKey.contains("_")) {
                try {
                    String appIdStr = deployKey.substring(deployKey.lastIndexOf("_") + 1);
                    if (StrUtil.isNumeric(appIdStr)) {
                        app = appService.getById(Long.parseLong(appIdStr));
                        isPreview = true;
                    }
                } catch (Exception ignored) {
                }
            }

            // 2. 如果没查到或不是预览格式，尝试作为正式 deployKey 查询
            if (app == null) {
                app = appService.getOne(com.mybatisflex.core.query.QueryWrapper.create().eq("deployKey", deployKey));
            }

            // 3. 核心校验逻辑
            if (app != null) {
                Integer deployStatus = app.getDeployStatus();
                boolean isOnline = Integer.valueOf(AppDeployStatusEnum.ONLINE.getValue()).equals(deployStatus);
                
                // 如果不是“已上线”状态
                if (!isOnline) {
                    // 如果是预览请求，检查是否为本人
                    if (isPreview) {
                        User loginUser = null;
                        try {
                            loginUser = userService.getLoginUser(request);
                        } catch (Exception ignored) {
                        }
                        // 如果未登录或不是本人，禁止访问
                        if (loginUser == null || !app.getUserId().equals(loginUser.getId())) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                        }
                    } else {
                        // 正式部署链接非 ONLINE 一律禁止
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                }
            }

            // 路径查找逻辑
            // A. 尝试从部署目录获取
            String deployFilePath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey + resourcePath;
            File deployFile = new File(deployFilePath);
            if (deployFile.exists()) {
                return getFileResource(deployFilePath, deployFile);
            }

            // B. 尝试从生成目录获取
            String previewFilePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + deployKey + resourcePath;
            File previewFile = new File(previewFilePath);
            if (previewFile.exists()) {
                return getFileResource(previewFilePath, previewFile);
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<org.springframework.core.io.Resource> getFileResource(String filePath, File file) {
        org.springframework.core.io.Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header("Content-Type", getContentTypeWithCharset(filePath))
                .body(resource);
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
