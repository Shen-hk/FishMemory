# FishMemory 富文本编辑器精读手册（从 0 到工程落地）

> 目标读者：Android 初学者到中级开发者  
> 目标结果：不仅能看懂当前项目，还能复刻一个可面试讲清楚的块级富文本编辑器

---

## 1. 先把目标讲清楚：你到底要学会什么

这个模块不是“给 `EditText` 加几个 Span”这么简单。它的本质是：

1. 用 `RecyclerView + 多 Block 类型` 搭出可扩展的编辑器容器；
2. 把输入行为（回车/退格/粘贴/焦点切换）转换成可预测的结构化操作；
3. 把编辑态数据与持久化数据解耦，保证可保存、可恢复、可导出；
4. 在复杂交互下维持光标和选区稳定，避免“看起来随机”的编辑行为。

如果你最终能讲清下面四句话，说明你真正吃透了：

- 为什么不用 WebView 和现成富文本库；
- 为什么要做块级模型（Block）而不是一坨大文本；
- 为什么输入事件先变成语义回调，再交给操作层；
- 为什么焦点管理和 UI 副作用必须从数据操作中分离。

---

## 2. 总体架构地图：先看全局，再钻细节

以 `app/src/main/java/com/fishmemory/app/ui/publish/richtext` 为中心，可以按六层理解：

1. **模型层（Model）**：定义文档和块结构  
   - `core/model/Document.kt`  
   - `core/model/EditorBlock.kt`  
   - `core/model/EditorBlockEntity.kt`  
   - `core/model/EditorBlockList.kt`

2. **输入协议层（Input Contract）**：把控件事件变成语义事件  
   - `core/BlockCallbacks.kt`  
   - `ui/view/BlockEditText.kt`

3. **编排与操作层（Orchestration + Action）**：决定“怎么改块列表”  
   - `ui/container/BlockEditorOperations.kt`  
   - `business/format/BlockActionManager.kt`

4. **焦点与选区层（Selection/Focus）**：保障交互一致性  
   - `business/selection/SelectionManager.kt`  
   - `business/selection/OperationFocusResult.kt`  
   - `business/selection/FocusManager.kt`  
   - `business/selection/BlockEditorUiSideEffects.kt`

5. **渲染层（UI Rendering）**：把块显示出来  
   - `ui/adapter/EditorAdapter.kt`  
   - `ui/adapter/*ViewHolder.kt`  
   - `ui/container/BlockEditorRecyclerView.kt`  
   - `res/layout/item_*.xml`

6. **转换与导出层（Converter/Serializer）**：可持久化可恢复  
   - `core/converter/BlockDocumentConverter.kt`  
   - `core/engine/parser/EditorSpanParser.kt`  
   - `core/engine/span/EditorSpanApplier.kt`  
   - `core/converter/StandardJsonExporter.kt`  
   - `core/converter/StandardJsonParser.kt`

7. **发布页扩展能力（可选）**：AI 润色等与编辑器协作、但不污染块模型  
   - `ui/publish/ai/`（ViewModel、SSE 客户端、Prompt 组装）  
   - 说明文档：[docs/ai-polish-implementation.md](ai-polish-implementation.md)

一句话：**编辑器行为由“数据规则”驱动，UI 负责表现和副作用执行。**

---

## 3. 学习前置知识清单（按先后）

### 3.1 Kotlin 基础（必须）
- `sealed class`：建模不同 Block 类型；
- 数据类和拷贝：在不破坏引用关系时做安全更新；
- 高阶函数与回调：输入事件上抛。

### 3.2 Android View 基础（必须）
- `RecyclerView` 多类型 `ViewHolder`；
- `Adapter` 的刷新语义（`notifyItemInserted` 等）；
- 焦点与滚动配合（`requestFocus` + `scrollToPosition` + `post`）。

### 3.3 文本与输入（核心）
- `Editable` / `SpannableStringBuilder`；
- Span 范围、叠加、移除；
- `Selection` 的起止与光标位置；
- IME 行为（回车键语义变化）。

### 3.4 架构思维（核心）
- 数据与 UI 解耦；
- “操作结果”对象化（例如焦点结果）；
- 副作用隔离（UI 层统一处理）。

### 3.5 序列化思维（进阶）
- 编辑态模型和持久化模型分离；
- 富文本不能直接存 `Spannable`，要存结构化描述。

---

## 4. 逐文件精读（重点章节）

本章是“照着读源码”的主路线。每个文件都按四个问题看：

1. 这个文件解决什么问题；
2. 对外暴露什么能力；
3. 和哪些类协作；
4. 为什么这样设计。

---

### 4.1 `core/model/Document.kt`

