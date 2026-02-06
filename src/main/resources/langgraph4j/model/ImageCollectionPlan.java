package langgraph4j.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ImageCollectionPlan implements Serializable {
    
    /**
     * 内容图片搜索任务列表
     */
    private List<ImageSearchTask> contentImageTasks;
    
    /**
     * 插画图片搜索任务列表
     */
    private List<IllustrationTask> illustrationTasks;
    
    /**
     * 架构图生成任务列表
     */
    private List<DiagramTask> diagramTasks;
    
    /**
     * Logo生成任务列表
     */
    private List<LogoTask> logoTasks;
    
    /**
     * 内容图片搜索任务
     * 对应 ImageSearchTool.searchContentImages(String query)
     */

    /*
   语法层面：什么是 record？
    public record ImageSearchTask(String query) 这一行代码，相当于帮你写了一个传统的 Java 类，并且自动帮你完成了以下所有繁琐的工作：
    自动定义字段：定义了一个 private final String query; 字段。
    自动生成构造方法：生成了一个全参构造器 public ImageSearchTask(String query) { ... }。
    自动生成 Getter 方法：生成了获取值的方法，名字叫 query()（注意：不带 get 前缀）。
    自动生成通用方法：自动重写了 toString(), hashCode(), equals() 方法。
     */
    public record ImageSearchTask(String query) implements Serializable {}
    
    /**
     * 插画图片搜索任务
     * 对应 UndrawIllustrationTool.searchIllustrations(String query)
     */
    public record IllustrationTask(String query) implements Serializable {}
    
    /**
     * 架构图生成任务
     * 对应 MermaidDiagramTool.generateMermaidDiagram(String mermaidCode, String description)
     */
    public record DiagramTask(String mermaidCode, String description) implements Serializable {}
    
    /**
     * Logo生成任务
     * 对应 LogoGeneratorTool.generateLogos(String description)
     */
    public record LogoTask(String description) implements Serializable {}
}
