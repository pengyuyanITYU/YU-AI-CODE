报错原因是测试文件 `MermaidDiagramToolTest.java` 中的 Mermaid 代码包含语法错误。
具体是在：
`Decision -->|是| Output[输出结果]npm install -g @mermaid-js/mermaid-cli`
这一行末尾多出了 `npm install ...` 命令文本（应该是复制粘贴时的失误），导致 Mermaid CLI 工具解析失败，无法生成图片，进而抛出异常。

**计划：**

1. 修改 `src/test/java/com/yu/yuaicodemother/langgraph4j/tools/MermaidDiagramToolTest.java`，删除 `mermaidCode` 字符串中错误的 `npm install -g @mermaid-js/mermaid-cli` 文本。
2. 运行测试 `testGenerateMermaidDiagram` 验证修复结果。

