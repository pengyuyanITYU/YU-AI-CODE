package com.yu.yuaicodemother.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.yu.yuaicodemother.exception.BusinessException;
import com.yu.yuaicodemother.exception.ErrorCode;
import com.yu.yuaicodemother.langgraph4j.model.ImageResource;
import com.yu.yuaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import com.yu.yuaicodemother.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Component
public class MermaidDiagramTool {

    @Resource
    private CosManager cosManager;
    
    @Tool("将 Mermaid 代码转换为架构图图片，用于展示系统结构和技术关系")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid 图表代码") String mermaidCode,
                                                      @P("架构图描述") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            // 转换为SVG图片
            File diagramFile = convertMermaidToSvg(mermaidCode);
            // 上传到COS
            String keyName = String.format("/mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, diagramFile);
            // 清理临时文件
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(cosUrl)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(cosUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("生成架构图失败: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * 将Mermaid代码转换为SVG图片
     */
    private File convertMermaidToSvg(String mermaidCode) {
        // 创建临时输入文件
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
        // 创建临时输出文件
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);

        // 构建命令参数列表
        List<String> commands = new ArrayList<>();
        if (SystemUtil.getOsInfo().isWindows()) {
            commands.add("mmdc.cmd");
        } else {
            commands.add("mmdc");
        }
        commands.add("-i");
        commands.add(tempInputFile.getAbsolutePath());
        commands.add("-o");
        commands.add(tempOutputFile.getAbsolutePath());
        commands.add("-b");
        commands.add("transparent");

        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            
            // 尝试自动寻找本地 Chrome 并设置环境变量，解决 Puppeteer 找不到浏览器的问题
            if (SystemUtil.getOsInfo().isWindows()) {
                String[] chromePaths = {
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
                };
                for (String path : chromePaths) {
                    if (FileUtil.exist(path)) {
                        pb.environment().put("PUPPETEER_EXECUTABLE_PATH", path);
                        log.info("检测到本地 Chrome，已设置 PUPPETEER_EXECUTABLE_PATH: {}", path);
                        break;
                    }
                }
            }

            log.info("Executing Mermaid command: {}", String.join(" ", commands));
            Process process = pb.start();
            
            // 读取标准输出和错误输出
            String output = cn.hutool.core.io.IoUtil.read(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
            String error = cn.hutool.core.io.IoUtil.read(process.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8);
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.error("Mermaid CLI failed. Exit code: {}, Output: {}, Error: {}", exitCode, output, error);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行失败: " + error);
            }
        } catch (Exception e) {
            log.error("Mermaid CLI execution error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行异常: " + e.getMessage());
        }

        // 检查输出文件
        if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 未生成输出文件");
        }
        // 清理输入文件，保留输出文件供上传使用
        FileUtil.del(tempInputFile);
        return tempOutputFile;
    }
}