**看点**
- 文档顶层聚合：标题 + 块列表。

**你要理解**
- 页面发布、保存草稿、详情展示，都围绕这个结构流转；
- 它是业务层统一输入输出协议，不应该塞 UI 细节。

**设计原因**
- 用聚合根统一上下游，避免方法参数四处分散（标题一份、正文一份、附加信息一份）。

---

### 4.2 `core/model/EditorBlock.kt`

**看点**
- `sealed class` 定义编辑器支持的块类型，如文本、图片、视频、代码、分割线、链接卡片等。

**你要理解**
- 块类型是“一等公民”，不是“文本里混杂特殊标记”；
- `TextBlock` 持有可编辑文本（通常是 `SpannableStringBuilder`），让块内样式和块间结构分工明确。

**设计原因**
- 后续新增块类型（例如引用块）时，只需扩展模型 + ViewHolder + 操作分支，改动可控。

---

### 4.3 `core/model/EditorBlockEntity.kt`

**看点**
- 持久化/传输态 block 结构，字段更偏 JSON 协议。

**你要理解**
- 编辑态模型服务“运行时易操作”；
- Entity 模型服务“跨进程/跨网络稳定传输”。

**设计原因**
- `Spannable` 不能直接作为持久化协议，必须转换为文本 + Span 数据。

---

### 4.4 `core/model/EditorBlockList.kt`

**看点**
- 编辑器块列表的统一读写入口；
- 提供 `insert/remove/replace` 等操作。

**你要理解**
- 不让 `Adapter` 或任意 ViewHolder 直接改底层集合；
- 所有结构变更集中在可控边界内。

**设计原因**
- 防止“哪里都能改列表”导致的数据一致性问题；
- 方便未来测试和日志追踪。

---

### 4.5 `core/BlockCallbacks.kt`

**看点**
- 定义块内输入与外层容器的协议，如分裂、合并、回车请求、内容变化回调。

**你要理解**
- 这是解耦关键：输入控件只报告“意图”，不直接改全局结构。

**设计原因**
- 让 `BlockEditText` 可复用、可测试，避免变成业务黑洞。

---

### 4.6 `ui/view/BlockEditText.kt`

**看点**
- 输入层核心；
- 处理按键、回车、删除边界；
- 在合适时机触发 `BlockInteractionListener`。

**你要理解**
- 这里负责“识别行为”，不负责“执行跨块操作”；
- 例如“光标在行首按退格”应上抛为 merge 意图，而不是直接拼接前一个块。

**设计原因**
- 把“输入解码”和“业务规则”分离，避免控件层复杂度爆炸。

---

### 4.7 `ui/adapter/EditorAdapter.kt`

**看点**
- 多类型 block 的绑定中心；
- 标题与正文块的 position 映射；
- 给文本 ViewHolder 注入交互回调。

**你要理解**
- `Adapter` 负责渲染和事件转发，不做复杂规则决策；
- 只读模式与编辑模式切换要尽量局部，不污染业务层。

**设计原因**
- 让渲染层保持“薄”，业务复杂度下沉到操作层。

---

### 4.8 `ui/container/BlockEditorRecyclerView.kt`

**看点**
- 组件门面（Facade）；
- 对外提供编辑器能力（设置内容、获取内容、插入块、监听变化等）；
- 内部组合 `EditorBlockList + Adapter + Operations`。

**你要理解**
- 页面（如发布页）只与它交互，不直接操控内部细节；
- 它承担“模块边界保护层”的角色。

**设计原因**
- 降低页面层心智负担，让模块可替换、可重构。

---

### 4.9 `ui/container/BlockEditorOperations.kt`

**看点**
- 绑定 Adapter 回调到业务处理；
- 把分裂/合并/删除/焦点操作串联起来；
- 管理 `getBlocks/setBlocks` 的转换调用。

**你要理解**
- 这是编排层，不是纯业务层；
- 它负责“事件路由 + 调度 + 收尾”，例如通知 UI、派发内容变更。

**设计原因**
- 从大而全容器中拆出流程层，降低单类复杂度。

---

### 4.10 `business/format/BlockActionManager.kt`

**看点**
- 核心结构操作实现：split、merge、插入代码块、列表行为等。

**你要理解**
- 这里应尽量偏“数据操作”，少依赖具体 UI；
- 操作后只返回必要结果（如焦点目标），不直接 `requestFocus`。

**设计原因**
- 业务规则集中，避免“同一规则散落在多个 ViewHolder”。

---

### 4.11 `business/selection/SelectionManager.kt`

**看点**
- 管理结构块（图片、代码、链接卡片等）的选中态；
- 管理删除后的焦点回落策略。

