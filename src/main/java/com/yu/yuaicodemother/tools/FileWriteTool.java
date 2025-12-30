package com.yu.yuaicodemother.tools;

import com.yu.yuaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
public class FileWriteTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                // 相对路径处理，创建基于 appId 的项目目录
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
//                含义：把“总仓库目录”和“当前项目名”拼起来，找到这个项目的根目录。

                path = projectRoot.resolve(relativeFilePath);
            }
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path.toAbsolutePath());
            /*
            * . StandardOpenOption.CREATE (关键选项 1)
                含义：不存在则创建。
              逻辑：如果 path 指向的文件不存在，程序会自动帮你创建一个新文件
              * 。如果文件已经存在，这个选项就不起作用。
                5. StandardOpenOption.TRUNCATE_EXISTING (关键选项 2)
                含义：存在则清空（截断）。
              逻辑：如果文件已经存在，在写入之前，先把文件的长度截断为 0（也就是把里面的旧内容全部删光）。
            为什么要加这个？
            如果不加这个，且新内容比旧内容短，旧内容的尾巴可能会残留下来。
            或者如果不加这个改用 APPEND，新内容会追加在旧内容后面，文件会越来越大。
            加上这个，保证了文件内容完全等于你当前写入的 content
            * */
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}
