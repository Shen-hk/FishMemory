# FishMemory 富文本编辑器 SDK 沉淀规划

本文档用于记录 FishMemory 富文本编辑器从“业务内模块”沉淀为“可复用 Android SDK”的目标、边界、现状与后续路线。当前已完成仓库内第一阶段模块化抽离，后续待 API 稳定后再考虑独立仓库或发布。

---

## 1. 背景与目标

FishMemory 最初是一个 Android 资讯阅读与内容发布应用，功能包含首页信息流、文章详情、发布页、草稿箱、AI 润色等。随着项目复盘，继续扩展传统 App 业务功能的简历价值有限，而发布页中的原生块式富文本编辑器具备更高的工程沉淀价值。

因此，后续重点从“继续开发完整资讯 App”调整为：

> 从 FishMemory 中抽离并沉淀一个 Android 原生块式富文本编辑器 SDK。

目标结果：

- 让富文本编辑器可以脱离 FishMemory 业务独立接入。
- 将编辑器能力收敛为清晰、稳定的对外 API。
- 保留 FishMemory 作为完整接入示例和回归验证场景。
- 形成可用于简历、面试和开源展示的技术资产。

---

## 2. SDK 定位

暂定名称：

```text
FishRichEditor
```

一句话定位：

> 一个基于 RecyclerView 的 Android 原生块式富文本编辑器 SDK，支持结构化文档模型、多类型内容块、JSON 持久化、只读渲染与可插拔 AI 写作辅助。

核心差异点：

- 原生 View 实现，不依赖 WebView 整页富文本方案。
- 块式模型组织内容，适合图片、视频、代码块、链接卡片等复杂内容混排。
- 编辑态模型与持久化模型分离，便于草稿保存、文章发布和只读渲染。
- AI 润色作为扩展能力接入，不污染核心编辑器模型。

---

## 3. 当前已经具备的能力

当前富文本编辑器主要位于：

```text
richtext-editor/src/main/java/com/fishmemory/app/ui/publish/richtext
```

已具备能力：

- 块式编辑容器：`BlockEditorRecyclerView`
- 多类型块模型：标题、正文、图片、视频、代码块、列表、链接卡片、分割线等
- 富文本 Span：加粗、下划线、删除线、链接等结构化描述
- 编辑操作：文本块拆分、合并、删除、焦点跳转、选区处理
- RecyclerView 多 ViewHolder 渲染
- 局部刷新机制：通过 payload 避免不必要重绑，降低 IME 被打断风险
- 代码块高亮：基于内部 formatter 做语法高亮
- 链接识别与链接卡片展示
- 图片与视频块的插入、替换、预览、上传进度展示
- 文档转换：`Document` / `EditorBlock` / `EditorBlockEntity` 之间转换
- 标准 JSON 导出与解析：`StandardJsonExporter`、`StandardJsonParser`
- AI 润色协作链路：发布页通过 `BlockAiAssistViewModel` 与编辑器协作，未替换前不污染块模型

已经完成的工程整理：

- 富文本实现已集中在独立 `richtext` 包下。
- 文档已沉淀富文本编辑器精读手册：`docs/richtext-editor-deep-dive.md`
- 发布页与富文本链路已沉淀面试版文档：`docs/publish-richtext-interview-deep-dive.md`
- AI 润色实现已独立说明：`docs/ai-polish-implementation.md`
- 架构文档中已明确“领域内核 + UI 外壳”的方向。
- 已新增 `:richtext-editor` Android Library 模块。
- `app` 已通过 `implementation(project(":richtext-editor"))` 接入编辑器 SDK。
- 富文本源码与相关布局/资源已从 `app` 迁移到 `richtext-editor`。
- `AiAssistUiState`、`AiPolishStyle` 作为阶段 1 过渡模型迁入 SDK 模块，后续会继续收敛为可插拔 AI 扩展接口。
- 已验证 `:richtext-editor:assembleDebug` 与 `:app:assembleDebug` 构建通过。
- 已新增 `api` 包，提供 `RichTextDocumentCodec` 与 `ReadOnlyRichTextRenderer` 两个对外入口。
- `BlockEditorRecyclerView` 已补充 `getDocument()`、`setDocument()`、`exportStandardJson()`，发布页不再自行拼装标题与正文块。
- 本地文章详情页已改为通过 `ReadOnlyRichTextRenderer` 渲染标准 JSON，不再直接依赖 `EditorAdapter`、`VideoBlockViewHolder`、`StandardJsonParser`、`StandardBlockToDisplay`。
- 已新增 `RichTextDocumentCodecTest`，覆盖标准 JSON 导出后解析为只读块的基础链路。
- 已新增 `EditorVideoUploader` 视频上传扩展接口，编辑器上传/重试链路已依赖接口而不是具体上传实现。
- `VideoUploadManager` 保留为当前 App 的默认实现，后续可迁出为 sample 或 extension 示例。
- 已新增 `AiAssistProvider` AI 润色扩展接口，`BlockAiAssistViewModel` 只依赖接口，不再直接创建 DeepSeek client 或读取 `BuildConfig`。
- `DeepSeekAiAssistProvider` 已作为 FishMemory App 侧接入实现，负责 Prompt 组装、Key 校验与流式网络请求。
- 已新增 `EditorCallback` 统一编辑器事件回调，收口内容变化、焦点变化、链接点击、图片、视频与 AI 入口事件。
- `BlockEditorRecyclerView` 已补充 `loadStandardJson()`，标准 JSON 既可只读渲染，也可恢复到编辑态。
- 已新增 `docs/richtext-sdk-quickstart.md`，记录当前 SDK 接入方式与阶段性交付边界。

