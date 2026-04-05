# FishMemory（Android）— Agent / 协作者说明

本仓库为 **Kotlin Android 应用**（非 Maven Web 模板）。以下为与代码协作相关的准确信息。

## 工程与构建

- **语言**：Kotlin（主）  
- **构建**：Gradle（Kotlin DSL：`build.gradle.kts`），**不是** Maven；**不是** `just build` 默认路径（若根目录存在 `justfile` 以其实际内容为准）。  
- **常用命令**：`./gradlew :app:assembleDebug`（Windows：`gradlew.bat :app:assembleDebug`）

## 技术栈（摘要）

- Android SDK、View 体系（ViewBinding）、Material、Navigation  
- Room（KSP）、Retrofit + OkHttp、Kotlin Coroutines / Flow  
- 发布页富文本：**自研块编辑器** `BlockEditorRecyclerView`，**未**使用 WebView 整页富文本方案  

## 配置

- **本地 SDK**：`local.properties` 中 `sdk.dir`  
- **DeepSeek API Key**（若使用 AI 润色）：同一文件中的 `DEEPSEEK_API_KEY`，由 `app/build.gradle.kts` 读入 `BuildConfig`（详见 `docs/ai-polish-implementation.md`）

## 文档入口

- **[docs/README.md](docs/README.md)**：文档总索引与阅读顺序  
- **[ARCHITECTURE.md](ARCHITECTURE.md)**：模块与分层概览  

## 协作约束（与 `.cursor/rules` 一致时以规则为准）

- 编辑器相关改动优先保证**行为可解释、可回滚**；避免在 RecyclerView **layout 过程中**直接 `notifyItemChanged`（AI 条刷新已用 `post` 规避）。
