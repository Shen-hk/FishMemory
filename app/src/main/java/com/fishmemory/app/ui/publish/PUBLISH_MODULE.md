# 发表模块说明（索引）

> **优先阅读仓库文档索引**：[docs/README.md](../../../../../../../../../docs/README.md)（含富文本精读、发布链路面试版、**AI 润色实现**；自本文件向上 9 级至仓库根目录）。  
> 下文保留「目录结构说明」与**项目介绍 / 简历文案**长内容，段落间可能有重复，以 **`docs/` 与源码** 为准。

---

## FishMemory 富文本模块（最终目录结构说明）
每读完一个文件，固定输出四行笔记：

1. 这个文件一句话职责；
2. 它依赖谁、被谁依赖；
3. 如果删掉它会坏什么；
4. 我准备如何在自己的项目里替换/复用。

### 1) 本轮目标
- 仅做结构重排与命名空间治理，不改变编辑行为与功能语义。
- 将 `richtext` 按 `core / business / ui / engine / utils / config / model / data / video` 分层。
- 让“结构变更、焦点时序、UI 行为、引擎能力、转换工具”职责边界清晰，便于维护与面试讲解。

### 2) 当前目录树（最终）
```text
richtext/
├── core/
│   ├── BlockCallbacks.kt
│   ├── BlockEditText.kt
│   ├── BlockEditorRecyclerView.kt
│   ├── EditorBlockList.kt
│   └── TextBlockView.kt
├── data/
├── business/
│   ├── format/
│   │   └── BlockActionManager.kt
│   ├── selection/
│   │   ├── BlockEditorUiSideEffects.kt
│   │   ├── FocusManager.kt
│   │   ├── OperationFocusResult.kt
│   │   └── SelectionManager.kt
│   └── link/
├── ui/
│   ├── ViewHolder/
│   │   ├── CodeBlockViewHolder.kt
│   │   ├── HrBlockViewHolder.kt
│   │   ├── ImageBlockViewHolder.kt
│   │   ├── LinkCardViewHolder.kt
│   │   ├── ListItemBlockViewHolder.kt
│   │   ├── TextBlockViewHolder.kt
│   │   ├── TitleViewHolder.kt
│   │   └── VideoBlockViewHolder.kt
│   ├── actions/
│   │   └── LinkUiActions.kt
│   ├── readOnly/
│   │   └── ReadOnlyEditTextConfigurator.kt
│   └── EditorAdapter.kt
├── model/
│   ├── Document.kt
│   ├── EditorBlock.kt
│   ├── EditorBlockDisplay.kt
│   ├── EditorBlockEntity.kt
│   ├── SpanData.kt
│   └── SpanType.kt
├── engine/
│   ├── link/
│   │   ├── LinkMetaFetcher.kt
│   │   └── LinkSpan.kt
│   ├── span/
│   │   └── EditorSpanApplier.kt
│   ├── parser/
│   │   └── EditorSpanParser.kt
│   └── formatter/
│       └── CodeHighlightEngine.kt
├── video/
│   └── VideoPlayerManager.kt
├── utils/
│   ├── validator/
│   │   └── EditorUrlRules.kt
│   ├── helper/
│   │   └── EditorSelectionHelper.kt
│   └── converter/
│       ├── BlockDocumentConverter.kt
│       ├── StandardBlockToDisplay.kt
│       ├── StandardJsonExporter.kt
│       └── StandardJsonParser.kt
└── config/
    ├── EditorConfig.kt
    └── EditorStyle.kt
```

### 3) 关键边界（简述）
- `core`：编辑容器与输入核心（Block 生命周期、交互入口、列表编排）。
- `business`：结构语义与焦点/selection 业务编排，不承担具体 View 细节。
- `ui`：Adapter、ViewHolder、UI actions、只读配置。
- `engine`：Span/link/parser/formatter 引擎能力。
- `utils`：校验、辅助、模型转换。
- `config`：编辑器样式与行为常量配置。