---

## 4. SDK 边界

SDK 应该负责：

- 文档块模型定义
- 编辑器 View 容器
- 多类型块编辑与渲染
- 焦点、选区、拆分、合并、删除等编辑行为
- 富文本 Span 解析与应用
- JSON 导入导出
- 只读渲染
- 样式与行为配置
- 媒体、AI、链接解析等扩展点定义

SDK 不应该负责：

- FishMemory 的发布流程
- 草稿数据库与业务保存策略
- 用户登录、账号体系、收藏、点赞、消息等 App 业务
- DeepSeek API Key 管理
- 具体图片/视频上传服务
- 具体网络接口和后端协议
- 业务页面路由与权限申请编排

建议把业务能力改造成接口注入：

```kotlin
interface EditorMediaProvider {
    suspend fun uploadImage(localUri: Uri): String
    suspend fun uploadVideo(localUri: Uri): String
}
```

```kotlin
interface AiAssistProvider {
    fun polish(
        blockList: EditorBlockList,
        blockId: String,
        style: AiPolishStyle,
    ): Flow<String>
}
```

---

## 5. 推荐模块结构

第一阶段采用仓库内模块化，不单独新开项目：

```text
FishMemory
├── app
│   └── FishMemory 业务 App，同时作为完整接入示例
└── richtext-editor
    └── Android Library SDK
```

第一阶段稳定后，再考虑进一步拆分：

```text
FishMemory
├── app
├── richtext-core
│   └── Document、EditorBlock、SpanData、JSON 转换、纯 Kotlin 逻辑
├── richtext-editor
│   └── Android View、RecyclerView、ViewHolder、交互行为
├── richtext-ai-deepseek
│   └── DeepSeek AI 润色扩展示例
└── sample
    └── 最小 SDK 接入示例
```

当前不建议一开始拆太细，优先完成 `:richtext-editor` 抽离并保证原 App 可运行。

---

## 6. 对外 API 草案

SDK 对外暴露应尽量少而稳定。当前编辑态接入方式：

```kotlin
binding.editor.setDocument(document)
binding.editor.loadStandardJson(json)

val document = binding.editor.getDocument()
val json = binding.editor.exportStandardJson()
```

当前统一事件回调：

```kotlin
binding.editor.setEditorCallback(object : EditorCallback {
    override fun onContentChanged() = saveDraft()
    override fun onAiPolishRequested(blockId: String) = showAiDialog(blockId)
})
```

当前只读渲染接入方式：

```kotlin
val renderer = ReadOnlyRichTextRenderer(recyclerView)
renderer.onImagePreviewRequested = { url -> openImagePreview(url) }
renderer.renderStandardJson(json)
```

当前第一批公开 API：

- `BlockEditorRecyclerView`：编辑态 facade
- `EditorCallback`：统一编辑器事件回调
- `RichTextDocumentCodec`：标准 JSON 导入导出 facade
- `ReadOnlyRichTextRenderer`：只读渲染 facade
- `EditorVideoUploader`：视频上传扩展接口
- `AiAssistProvider`：AI 润色扩展接口
- `Document`
- `EditorBlockEntity`
- `EditorConfig`
- `EditorStyle`

后续建议补齐的公开 API：

- `EditorMediaProvider` 或图片/视频分离 Provider

建议收敛为内部实现的类：

- `EditorAdapter`
- 各类 `ViewHolder`
- `BlockEditorOperations`
- `BlockActionManager`
- `SelectionManager`
- `FocusManager`
- `BlockEditorUiSideEffectsImpl`

---

## 7. 分阶段实施计划

### 阶段 0：现状冻结与依赖盘点

目标：明确哪些代码可以进入 SDK，哪些必须留在业务层。

任务：

- 梳理 `richtext` 包对 `app` 其他包的依赖。
- 标记 FishMemory 业务专属逻辑，如发布、草稿、上传、DeepSeek Key。
- 记录现有功能基线，保证后续抽离前后行为一致。

验收标准：

- 输出 SDK 边界清单。
- 明确第一批迁移文件列表。

### 阶段 1：新增 `:richtext-editor` 模块（已完成）

目标：在当前仓库内建立 Android Library SDK 模块。

任务：

- 在 `settings.gradle.kts` 中加入 `include(":richtext-editor")`。
- 新建 Android Library 模块与独立 namespace。
- 迁移 `richtext` 包中的核心代码和资源。
- `app` 改为通过 `implementation(project(":richtext-editor"))` 接入。

验收标准：