**你要理解**
- 编辑器一致性核心之一：删除后焦点落在哪里；
- 错误焦点会造成“用户感觉编辑器坏了”。

**设计原因**
- 将“焦点策略”独立成单元，便于演进和问题隔离。

---

### 4.12 `business/selection/OperationFocusResult.kt`

**看点**
- 封装操作后应该聚焦到哪里（目标 block、selection 区间等）。

**你要理解**
- 这是在操作层和 UI 副作用层之间传递“意图结果”的桥梁。

**设计原因**
- 防止业务方法里直接调用 UI API，维持层次清晰。

---

### 4.13 `business/selection/BlockEditorUiSideEffects.kt` 与 `BlockEditorUiSideEffectsImpl.kt`

**看点**
- 抽象并实现所有 UI 副作用：`notify`、滚动、尝试聚焦等。

**你要理解**
- 同样的数据操作，在不同 UI 容器下副作用实现可能不同；
- 抽接口后业务可复用，替换 UI 成本更低。

**设计原因**
- 典型“依赖倒置”：高层规则不依赖低层具体控件。

---

### 4.14 `core/converter/BlockDocumentConverter.kt`

**看点**
- `EditorBlock` 与 `EditorBlockEntity` 双向转换总入口。

**你要理解**
- 外部只关心 Document 协议，内部如何持有 `Spannable` 不暴露给上游；
- 这是“编辑器可存可读”的中枢。

**设计原因**
- 避免转换逻辑散落到页面、Adapter、业务类中。

---

### 4.15 `core/engine/parser/EditorSpanParser.kt`

**看点**
- 把运行时 `Spannable` 解析为可序列化 `SpanData`。

**你要理解**
- Span 要存的是“类型 + 范围 + 参数”，而不是对象实例。

**设计原因**
- 跨端、跨版本时，结构化数据可兼容和迁移。

---

### 4.16 `core/engine/span/EditorSpanApplier.kt`

**看点**
- 将 `SpanData` 应回文本，恢复编辑态视觉与行为。

**你要理解**
- 解析与应用是镜像关系，二者必须协议一致；
- 任何一侧变动，都要考虑兼容历史数据。

**设计原因**
- 建立可靠的“保存 -> 恢复”闭环。

---

### 4.17 `core/engine/formatter/CodeHighlightEngine.kt`

**看点**
- 代码块高亮处理，作用于可编辑内容。

**你要理解**
- 代码块是独立场景：高亮、光标、性能都和普通文本不同。

**设计原因**
- 代码展示能力是编辑器专业度体现之一，应有专门引擎而非临时 if。

---

### 4.18 `ui/adapter/CodeBlockViewHolder.kt` 与 `res/layout/item_code_block.xml`

**看点**
- 代码块 UI 呈现与交互边界。

**你要理解**
- 为什么代码块用独立 ViewHolder，而不是普通文本加个样式；
- 因为交互规则和样式策略明显不同。

**设计原因**
- 块级编辑器强调“结构先于样式”。

---

### 4.19 `PublishActivity.kt`（编辑器接入层）

**看点**
- 页面如何构建 `Document`、如何回填旧文档；
- 发布流程如何拿到标准化内容。

**你要理解**
- 页面只调用编辑器门面 API，不应深入操作内部块结构。

**设计原因**
- 防止业务页面和编辑器内部强耦合。

---

## 5. 四条核心链路（必须背下来）

---

### 5.1 回车分裂链路（Split）

1. 用户在文本块按回车；  
2. `BlockEditText` 判定触发分裂语义；  
3. 通过 `BlockInteractionListener.onSplitRequested` 上抛；  
4. `BlockEditorOperations` 路由到 `BlockActionManager`；  
5. 改写 `EditorBlockList`（当前块拆为前后两块）；  
6. 返回 `OperationFocusResult`；  
7. `UiSideEffects` 执行插入动画、滚动和焦点落位。

**关键思想**：输入层不直接改全局，业务层不直接调 UI。

---

### 5.2 行首退格合并链路（Merge）

1. 光标在块首按退格；  
2. 输入层上抛 merge 意图；  
3. 操作层决定是否可并、如何并；  
4. 更新块列表并计算新光标位置；  
5. 由 `SelectionManager` + `UiSideEffects` 完成焦点回落。

**关键思想**：合并规则集中管理，避免行为漂移。

---

### 5.3 删除结构块链路（Image/Video/Code/Link/Hr）

1. 结构块被选中；  
2. 用户执行删除；  
3. 选中管理器清理状态并移除 block；  
4. 通知 UI 刷新；  
5. 焦点回到合理文本块。

