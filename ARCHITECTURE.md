# FishMemory 项目结构说明

本文档描述应用整体分层与模块划分，便于维护与面试展示。各功能模块的详细说明见对应子文档。

---

## 一、分层与包结构

| 层级 | 包路径 | 职责 |
|------|--------|------|
| **UI** | `com.fishmemory.app.ui.*` | Activity / Fragment / Adapter / ViewHolder，以及纯 View 逻辑；不直接访问网络或数据库。 |
| **数据** | `com.fishmemory.app.data.*` | 数据模型（DTO / 领域模型）、Repository、Room 实体与 Dao。 |
| **基础设施** | `com.fishmemory.app.core.*` | 网络（NetworkClient、PostApi）、工具类（utils、utils.view）。 |
| **共享** | `com.fishmemory.app.shared.viewmodel` | 跨多个页面的 ViewModel（如 SharedArticleViewModel）。 |
| **公共 UI** | `com.fishmemory.app.ui.common` | 自定义 View（如 BottomNavWithFab、CircleView）。 |

- **UI 层** 通过 ViewModel 获取数据与状态，通过 Repository 暴露的接口访问持久化与网络，不直接依赖 Dao 或 NetworkClient 实现细节。
- **数据层** 中：网络 DTO（如 `ArticleResponse`、`DmItArticle`）与 Room 实体（如 `ArticleEntity`）分离；列表/详情使用的领域模型为 `ArticleData` 等，可来自 DTO 转换或与本地收藏状态合并。

---

## 二、入口与主导航

- **`App.kt`**：初始化 Room `AppDatabase`、`ArticleRepository`（后续可改为依赖注入容器）。
- **`MainActivity`**：单 Activity 容器，承载 `NavHostFragment`（Home / 热榜 / 消息 / 我）与底部 Tab、FAB；底部选中态由 `updateTabSelection(index)` 统一维护。
- **底部导航**：Tab 点击与 Navigation 绑定，FAB 跳转发表页；自定义背景为 `ui.common.BottomNavWithFab`。

---

## 三、功能模块概览

### 首页（Home）

- **`ui.home.HomeFragment`**：搜索栏、分类 Tab、ViewPager；搜索/分类/滚动动画逻辑集中在本 Fragment。
- **`ui.home.category.CategoryPageFragment`**：单个分类下的文章列表，依赖 `SharedArticleViewModel` 与 `ArticleRepository` 拉取数据与收藏状态。
- **`ui.home.ArticleAdapter`**：列表项绑定与点赞/收藏点击回调；点赞/收藏状态通过 `bindLikeState` / `bindCollectState` 封装。

### 文章详情

- **`ui.articledetail.ArticleDetailActivity`**：标题、作者、WebView 正文，点赞/收藏/分享；WebView 配置在 `setupWebView()`。
- **`ui.articledetail.ArticleDetailViewModel`**：收藏/点赞状态与持久化，通过 `ArticleRepository` 完成；Activity 仅订阅状态与转发点击。

### 发表与富文本

- **富文本编辑与展示**：见 **[发表模块文档](app/src/main/java/com/fishmemory/app/ui/publish/PUBLISH_MODULE.md)**。  
- 编辑使用 `RichEditorView` + `RichDocument`；只读展示使用 `RichContentAdapter` + ViewHolder。

### 其他

- **热榜**：`ui.community.HotRankingFragment`。
- **消息**：`ui.message.MessageFragment` + `MessageViewModel`。
- **我的**：`ui.mine.MyFragment`。

---

## 四、数据与网络

- **`data.repository.ArticleRepository`**：文章收藏状态与 Room 持久化的统一入口；网络列表拉取当前由调用方经 `PostApi` 完成，后续可并入 Repository。
- **`core.network.NetworkClient`**：Retrofit + OkHttp 单例，所有网络请求通过 `NetworkClient.postApi`。
- **`core.api.PostApi`**：接口定义（如 getItNews）。

---

## 五、工具与扩展

- **`core.utils.view`**：View/Window 相关扩展（如 `clickFeedback`、`vibrate`、`setStatusBarIconsBlack`），使用处：MainActivity、ArticleAdapter 等。
- **`core.utils`**：通用工具（如防抖占位 `Debounce`）。

---

## 六、文档索引

| 文档 | 说明 |
|------|------|
| [PUBLISH_MODULE.md](app/src/main/java/com/fishmemory/app/ui/publish/PUBLISH_MODULE.md) | 发表页与富文本编辑器（RichDocument、RichEditorView、粘贴与滚动策略）。 |
| 本文档 ARCHITECTURE.md | 整体分层、包结构、模块入口与数据流。 |

代码已就绪，审阅确认后可按需合入与扩展。
