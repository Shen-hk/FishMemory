# FishMemory 文档索引

本目录存放**可维护、可面试口述**的架构与实现说明。App 代码以 `app/src/main/java/com/fishmemory/app` 为准，富文本 SDK 代码以 `richtext-editor/src/main/java` 为准，文档与源码冲突时以源码为准。

## 阅读顺序建议

| 顺序 | 文档 | 说明 |
|------|------|------|
| 1 | [ARCHITECTURE.md](../ARCHITECTURE.md) | 项目分层、包结构、模块入口（仓库根目录） |
| 2 | [richtext-editor-deep-dive.md](richtext-editor-deep-dive.md) | 富文本块编辑器：从模型到 ViewHolder 的精读手册 |
| 3 | [publish-richtext-interview-deep-dive.md](publish-richtext-interview-deep-dive.md) | 发布链路 + 块状编辑器：面试版结论与数据流 |
| 4 | [ai-polish-implementation.md](ai-polish-implementation.md) | 正文块 AI 润色（DeepSeek 流式、ViewModel、RecyclerView 约束） |
| 5 | [richtext-sdk-quickstart.md](richtext-sdk-quickstart.md) | 富文本 SDK 当前接入方式：编辑态、只读态、扩展接口 |
| 6 | [richtext-sdk-roadmap.md](richtext-sdk-roadmap.md) | 富文本编辑器 SDK 沉淀规划、现状与阶段路线 |

## 文档一览

| 文件 | 内容摘要 |
|------|-----------|
| **richtext-editor-deep-dive.md** | 块级模型、输入协议、BlockActionManager、焦点/选区、Adapter 刷新、故障排查、复刻路线 |
| **publish-richtext-interview-deep-dive.md** | PublishActivity、DraftCoordinator、PublishArticleUseCase、草稿与发布数据流、编辑器性能策略 |
| **ai-polish-implementation.md** | `BlockAiAssistViewModel`、`DeepSeekChatStreamClient`、`AiPromptBuilder`、`PAYLOAD_AI_ASSIST`、BuildConfig 密钥 |
| **richtext-sdk-quickstart.md** | `BlockEditorRecyclerView`、`EditorCallback`、JSON 导入导出、只读渲染、媒体/AI 扩展接入 |
| **richtext-sdk-roadmap.md** | SDK 化背景、当前已具备能力、模块拆分方案、Facade API 进展、阶段验收标准 |
| **123654.md** | 占位说明：历史长文已收敛到上文三份主文档，请勿再向此文件追加大段内容 |

## 模块内短文

| 路径 | 说明 |
|------|------|
| [richtext-editor/.../richtext/README.md](../richtext-editor/src/main/java/com/fishmemory/app/ui/publish/richtext/README.md) | `richtext` SDK 模块分层与依赖约定 |
| [app/.../publish/PUBLISH_MODULE.md](../app/src/main/java/com/fishmemory/app/ui/publish/PUBLISH_MODULE.md) | 发表模块历史说明与项目介绍文案（含重复段落，以本索引为准） |

## 维护约定

- 新增能力：先更新**对应实现章节**与**本索引**，避免只写零散 README。
- 密钥与本地配置：不写真实 Key；见 `ai-polish-implementation.md` 中 `local.properties` / `BuildConfig` 说明。