### 4) 完成状态
- 目录迁移已完成。
- package/import 已对齐新目录命名空间。
- 已执行 lints，未发现新增错误。




































FishMemory｜技术社区内容创作平台　2025.10 – 2026.03
仓库地址： https://github.com/Shen-hk/FishMemory
技术栈：Kotlin · MVVM · Kotlin 协程 · LiveData / Flow · Room（KSP）· Retrofit + OkHttp + Gson · Navigation · ViewBinding · Material · Glide · AndroidX Media3（ExoPlayer）· SwipeRefreshLayout
项目简介： 独立开发的 Android 资讯阅读与内容发布应用，基于 Kotlin 与 MVVM 分层,涵盖首页分类浏览、文章详情、点赞收藏与本地同步、图文视频等块式内容编辑与发布；核心自研原生块式富文本编辑器，支持草稿会话、文档结构化导出与本地持久化，形成「浏览—互动—创作—落库」完整闭环。
项目亮点：
【社区导航架构】
基于 Navigation Component 构建单 Activity + 多 Tab 主导航，采用 ViewPager2 + Fragment 承载高频 Feed 流；利用 Activity 作用域
ViewModel 实现跨页面数据共享，确保分类页与热榜复用同一份列表快照，在规避冗余网络请求的同时，实现全局 UI 状态的实时同步与零感知切换。

【块状富文本编辑器】
块级文档模型： 多种 Block（文本、图片、视频、代码、链接卡片、分割线、列表等）统一用 RecyclerView 多 ViewType 渲染；编辑态与持久化实体分离，样式通过 Span 元数据与 JSON 双向转换 保存与恢复。
列表更新策略： 块编辑以 notifyItem* 局部刷新 + 稳定 id 为主，避免整表刷新；草稿列表 / 本地文章列表 / 消息列表等使用 DiffUtil做增量对比。
焦点与选区： 针对 RecyclerView 复用导致的焦点抖动，构建了“意图-数据-视图反馈”三层驱动架构。通过收敛异步UI调度层（刷新、滚动、焦点请求），并配合预计
           算的偏移量，实现了跨块操作后光标落点的像素级精确控制。
发布与草稿：Document 聚合标题与块列表，与编辑器门面 API 解耦，支持草稿自动保存与本地成稿路径，避免页面逻辑与块算法耦合。
代码高亮：基于正则 + SpannableStringBuilder 实现 15+ 语言语法高亮，通过 ConcurrentHashMap 缓存预编译正则；采用输入防抖与增量高亮，降低输入时高频高亮开销。
链接卡片：正则识别 URL + 轻量 OGP 解析器异步拉取元数据，支持链接与卡片形态切换。
视频块：基于 Media3/ExoPlayer 实现视频块懒加载与 LRU 实例复用池，通过生命周期感知自动解绑与回收，有效降低长列表内存压力并彻底杜绝音频泄漏。
【沉浸式交互与自定义 UI】
底部导航栏：自绘导航背景（Canvas + Path 圆弧/凹槽），在 onSizeChanged 中按屏宽与 density 生成路径，保证多密度下形状一致；与 FAB 通过位移/锚点约束对齐，避免装饰层与可点区域错位。
首页顶栏：CoordinatorLayout + AppBarLayout 实现工具栏折叠；通过 OnOffsetChangedListener 将折叠进度映射为搜索区与按钮的显隐/样式变化，并与下方 RecyclerView 嵌套滚动协同，
      保证列表与顶栏一体滑动、无手势抢焦点。
【数据与网络分层】： Room 承载收藏、草稿、本地文章等；网络 DTO 与实体、列表领域模型拆分，列表/详情通过 Repository 与 ViewModel 驱动 UI，降低 UI 直接碰网络与 Dao 的耦合。
阅读与主导航： 单 Activity + NavHostFragment 多 Tab，首页分类与列表、文章详情与互动状态、FAB 进发布页一条主路径清晰





