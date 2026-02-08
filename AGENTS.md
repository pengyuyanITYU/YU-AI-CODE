# yu-ai-code-mother 代理说明

本仓库是一个 AI 驱动的全栈代码生成平台。
**后端**：Spring Boot 3.5（Java 21） | **前端**：Vue 3 + TypeScript（Vite）。

## 🛠 构建、检查与测试命令

### 后端（根目录）
优先使用 Maven Wrapper `mvnw`，也可使用已安装的 `mvn`。
- **编译**：`mvn compile`
- **构建（跳过测试）**：`mvn clean install -DskipTests`
- **运行应用**：`mvn spring-boot:run`
- **运行全部测试**：`mvn test`
- **运行单个测试类**：`mvn test -Dtest=ClassName`
  - 示例：`mvn test -Dtest=AppServiceImplTest`
- **运行单个测试方法**：`mvn test -Dtest=ClassName#methodName`
  - 示例：`mvn test -Dtest=AppServiceImplTest#testCreateApp`
- **清理**：`mvn clean`

### 前端（`yu-ai-code-mother-frontend/`）
以下命令请在前端目录内执行。
- **安装依赖**：`npm install`
- **启动开发服务器**：`npm run dev`
- **生产构建**：`npm run build`（包含类型检查）
- **运行单元测试**：`npm run test:unit`
- **类型检查**：`npm run type-check`（提交前执行）
- **代码检查并修复**：`npm run lint`
- **代码格式化**：`npm run format`
- **生成 API 类型**：`npm run openapi2ts`（与后端 Swagger 同步）

---

## 📐 代码风格与规范

### 后端（Java 21 + Spring Boot 3）

#### 1. 命名与格式
- **类名**：`PascalCase`（例如 `AiCodeGeneratorFacade`）。
- **方法/变量**：`camelCase`（例如 `generateCode`）。
- **常量**：`UPPER_SNAKE_CASE`（例如 `MAX_RETRY_COUNT`）。
- **数据库**：表名/字段名使用 `snake_case`。
- **格式化**：遵循 Java 标准规范（4 空格缩进）。

#### 2. Lombok 与 依赖注入
- **注解**：
  - 实体/DTO 使用 `@Data`。
  - Service/Controller 层日志使用 `@Slf4j`。
  - 复杂对象构建使用 `@Builder`。
- **注入方式**：优先使用 `@Resource`（Jakarta），而不是 `@Autowired`。
  - 示例：`@Resource private UserService userService;`

#### 3. 导入顺序
按下列分组组织 import，提升可读性：
1. **第三方库**（`cn.hutool.*`、`org.apache.*`）
2. **框架相关**（`org.springframework.*`、`com.mybatisflex.*`）
3. **项目代码**（`com.yu.yuaicodemother.*`）
4. **Java 标准库**（`java.*`、`jakarta.*`）
*避免使用通配符导入（例如 `import java.util.*;`）。*

#### 4. 错误处理
- 使用 `BusinessException` + `ErrorCode` 枚举。
- 校验建议：`ThrowUtils.throwIf(condition, ErrorCode, "Msg");`
- **流式场景**：在 `Flux`/`Mono` 中要优雅处理异常（如 `.onErrorResume`），避免中断 SSE 流。

#### 5. 架构模式与 AI 逻辑
- **数据库访问**：MyBatis-Flex + `QueryWrapper`。
  - 优先使用 `QueryChain` 或 `UpdateChain` 简化操作。
  - 示例：`QueryChain.of(mapper).where(...).list();`
- **AI 逻辑**：LangChain4j + `AiServices`，流式处理使用 Project Reactor（`Flux`）。
- **异步处理**：复杂任务使用 Redisson 分布式锁或消息队列，AI 任务通常在 Facade 层编排。
- **文件处理**：COS (Tencent Cloud) 用于存储，Selenium 用于网页截图。

---

### 前端（Vue 3 + TypeScript）

#### 1. 框架与核心
- **开发风格**：使用 Composition API + `<script setup lang="ts">`。
- **UI 组件库**：Ant Design Vue（v4），图标使用 `@ant-design/icons-vue`。
- **状态管理**：Pinia（`defineStore`）。
- **HTTP 请求**：Axios（通过 `src/request.ts`）。
- **编辑器**：使用 Monaco Editor 处理代码展示与编辑。

#### 2. 组件结构
- 显式定义 `interface Props` 与 `interface Emits`。
- Props 使用 `withDefaults`（TypeScript 特性）。
- **样式**：仅使用 `<style scoped>`，优先使用 Ant Design 变量或 CSS 变量。
- **API 调用**：统一使用 `src/api/` 下生成的函数，禁止手动拼写 URL。

#### 3. 命名与文件
- **组件**：`PascalCase`（例如 `AppCard.vue`）。
- **组合式函数**：`useCamelCase`（例如 `useAppStatus.ts`）。
- **文件命名**：通常使用 `camelCase.ts`，组件文件使用 `PascalCase.vue`。

---

## 🤖 Agent 工作规则

1. **经验检索**：每次回答用户问题或执行任务前，必须先查看 `bugExperience/README.md` 文件，分析历史报错与修复方案，严禁犯同样的错误。
2. **绝对路径**：所有文件操作必须使用绝对路径。
   - 根路径：`D:\develop\IT\Project\YU-AI\yu-ai-code-mother`
3. **主动修复**：发现 Bug（如 NPE 风险、编译错误、TS 类型缺失）时应主动修复。
4. **自验证**：
   - **后端**：修改后运行相关 JUnit 测试（`mvn test -Dtest=...`）。
   - **前端**：运行 `npm run type-check` 检查 TS 错误。
5. **安全与隐私**：严禁提交 `.env`、密钥或任何凭据。相关输入应接入 `PromptSafetyInputGuardrail` 校验。
6. **文档约束**：不要主动改 `src/main/java/com/yu/yuaicodemother/README.md`；Bug 总结应精炼地写入 `bugExperience/README.md`。
7. **测试用例**：`测试/编译命令类文件`：统一存放至 `AITEST` 文件夹。使用完成后立即清理该文件夹内容。
8. **Git 规范**：
   - 提交前必须执行后端 `mvn compile` 和前端 `npm run type-check`。
   - 严禁未经授权执行 `git reset --hard` 或 `git push --force`。
9. **日志记录**：重要业务流程必须使用 `@Slf4j` 打印关键日志（如 AI 调用参数、执行耗时）。

---

## 📦 关键目录结构

```text
backend/src/main/java/com/yu/yuaicodemother/
├── ai/          # LangChain4j 工具、护栏与服务
├── controller/  # REST API 接口层
├── core/        # 核心业务：Facade、Builder、Saver、Parser
├── model/       # 实体、DTO、VO、枚举
└── service/     # 业务逻辑（实现类在 /impl）

frontend/src/
├── api/         # 生成的 API 客户端（由 openapi2ts 产生）
├── components/  # 公共 UI 组件
├── pages/       # 路由页面
└── stores/      # Pinia 状态管理
```