- `:richtext-editor` 可以单独构建。已通过 `gradlew.bat :richtext-editor:assembleDebug` 验证。
- `:app` 发布页仍能随 App 编译。已通过 `gradlew.bat :app:assembleDebug` 验证。

### 阶段 2：业务依赖解耦（进行中）

目标：SDK 不再依赖 FishMemory 的发布页、草稿、网络与数据库实现。

任务：

- 视频上传改为 `EditorVideoUploader`。（已完成）
- 图片上传继续收敛为 `EditorMediaProvider` 或独立 `EditorImageUploader`。
- AI 润色改为 `AiAssistProvider`。（已完成）
- 链接元信息解析改为可替换接口或默认实现。
- 移除 SDK 中对 `PublishActivity`、Draft、Repository、BuildConfig Key 的直接依赖。

验收标准：

- SDK 模块不引用 `com.fishmemory.app.ui.publish` 的业务类。
- DeepSeek 实现只存在于 app 或 extension 示例中。当前 `DeepSeekAiAssistProvider` 已在 app 侧承接具体实现。

### 阶段 3：Facade API 收敛（进行中）

目标：外部接入方不需要理解内部 RecyclerView 细节。

任务：

- 统一编辑器入口，如 `BlockEditorView`。
- 暴露 `setDocument()`、`getDocument()`、`loadJson()`、`exportJson()`。
- 暴露内容变化、焦点变化、媒体点击、错误等回调。
- 将内部类改为 `internal` 或移动到 `internal` 包。
- 已完成：编辑态 `getDocument()`、`setDocument()`、`exportStandardJson()`。
- 已完成：编辑态 `loadStandardJson()`。
- 已完成：统一事件回调 `EditorCallback`。
- 已完成：标准 JSON facade `RichTextDocumentCodec`。
- 已完成：只读渲染 facade `ReadOnlyRichTextRenderer`。
- 已完成：本地文章详情页从内部 Adapter 迁移到只读渲染 facade。
- 已完成：视频上传与重试链路改为依赖 `EditorVideoUploader` 接口。
- 已完成：AI 润色链路改为依赖 `AiAssistProvider` 接口，DeepSeek 作为 App 侧 provider 注入。

验收标准：

- sample 中 10 到 20 行代码可以完成基础接入。
- 外部不直接调用 Adapter、ViewHolder、SelectionManager。
- 当前状态：编辑态和只读态已有阶段性交付入口；发布页仍保留部分历史 setter，作为兼容层保留。

### 阶段 4：文档协议与兼容层

目标：让 SDK 的数据格式稳定、可测试、可解释。

任务：

- 定义标准 JSON schema。
- 提供不同块类型的 JSON 示例。
- 补充旧 FishMemory 草稿格式到标准格式的 converter。
- 标注字段版本号，预留升级空间。

验收标准：

- 文档导出后可完整导入。
- 旧草稿数据可以兼容迁移。

### 阶段 5：测试与 Demo

目标：证明 SDK 是可维护组件，而不是简单搬包。

任务：

- 增加 JSON 导入导出可逆测试。
- 增加 Span 解析与应用测试。
- 增加文本块 split / merge 测试。
- 增加空文档、图片块、代码块、链接块边界测试。
- 建立最小 Demo 页面：基础编辑、JSON 导入导出、AI 扩展示例。

验收标准：

- 核心转换逻辑有单元测试覆盖。
- Demo 能展示 SDK 的最小接入链路。

---

## 8. 简历表达建议

可将原项目描述从“资讯 App”调整为“从业务项目中沉淀 SDK”：

```text
从 FishMemory 内容发布模块中抽离并沉淀 Android 原生块式富文本编辑器 SDK，基于 RecyclerView 实现多类型 Block 编辑、跨块焦点管理、富文本 Span 解析、结构化 JSON 持久化与只读渲染；通过 Facade API、媒体/AI 扩展接口和 sample app 降低第三方接入成本。
```

可以拆成 3 条简历 bullet：

```text
- 设计 Document / EditorBlock / SpanData 结构化文档模型，支持标题、正文、图片、视频、代码块、链接卡片、列表等多类型内容块的编辑与渲染。
- 封装 BlockEditorView 对外入口，收敛 Adapter、ViewHolder、SelectionManager 等内部实现，提供 JSON 导入导出、内容变化回调与可配置样式能力。
- 将 DeepSeek AI 润色与媒体上传改造为可插拔扩展接口，避免业务网络、密钥和上传流程侵入编辑器核心。
```

---

## 9. 下一阶段优先级

阶段 1 已完成，阶段 2/3 已达到仓库内阶段性交付状态。下一步建议只做三件事：

1. 补图片上传接口或统一 `EditorMediaProvider`，完成媒体扩展点闭环。
2. 写清楚 JSON schema，并把当前基础测试扩展为更完整的导入导出可逆测试。
3. 建立独立 `sample` 模块，用 1 个页面演示编辑态、只读态、JSON、AI/provider 与视频上传 provider。

暂缓事项：

- 暗黑模式深度适配
- 社区、消息、评论、账号体系
- 云端同步
- 发布 Maven Central
- 复杂协同编辑

这些能力不是不能做，而是当前不如 SDK 边界沉淀更能提升项目含金量。
