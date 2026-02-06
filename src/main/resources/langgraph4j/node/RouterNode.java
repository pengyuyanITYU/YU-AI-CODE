package langgraph4j.node;

import com.yu.yuaicodemother.ai.AiCodeGenTypeRoutingService;
import com.yu.yuaicodemother.ai.model.CodeGenTypeRoutingResult;
import com.yu.yuaicodemother.langgraph4j.state.WorkflowContext;
import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yu.yuaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            CodeGenTypeRoutingResult generationType = new CodeGenTypeRoutingResult();
            try {
                // 获取AI路由服务
                AiCodeGenTypeRoutingService routingService = SpringContextUtil.getBean(AiCodeGenTypeRoutingService.class);
                // 根据原始提示词进行智能路由
                generationType = routingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getType().getValue(), generationType.getType().getText());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType.setType(CodeGenTypeEnum.HTML);
            }

            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType.getType());
            return WorkflowContext.saveContext(context);
        });
    }
}