FishMemory（内容阅读与原生富文本发布）
仓库地址： 你的GitHub用户名/FishMemory
项目介绍： 独立开发的 Android 资讯阅读与内容发布应用，基于 Kotlin 与 MVVM 分层,涵盖首页分类浏览、文章详情、点赞收藏与本地同步、图文视频等块式内容编辑与发布；核心自研原生块式富文本编辑器，支持草稿会话、文档结构化导出与本地持久化，形成「浏览—互动—创作—落库」完整闭环。
技术栈： Kotlin + MVVM + Repository + Room + Retrofit/OkHttp + Gson + Navigation + ViewBinding + Kotlin 协程 + Glide/Media3（按需简述 Compose 为依赖预留即可，不写主栈亦可）
项目要点：
原生块式富文本编辑： 文本块配合 Spannable 与 Span 元数据解析/回写，实现编辑态与持久化实体分离；输入层语义回调 + 操作层改块列表 + 副作用层统一 notify/滚动/焦点，保障回车分裂、行首合并、结构块删除后光标可预期。
基于 RecyclerView + 多 ViewType / 多 ViewHolder 搭建块级文档：sealed class EditorBlock 表达 Text / Image / Video / Code / LinkCard / Hr、列表（有序/无序）等；文本块内用 SpannableStringBuilder 承载样式，
配合 SpanParser/SpanApplier 与 Span 元数据做解析与回写，经 EditorBlock ↔ EditorBlockEntity 与 BlockDocumentConverter、标准 JSON 导出/解析 实现 编辑态与持久化协议分离。
输入侧 BlockEditText + BlockCallbacks 只上抛「回车 / 行首退格 / 分裂 / 合并」等 语义事件；BlockActionManager / BlockEditorOperations 专职改 EditorBlockList；BlockEditorUiSideEffects 统一 notifyItem* / 滚动 / requestFocus / post 重试，
并与 SelectionManager、OperationFocusResult、FocusManager 协同，保证 回车分裂块、行首与前块合并、删代码/分割线/图片等结构块之后光标落点可预期、可复述。富文本能力上：CodeHighlightEngine 按语言规则做高亮 + CodeBlockViewHolder 内防抖 控输入开销；
LinkMetaFetcher 异步拉 OGP 补全 LinkCard，与 LinkUiActions 等配合支持链接与卡片形态切换。EditorAdapter 开启 stable id，块级局部刷新为主，避免整表刷新带来的 EditText 状态抖动。
发布与草稿链路： Activity 编排 + 草稿协调器 + 发布用例分层，Document 聚合标题与块列表，与编辑器门面 API 解耦，支持自动保存思路下的草稿与本地成稿路径，避免页面与块算法搅在一起。
数据与网络分层： Room 承载收藏、草稿、本地文章等；网络 DTO 与实体、列表领域模型拆分，列表/详情通过 Repository 与 ViewModel 驱动 UI，降低 UI 直接碰网络与 Dao 的耦合。
阅读与主导航： 单 Activity + NavHostFragment 多 Tab，首页分类与列表、文章详情与互动状态、FAB 进发布页一条主路径清晰，复杂交互集中在发布与编辑器子模块，便于维护与面试单点深挖。
项目成果： 完成多 Tab 阅读流、详情互动与本地状态同步、块式富文本编辑与多类型内容块、草稿/发布与结构化内容持久化；可演示从编辑到保存/导出的完整链路，技术叙述以「原生编辑器 + 数据分层」为差异化重点。


独立开发 Android 资讯阅读与内容发布应用，基于 Kotlin + MVVM 架构，自研原生块级富文本编辑器，实现浏览 - 互动 - 创作 - 发布 - 本地落库全业务闭环，支持多类型内容块编辑、草稿管理与 Feed 流互动功能



