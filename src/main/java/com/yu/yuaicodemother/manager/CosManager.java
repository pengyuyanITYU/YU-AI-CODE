package com.yu.yuaicodemother.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.yu.yuaicodemother.config.CosClientConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * COS对象存储管理器
 *
 * @author yupi
 */
@Component
@Slf4j
@ConditionalOnBean(COSClient.class)
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * 根据 URL 下载文件并上传到 COS
     *
     * @param url 源文件的网络URL
     * @param key COS对象键（存储路径，如 images/2024/1.png）
     * @return 文件的访问URL，失败抛出异常
     */
    public String uploadFileByUrl(String url, String key) {
        try {
            // 1. 建立连接
            URL sourceUrl = new URL(url);
            URLConnection connection = sourceUrl.openConnection();
            // 设置超时时间，防止网络卡顿导致线程阻塞
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            // 2. 读取流
            try (InputStream inputStream = connection.getInputStream()) {
                // 为了获取准确的文件大小（COS上传流必须指定大小），先读入内存
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > -1) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();

                // 转为输入流
                byte[] fileBytes = baos.toByteArray();
                ByteArrayInputStream uploadStream = new ByteArrayInputStream(fileBytes);

                // 3. 设置元数据
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(fileBytes.length);
                // 自动识别 Content-Type (如 image/png)，这对浏览器预览很重要
                String contentType = connection.getContentType();
                if (contentType != null) {
                    metadata.setContentType(contentType);
                }

                // 4. 执行上传
                PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, uploadStream, metadata);
                cosClient.putObject(putObjectRequest);

                String resultUrl = cosClientConfig.getHost().replaceAll("/$", "") + "/" + key.replaceAll("^/", "");
                log.info("网络文件上传COS成功: Source={} -> Target={}", url, resultUrl);
                return resultUrl;
            }
        } catch (Exception e) {
            log.error("网络文件上传COS失败, url: {}, key: {}", url, key, e);
            throw new RuntimeException("上传网络文件失败: " + e.getMessage());
        }
    }

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件到 COS 并返回访问 URL
     *
     * @param key  COS对象键（完整路径）
     * @param file 要上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        // 上传文件
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            // 构建访问URL
            String url = cosClientConfig.getHost().replaceAll("/$", "") + "/" + key.replaceAll("^/", "");
            log.info("文件上传COS成功: {} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("文件上传COS失败，返回结果为空");
            return null;
        }
    }
}
