# richtext 模块总览

## 1. 模块目标

`richtext` 是发布页的原生富文本编辑与渲染模块，目标是：

- 提供可维护、可解释的块级编辑能力
- 保持编辑行为稳定且可预测
- 在不引入 WebView/第三方整包编辑器的前提下完成工程化落地

## 2. 目录分层

- `../ai/`（与 `richtext` 同级，包路径 `ui/publish/ai`）：发布页 **AI 正文润色**（ViewModel、流式网络、Prompt），**不**属于 `richtext` 包内层，但与 `BlockEditorRecyclerView` 协作；说明见 **[docs/ai-polish-implementation.md](../../../../../../../../../../docs/ai-polish-implementation.md)**（自本文件向上 10 级至仓库根目录）。
- `core/`：核心编辑容器与输入入口（编辑主流程）
- `data/`：数据层（本地/远程数据源，占位中）
- `business/`：业务编排层
  - `selection/`：焦点与选择态编排
  - `format/`：结构变更与格式化业务语义
  - `link/`：链接业务编排（占位中）
- `ui/`：界面层
  - `ViewHolder/`：各块视图持有者
  - `readOnly/`：只读态配置
  - `actions/`：UI 行为（菜单/弹窗/剪贴板/跳转）
- `model/`：文档与块模型
- `engine/`：富文本引擎能力
  - `link/`、`span/`、`parser/`、`formatter/`
- `video/`：视频播放相关能力
- `utils/`：通用工具
  - `validator/`、`helper/`、`converter/`
- `config/`：样式与行为配置

## 3. 依赖方向约束

建议遵循以下依赖方向（从高层到低层）：

`ui -> business -> core/model/engine/utils/config -> data`

约束原则：

- `business` 不直接依赖具体 `View` 实现
- `engine` 不感知 UI 事件来源
- `core` 负责编辑主链路，不承载页面级业务
- `utils` 只提供无状态通用能力，避免反向依赖上层

## 4. 变更约定

- 优先小步、可回滚变更
- 结构调整不改变既有交互语义
- 包名与目录保持一致
- 关键行为改动（焦点、selection、结构变更语义）需先评审再实施

## 5. 协作建议

- 新增能力先判断归属层，再落文件
- 新目录先补简短 README 再加实现
- 对外共享能力优先走明确接口，避免跨层“直连调用”