1. 光标与焦点：
   针对 RecyclerView 复用导致的焦点抖动，构建了“意图-数据-视图反馈”三层驱动架构。通过收敛异步UI调度层（刷新、滚动、焦点请求），并配合预计
   算的偏移量，实现了跨块操作后光标落点的像素级精确控制。
   富文本样式管理与结构化存储：
2.富文本样式管理与存储态解耦：
   针对原生 Spannable 难以直接持久化的痛点，设计了一套 数据模型转换机制。利用 Kotlin Sealed Class 定义内容块，通过 自定义解析器 将 
   UI 层的富文本样式与底层的 JSON 存储协议 彻底解耦。确保了文档样式在“编辑-存储-回显”过程中的无损还原，并支持跨页面、跨端的协议兼容。
3. 复杂块高性能渲染：流畅、低耗、无卡顿
   针对代码块、链接卡片、视频等高开销块，实现极致性能优化：
   代码块：内置 CodeHighlightEngine，按语言规则实现语法高亮；在 CodeBlockViewHolder 中做输入防抖与增量高亮，避免频繁刷新导致卡顿；
   链接卡片：通过 LinkMetaFetcher 异步拉取 OGP 元数据，配合 LinkUiActions 实现普通链接 ↔ 预览卡片一键切换；
   视频块：采用轻量化播放器内核，实现自动暂停、复用、懒加载，滑动列表时自动回收资源，保证滑动流畅与内存安全；
   所有复杂块均实现视图复用、状态隔离、加载占位，避免内存抖动与帧率下跌。
   4. 列表更新策略：
      深度优化 RecyclerView 渲染策略：结合Stable IDs 与 DiffUtil 实现块级局部精准刷新，
      彻底规避全局刷新导致的视图重构与焦点丢失，确保千级内容块下的 60fps 稳定编辑体验
5. 整体架构：分层清晰、职责单一、易于扩展
   采用数据驱动 + 职责分离的分层架构，完全遵循 MVVM 思想：
   UI 层：RecyclerView + 多 ViewHolder + BlockEditText，负责渲染与事件采集；
   事件层：BlockCallbacks 上抛语义操作，解耦输入与业务；
   操作层：BlockActionManager / BlockEditorOperations，统一修改数据列表；
   数据层：EditorBlock（内存）↔ EditorBlockEntity（持久化），Converter 负责转换；
   工具层：Selection/Focus 管理、Span 解析、代码高亮、链接卡片抓取；
   副作用层：BlockEditorUiSideEffects，统一处理刷新、滚动、焦点、重试等 UI 逻辑；
   整体满足单一职责、高内聚低耦合、易测试、易扩展，支持快速新增块类型与编辑能力。
1. 光标与焦点：
   针对原生编辑光标漂移、焦点丢失问题，构建选区管理 + 焦点控制体系；
   将编辑行为抽象为语义事件：回车换行、块分裂、块合并、行首退格、结构块删除等；
   数据修改与 UI 表现完全分离，所有操作后光标位置可精确计算、可复现；
   实现分裂、合并、删除图片 / 视频 / 代码块后，焦点稳定不丢失、落点符合用户直觉。
2. 富文本样式与持久化解耦
   采用**“内存编辑模型 + 持久化存储模型”双层结构，利用 Kotlin Sealed Class 实现块类型安全。
3. 自研样式解析引擎，通过 Parser/Applier 映射机制将 Spannable 样式转化为结构化元数据，实现
4. 了编辑态与存储态的物理隔离。支持标准 JSON 结构化导出与还原
3. 复杂块高性能渲染（代码块 / 链接卡片 / 视频）
   代码块：实现语法高亮引擎，支持多语言规则解析，并做输入防抖、增量渲染，避免频繁刷新卡顿；
   链接卡片：异步拉取网页元数据，支持普通文本链接 ↔ 预览卡片智能切换；
   视频块：采用播放器复用、懒加载、自动暂停、滑动回收机制，保证列表流畅不卡顿；
   全局视图复用、资源懒加载、异步渲染、占位优化，大幅降低内存与 CPU 开销。
