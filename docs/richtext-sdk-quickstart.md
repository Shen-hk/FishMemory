# FishRichEditor SDK 接入说明

本文档面向 `:richtext-editor` 的接入方，记录当前阶段已经稳定下来的最小接入方式。FishMemory 的 `app` 模块仍作为完整示例与回归验证场景。

---

## 1. 模块依赖

仓库内接入：

```kotlin
dependencies {
    implementation(project(":richtext-editor"))
}
```

当前 SDK 是 Android Library 模块，尚未发布到 Maven。等 API 包稳定、sample 最小化后，再考虑独立仓库或远程发布。

---

## 2. 编辑态接入

布局中直接使用 `BlockEditorRecyclerView`：

```xml
<com.fishmemory.app.ui.publish.richtext.ui.container.BlockEditorRecyclerView
    android:id="@+id/editor"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

基础代码：

```kotlin
binding.editor.setEditorCallback(object : EditorCallback {
    override fun onContentChanged() {
        saveDraft(binding.editor.getDocument())
    }

    override fun onImagePreviewRequested(url: String) {
        openImagePreview(url)
    }

    override fun onImageMenuRequested(anchorView: View, blockId: String) {
        showImageMenu(anchorView, blockId)
    }

    override fun onAiPolishRequested(blockId: String) {
        showAiStyleDialog(blockId)
    }
})
```

导入导出：

```kotlin
val document = binding.editor.getDocument()
val json = binding.editor.exportStandardJson()

binding.editor.setDocument(document)
binding.editor.loadStandardJson(json)
```

当前仍保留 `setOnContentChangedListener`、`setOnImageBlockPreviewRequested` 等旧方法，方便 FishMemory 现有页面平滑迁移。新接入建议优先使用 `setEditorCallback()`。

---

## 3. 只读渲染接入

文章详情页等只读场景使用 `ReadOnlyRichTextRenderer`，避免接入方直接操作 `EditorAdapter` 或 ViewHolder。

```kotlin
val renderer = ReadOnlyRichTextRenderer(recyclerView)
renderer.onImagePreviewRequested = { url -> openImagePreview(url) }
renderer.renderStandardJson(json)
```

生命周期中可转发视频暂停：

```kotlin
override fun onPause() {
    super.onPause()
    renderer.pauseVisibleVideoPlayback()
}
```

---

## 4. 视频上传扩展

宿主 App 实现 `EditorVideoUploader`，SDK 只关心进度、成功 URL 和失败状态。

```kotlin
class MyVideoUploader : EditorVideoUploader {
    override fun enqueueUpload(
        blockId: String,
        localUri: String,
        callback: EditorVideoUploader.Callback,
    ) {
        callback.onProgress(blockId, 30)
        callback.onSuccess(blockId, "https://cdn.example.com/video.mp4")
    }

    override fun cancelUpload(blockId: String) {
        // cancel upload job
    }
}
```

注入：

```kotlin
binding.editor.setVideoManagers(
    playerManager = videoPlayerManager,
    uploadManager = MyVideoUploader(),
)
```

FishMemory 当前的 `VideoUploadManager` 是默认接入示例，后续可迁出为 sample 或 extension。

---

## 5. AI 润色扩展

SDK 通过 `AiAssistProvider` 定义 AI 能力，具体模型、Key、Prompt、网络协议都由宿主 App 负责。

```kotlin
class MyAiProvider : AiAssistProvider {
    override fun polish(
        blockList: EditorBlockList,
        blockId: String,
        style: AiPolishStyle,
    ): Flow<String> {
        return flow {
            emit("润色后的文本")
        }
    }
}
```

FishMemory 的 `DeepSeekAiAssistProvider` 是 App 侧示例实现：它负责读取 App 注入的 Key、组装 Prompt，并通过 DeepSeek 兼容流式接口返回文本增量。

---

## 6. 当前公开 API

- `BlockEditorRecyclerView`：编辑态入口
- `EditorCallback`：统一编辑器事件回调
- `RichTextDocumentCodec`：标准 JSON 导入导出工具
- `ReadOnlyRichTextRenderer`：只读渲染入口
- `EditorVideoUploader`：视频上传扩展接口
- `AiAssistProvider`：AI 润色扩展接口
- `Document` / `EditorBlockEntity`：编辑态文档模型
- `EditorConfig` / `EditorStyle`：配置与样式模型

---

## 7. 交付边界

当前阶段已经适合作为“仓库内 SDK 沉淀成果”展示：

- SDK 已从 `app` 抽为独立 Android Library 模块。
- 编辑态、只读态、标准 JSON、视频上传、AI 润色都有明确入口。
- FishMemory App 作为完整接入示例，能持续验证原发布流程。

暂不承诺：

- Maven Central 发布
- 独立 sample app
- 图片上传统一 Provider
- 完整 JSON schema 文档
- 二进制兼容承诺

这些适合放到下一阶段，而不是继续把当前阶段做散。
