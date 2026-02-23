## FishMemory 富文本模块（最终目录结构说明）

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