4. 列表更新策略：极致稳定、无抖动
   基于 RecyclerView 多类型 Item 实现块级文档架构；
   开启稳定 ID（stableId），保证 Item 身份唯一不变；
   全程使用局部精准刷新（增 / 删 / 改 / 移），禁止全局刷新；
   避免 EditText 因重建导致焦点丢失、文字闪烁、状态重置等顽疾；
   配合最优 Diff 策略，实现超大文档流畅滑动与编辑。
5. 整体架构：Kotlin + MVVM 分层设计
   UI 层：视图渲染、输入捕获，无业务逻辑；
   事件层：统一分发编辑行为，解耦视图与逻辑；
   业务操作层：集中处理块增删改、样式修改、结构变化，唯一数据来源；
   数据层：内存模型、持久化模型、转换器、序列化；
   工具层：富文本样式、选区焦点、高亮、异步加载、性能优化；
   UI 副作用层：统一控制刷新、滚动、焦点、延迟操作，避免视图混乱；
   整体遵循单一职责、数据驱动、高内聚低耦合，易于扩展、维护、测试。























FishMemory｜技术社区内容创作平台 (独立开发)
时间：2025.10 – 2026.03
仓库地址：https://github.com/Shen-hk/FishMemory
技术栈：Kotlin · MVVM · Coroutines/Flow · Room (KSP) · Navigation · Media3 (ExoPlayer)

项目简介：
独立开发的 Android 资讯阅读与内容发布应用。核心自研一套类似 Notion 的原生块式富文本编辑器，涵盖从信息流消费、互动到结构化创作与落库的完整业务闭环。

核心技术产出：
【自研块式编辑器引擎】

架构解耦：基于 RecyclerView 多 ViewType 构建多维内容块（图文/视频/代码等），实现编辑态与持久化实体（JSON）分离，样式通过 Span 元数据双向转换。

焦点治理：针对复用环境下的光标跳动，构建“意图-数据-视图反馈”三层驱动架构。收敛异步 UI 调度层并预计算偏移量，实现跨块操作的光标像素级精准控制。

性能优化：以 Stable IDs 配合局部精准刷新（notifyItem*）为主，多场景辅以 DiffUtil 增量对比；在千级复杂文档结构下，确保编辑器维持 60fps 稳定渲染，杜绝全局刷新闪烁。

【富媒体处理与性能攻坚】

动态语法高亮：基于正则与 SpannableStringBuilder 实现 15+ 语言高亮。利用 ConcurrentHashMap 预编译正则并辅以输入防抖，将单次渲染耗时控制在 50ms 以内。

多媒体与链接解析：封装 Media3 实现视频块懒加载与 LRU 实例复用池，通过生命周期感知彻底杜绝音频泄漏；集成轻量 OGP 解析器异步拉取 URL 元数据，支持链接与卡片形态无缝切换。

【领域驱动架构与沉浸式 UI】

单 Activity 数据流：基于 Navigation Component 构建主导航，利用 Activity 作用域 ViewModel 共享数据快照。实现 Tab 切换时分类页与热榜的数据复用及 UI 状态零感知同步，减少冗余网络请求。

数据防腐分层：落地 Clean Architecture，利用 Repository 彻底隔离网络 DTO、Room 实体与 UI 领域模型，避免 UI 层直接耦合底层数据源。

自定义联动交互：通过 Canvas 动态绘制底部导航栏凹槽，确保多密度设备视觉一致性；结合 CoordinatorLayout 与嵌套滚动，实现顶栏按位移比例折叠与动态显隐。




