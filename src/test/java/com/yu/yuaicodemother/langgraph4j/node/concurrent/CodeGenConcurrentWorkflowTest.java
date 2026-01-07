package com.yu.yuaicodemother.langgraph4j.node.concurrent;

import com.yu.yuaicodemother.langgraph4j.state.WorkflowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

@SpringBootTest
class CodeGenConcurrentWorkflowTest {

    @Test
    void testConcurrentWorkflow() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("执行工作流");
        WorkflowContext result = new CodeGenConcurrentWorkflow().executeWorkflow("创建一个技术博客网站，需要展示编程教程和系统架构");
        stopWatch.stop();
        Assertions.assertNotNull(result);
        System.out.println(stopWatch.prettyPrint());
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("构建结果目录: " + result.getBuildResultDir());
        System.out.println("收集的图片数量: " + (result.getImageList() != null ? result.getImageList().size() : 0));
    }

    @Test
    void testEcommerceWorkflow() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("执行工作流");
        WorkflowContext result = new CodeGenConcurrentWorkflow().executeWorkflow("创建一个电子商务网站，需要商品展示、购物车和支付功能");
        stopWatch.stop();
        Assertions.assertNotNull(result);
        System.out.println(stopWatch.prettyPrint());
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("收集的图片数量: " + (result.getImageList() != null ? result.getImageList().size() : 0));
    }
}
