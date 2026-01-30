const fs = require('fs');
const { Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, LevelFormat, Table, TableRow, TableCell, BorderStyle, WidthType, ShadingType } = require('docx');

const doc = new Document({
    styles: {
        default: {
            document: {
                run: {
                    font: "Arial",
                    size: 24, // 12pt
                },
            },
        },
        paragraphStyles: [
            {
                id: "Heading1",
                name: "Heading 1",
                run: {
                    size: 32,
                    bold: true,
                    color: "000000",
                },
                paragraph: {
                    spacing: { before: 240, after: 120 },
                },
            },
            {
                id: "Heading2",
                name: "Heading 2",
                run: {
                    size: 28,
                    bold: true,
                    color: "333333",
                },
                paragraph: {
                    spacing: { before: 240, after: 120 },
                },
            },
        ],
    },
    sections: [{
        properties: {},
        children: [
            // Title
            new Paragraph({
                text: "应用版本化管理与对比功能实现总结",
                heading: HeadingLevel.TITLE,
                alignment: AlignmentType.CENTER,
            }),
            new Paragraph({
                text: "",
                spacing: { after: 200 },
            }),

            // 1. 概述
            new Paragraph({
                text: "1. 概述",
                heading: HeadingLevel.HEADING_1,
            }),
            new Paragraph({
                children: [
                    new TextRun("本项目成功实现了应用代码的版本化管理及类似 Git 的差异对比功能。通过引入 Monaco Editor，为用户提供了专业级的代码对比体验，支持语法高亮、差异着色和即时渲染。"),
                ],
            }),

            // 2. 后端实现
            new Paragraph({
                text: "2. 后端实现 (Backend)",
                heading: HeadingLevel.HEADING_1,
            }),
            new Paragraph({
                text: "核心逻辑位于 AppVersionServiceImpl 中，主要改进如下：",
                spacing: { after: 120 },
            }),
            new Paragraph({
                children: [
                    new TextRun({
                        text: "• 安全性增强：",
                        bold: true,
                    }),
                    new TextRun(" 在 compareVersions 接口中增加了显式的权限校验，确保参与对比的两个 version ID 均属于当前请求的 App ID，有效防止了越权访问风险。"),
                ],
                bullet: { level: 0 }
            }),
            new Paragraph({
                children: [
                    new TextRun({
                        text: "• 数据处理：",
                        bold: true,
                    }),
                    new TextRun(" 负责读取指定版本的源码文件内容，不做复杂的 Diff 计算，将原始内容直接返回给前端，减轻服务器压力并利用前端算力。"),
                ],
                bullet: { level: 0 }
            }),

            // 3. 前端实现
            new Paragraph({
                text: "3. 前端实现 (Frontend)",
                heading: HeadingLevel.HEADING_1,
            }),
            new Paragraph({
                children: [
                    new TextRun("前端采用了 Monaco Editor (VS Code 的核心编辑器) 的 Diff 模式，替代了传统的简单的文本对比。"),
                ],
                spacing: { after: 120 },
            }),
            new Paragraph({
                text: "关键组件与技术：",
                heading: HeadingLevel.HEADING_2,
            }),
            new Paragraph({
                children: [
                    new TextRun({ text: "AppVersionDiff.vue", bold: true }),
                    new TextRun(": 重构了该组件，集成 monaco.editor.createDiffEditor。"),
                ],
                bullet: { level: 0 }
            }),
            new Paragraph({
                children: [
                    new TextRun({ text: "Vite 集成", bold: true }),
                    new TextRun(": 配置了 vite-plugin-monaco-editor 以自动处理 Monaco 的 Worker 文件加载。"),
                ],
                bullet: { level: 0 }
            }),

            // 4. 功能亮点
            new Paragraph({
                text: "4. 功能亮点",
                heading: HeadingLevel.HEADING_1,
            }),
            new Paragraph({
                children: [
                    new TextRun({ text: "Side-by-Side 视图", bold: true }),
                    new TextRun(": 左右分栏显示旧版本与新版本，差异一目了然。"),
                ],
                bullet: { level: 0 }
            }),
            new Paragraph({
                children: [
                    new TextRun({ text: "语法高亮", bold: true }),
                    new TextRun(": 支持 Java, Vue, TS 等多种语言的语法高亮，提升阅读体验。"),
                ],
                bullet: { level: 0 }
            }),
            new Paragraph({
                children: [
                    new TextRun({ text: "智能折叠", bold: true }),
                    new TextRun(": 自动折叠无差异的代码块，聚焦由于的核心变更。"),
                ],
                bullet: { level: 0 }
            }),
        ],
    }],
});

Packer.toBuffer(doc).then((buffer) => {
    fs.writeFileSync("Version_Control_Summary.docx", buffer);
    console.log("Document created successfully");
});