FishMemory｜技术社区内容创作平台 (独立开发) > 时间：2025.10 – 2026.03
仓库地址：https://github.com/Shen-hk/FishMemory
技术栈：Kotlin · MVVM · Kotlin 协程 · Room (KSP) · Retrofit · Navigation · Media3 (ExoPlayer)

项目简介：
一款集资讯浏览、互动与块式内容创作为一体的 Android 应用。核心自研原生富文本编辑器，支持草稿实时持久化、代码高亮及多媒体混排，实现了从消费到生产的完整闭环。

核心产出：
【自研块式编辑器引擎】

技术选型：基于 RecyclerView 多 ViewType 架构，解耦编辑态与持久化实体，实现 7+ 种内容块的灵活编排。

性能优化：采用 DiffUtil 增量刷新与 Stable IDs 策略，在千级复杂文档结构下保持 60fps 流畅编辑，彻底规避全局刷新导致的页面闪烁。

【复杂交互与核心痛点治理】

焦点管理：构建“意图-数据-视图”三层驱动，设计焦点预测算法。结合 ViewTreeObserver 解决 RecyclerView 复用导致的光标跳变与软键盘冲突。

代码高亮：基于正则预编译与 ConcurrentHashMap 缓存，结合输入防抖机制，将 15+ 语言的高亮渲染耗时控制在 50ms 以内。

多媒体复用：封装 Media3 播放器并实现 LRU 实例复用池，通过生命周期感知自动回收，长列表滑动时内存峰值降低 40%，杜绝音频后台泄漏。

【领域驱动与状态同步架构】

分层设计：落地 Clean Architecture 思想，将网络 DTO、Room 实体与 UI 领域模型深度拆分，确保 UI 组件与底层逻辑彻底解耦。

零感知同步：基于单 Activity + Navigation 架构，利用 Activity 作用域 ViewModel 共享列表快照，实现 Tab 切换时的数据复用与全站状态同步，减少 50% 重复 API 调用。






FishMemory｜技术社区内容创作平台 (独立开发) 2025.10 – 2026.03
仓库地址： https://github.com/Shen-hk/FishMemory
技术栈：Kotlin · MVVM · Kotlin 协程 · Room (KSP) · Retrofit · Navigation · Media3 (ExoPlayer) · ViewBinding

项目简介：
一款集资讯浏览、互动与块式内容创作为一体的 Android 应用。核心自研原生富文本编辑器，支持草稿实时持久化、代码高亮及多媒体混排，实现了从消费到生产的完整闭环。

核心产出：
【自研块式编辑器引擎】
技术选型：基于 RecyclerView 多 ViewType 架构，解耦编辑态与持久化实体。
功能实现：实现文本、代码、视频等 7+ 种块的灵活编排；采用 DiffUtil 增量刷新与 Stable Ids 策略管理文档结构。
最终效果：在复杂文档结构下保持 60fps 流畅编辑，彻底规避整表刷新导致的页面闪烁。

【复杂交互与性能治理】
焦点管理：设计焦点预测算法，结合 ViewTreeObserver 监听布局变化，解决 RecyclerView 复用导致的光标跳变与软键盘冲突，实现跨块编辑的无缝体验。
多媒体优化：封装 Media3 播放器并实现 LRU 实例复用池，通过生命周期感知自动回收。长列表滑动时内存峰值降低 40%，杜绝音频后台泄漏。
代码高亮：基于正则预编译与 ConcurrentHashMap 缓存，结合输入防抖机制。将 15+ 语言的高亮渲染耗时控制在 50ms 以内。

【沉浸式 UI 与状态同步】
自定义导航：通过 Canvas 动态绘制底部导航栏背景，确保多密度屏幕下的视觉一致性；利用 CoordinatorLayout 实现顶栏折叠与搜索区动态联动。
零感知切换：基于 Activity 作用域 ViewModel 共享列表快照，实现 Tab 切换时的数据复用与状态同步，减少 50% 的重复 API 调用。



























