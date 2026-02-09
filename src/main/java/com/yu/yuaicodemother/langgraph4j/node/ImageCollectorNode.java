package com.yu.yuaicodemother.langgraph4j.node;

import com.yu.yuaicodemother.langgraph4j.ai.ImageCollectionPlanService;
import com.yu.yuaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.yu.yuaicodemother.langgraph4j.model.ImageResource;
import com.yu.yuaicodemother.langgraph4j.state.WorkflowContext;
import com.yu.yuaicodemother.langgraph4j.tools.ImageSearchTool;
import com.yu.yuaicodemother.langgraph4j.tools.LogoGeneratorTool;
import com.yu.yuaicodemother.langgraph4j.tools.MermaidDiagramTool;
import com.yu.yuaicodemother.langgraph4j.tools.UndrawIllustrationTool;
import com.yu.yuaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片收集节点
 * 使用AI进行工具调用，收集不同类型的图片
 * 工作节点内部实现并发
 */
@Slf4j
public class ImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            List<ImageResource> collectedImages = new ArrayList<>();
/*          StopWatch  stopWatch = new StopWatch("收集图片");
            通常用于开发调试、
            排查性能瓶颈或非核心链路的日志记录
            生产环境慎用 prettyPrint()
*/
            try {
                // 第一步：获取图片收集计划
//                stopWatch.start("获取图片收集计划");
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
//                stopWatch.stop();
                log.info("获取到图片收集计划，开始并发执行");

                // 第二步：并发执行各种图片收集任务
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
//                 stopWatch.start("并发执行图片搜索任务");
                // 并发执行内容图片搜索
                if (plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                imageSearchTool.searchContentImages(task.query())));
                    }
                }
//                 stopWatch.stop();
                // 并发执行插画图片搜索
//                 stopWatch.start("并发执行插画图片搜索任务");
                if (plan.getIllustrationTasks() != null) {
                    UndrawIllustrationTool illustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                illustrationTool.searchIllustrations(task.query())));
                    }
                }
//                 stopWatch.stop();
                // 并发执行架构图生成
//                 stopWatch.start("并发执行架构图生成任务");
                if (plan.getDiagramTasks() != null) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                diagramTool.generateMermaidDiagram(task.mermaidCode(), task.description())));
                    }
                   
                }
//                 stopWatch.stop();
                // 并发执行Logo生成
//                 stopWatch.start("并发执行Logo生成任务");
                if (plan.getLogoTasks() != null) {
                    LogoGeneratorTool logoTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                logoTool.generateLogos(task.description())));
                    }
                }
//                  stopWatch.stop();
//                System.out.println( stopWatch.prettyPrint());

                // 等待所有任务完成并收集结果
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));
                allTasks.join();
                // 收集所有结果
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    List<ImageResource> images = future.get();
                    if (images != null) {
                        collectedImages.addAll(images);
                    }
                }
                log.info("并发图片收集完成，共收集到 {} 张图片", collectedImages.size());
            } catch (Exception e) {
                log.error("图片收集失败: {}", e.getMessage(), e);
            }
            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageList(collectedImages);
            return WorkflowContext.saveContext(context);
        });
    }
}
