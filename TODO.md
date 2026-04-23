# Neo Favorite Items TODO

最后更新: 2026-04-23

## 已完成

- 多模块 Gradle Kotlin DSL 项目结构: `common`、`fabric`、`forge`、`neoforge`
- Minecraft 1.21.1 / Java 21 / Architectury Loom 构建基础
- 统一逻辑槽位模型 `LogicalSlotIndex`
- 配置系统和中英文配置注释
- 收藏状态管理和基础持久化
- Fabric、Forge、NeoForge 入口与客户端按键注册
- 三个平台 Overlay 渲染入口和快捷栏 HUD Overlay
- 九种 Overlay PNG 材质和配置色渲染
- `InteractionGuardService` 统一交互决策
- 客户端锁定操作键、旁路键状态轮询
- 服务端收藏切换、修订号、全量/增量同步和旁路键状态同步
- 三个平台玩家登录/登出加载保存流程

## 待完成

- 完善旁路键下的物品移动追踪，明确 `FOLLOW_ITEM` 与 `STAY_AT_POSITION` 在复杂移动中的行为。
- 补齐视觉反馈和音效反馈的实际触发逻辑。
- 为槽位映射、配置解析、序列化兼容、交互决策和同步修订号添加自动化测试。
- 继续实测 Fabric、Forge、NeoForge 在创造模式、普通容器、快捷栏交换、拖拽和外部物品转移中的一致性。
- 评估是否需要配置热重载或游戏内配置界面。
- 清理未使用的 helper 方法和平台层重复代码。

## 已知限制

- 当前只支持玩家物品栏槽位收藏，不支持箱子、熔炉等容器自身槽位收藏。
- 服务端权威同步已存在，但仍需要更多多人实际场景验证。
- `FavoritesManager` 仍是单例状态管理器，后续可继续演进为更清晰的状态服务/仓库接口。
- 自动化测试体系尚未建立，当前主要依赖编译和手动运行验证。