**关键思想**：结构块是独立对象，不应按纯文本删除逻辑处理。

---

### 5.4 保存恢复链路（Converter）

1. 编辑态 `EditorBlock` 获取当前内容；  
2. 文本块走 `EditorSpanParser` 提取 `SpanData`；  
3. 生成 `EditorBlockEntity` / `Document`；  
4. 导出 JSON；  
5. 恢复时反向转换并 `EditorSpanApplier` 回放样式。

**关键思想**：富文本可编辑能力的生命线是“可逆转换”。

---

## 6. 为什么要这样做：设计取舍讲透

### 6.1 为什么不用 WebView
- 可编辑一致性差，输入法细节不可控；
- 原生焦点和光标能力难对齐；
- 与 Android 原生生命周期、性能优化链路不自然。

### 6.2 为什么不用整套第三方编辑器
- 项目目标是“可讲清核心能力”，不是“集成能力”；
- 深层交互（块级焦点、业务定制）受库约束；
- 面试导向下，必须能解释核心机制。

### 6.3 为什么是块级模型
- 结构块（图片/视频/代码/卡片）天然不是纯文本；
- 块级模型更利于增量扩展；
- 便于独立渲染和独立交互策略。

### 6.4 为什么要拆 UI 副作用接口
- 业务逻辑稳定，UI 可替换；
- 降低单元测试复杂度；
- 减少“业务方法里到处 `RecyclerView` API”的脆弱实现。

---

## 7. 工程思维：你真正要内化的能力

1. **边界先行**：谁能改数据、谁只能发意图，先定规则再写代码；  
2. **稳定优先**：光标和焦点比花哨功能重要；  
3. **最小改动**：每次只触达一个维度，保证可回滚；  
4. **可解释性**：每个模块职责一句话能讲清；  
5. **演进友好**：新增块类型时改动范围可预测。

---

## 8. 常见坑位与排查思路

### 8.1 光标错位
- 检查 split/merge 后 selection 计算；
- 检查 `notify` 时机与焦点请求时机；
- 检查是否存在 `post` 延迟导致状态过期。

### 8.2 Span 丢失
- 检查 parser 是否完整提取；
- 检查 applier 是否按同协议恢复；
- 检查转换链路是否有字段遗漏。

### 8.3 列表闪烁或错位刷新
- 检查 block id 与 adapter stable id 一致性；
- 检查局部刷新是否用错全量刷新；
- 检查 position 映射（标题占位）是否偏移。

### 8.4 删除后焦点异常
- 检查 `SelectionManager` 的目标策略；
- 检查 `OperationFocusResult` 是否落空；
- 检查 UI side effects 是否执行失败。

---

## 9. 复刻路线（从 0 做你的版本）

按三阶段做，不要一口气写完：

### 阶段 A（最小闭环）
- 支持标题 + 文本块；
- 支持回车分裂、行首退格合并；
- 支持保存恢复纯文本（先不做 Span）。

### 阶段 B（富文本能力）
- 文本块支持基础 Span（加粗、斜体、链接）；
- 增加 `SpanData` 转换；
- 做到保存恢复样式一致。

### 阶段 C（结构块能力）
- 增加代码块、图片块、分割线；
- 增加选中态和删除后焦点回落；
- 做基础性能优化（局部刷新、减少无效重绘）。

完成这三阶段，你就从“会用编辑器”升级到“会造编辑器”。

---

## 10. 给你的学习执行法（强烈建议）

每读完一个文件，固定输出四行笔记：

1. 这个文件一句话职责；  
2. 它依赖谁、被谁依赖；  
3. 如果删掉它会坏什么；  
4. 我准备如何在自己的项目里替换/复用。

坚持 2 周，你会明显感觉：代码不再是“文件堆”，而是一套有逻辑的系统。

---

## 11. 最后结论

FishMemory 这套富文本实现的价值，不在“功能多”，而在它逐步形成了工程化编辑器应有的骨架：

- 块级模型；
- 输入语义化；
- 操作层与副作用层分离；
- 可逆转换闭环；
- 可扩展的多类型渲染。

只要你按本手册的顺序把链路逐条走通，再自己复刻一个最小版，你就不仅能“看懂这个项目”，还能够把它变成你自己的核心项目能力。

---

## 12. 相关文档索引

| 文档 | 用途 |
|------|------|
| [docs/README.md](README.md) | 全库文档目录与阅读顺序 |
| [docs/publish-richtext-interview-deep-dive.md](publish-richtext-interview-deep-dive.md) | 发布链路 + 块状编辑器面试版 |
| [docs/ai-polish-implementation.md](ai-polish-implementation.md) | DeepSeek 流式润色实现细节 |

