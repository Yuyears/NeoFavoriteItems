# Neo Favorite Items TODO

Last updated: 2026-04-23

最后更新：2026-04-23

## Completed

## 已完成

- Multi-module Gradle Kotlin DSL project structure: `common`, `fabric`, `forge`, `neoforge`
- 多模块 Gradle Kotlin DSL 项目结构：`common`、`fabric`、`forge`、`neoforge`
- Minecraft 1.21.1 / Java 21 / Architectury Loom build foundation
- Minecraft 1.21.1 / Java 21 / Architectury Loom 构建基础
- Unified logical slot model: `LogicalSlotIndex`
- 统一逻辑槽位模型：`LogicalSlotIndex`
- Config system with English and Simplified Chinese comments
- 配置系统和中英文配置注释
- Favorite state management and basic persistence
- 收藏状态管理和基础持久化
- Fabric, Forge, and NeoForge entrypoints with client key binding registration
- Fabric、Forge、NeoForge 入口与客户端按键注册
- Overlay render entrypoints and hotbar HUD overlays for all three loaders
- 三个平台 Overlay 渲染入口和快捷栏 HUD Overlay
- Nine PNG overlay textures and configurable color rendering
- 九种 Overlay PNG 材质和配置色渲染
- Unified interaction decisions through `InteractionGuardService`
- `InteractionGuardService` 统一交互决策
- Client polling for lock-operation and bypass-key states
- 客户端锁定操作键、旁路键状态轮询
- Server-side toggle handling, revisions, full/incremental sync, and bypass-key state sync
- 服务端收藏切换、修订号、全量/增量同步和旁路键状态同步
- Player login/logout load-save flow for all three loaders
- 三个平台玩家登录/登出加载保存流程

## Remaining Work

## 待完成

- Improve item movement tracking while the bypass key is held, and define complex-move behavior for `FOLLOW_ITEM` and `STAY_AT_POSITION`.
- 完善旁路键下的物品移动追踪，明确 `FOLLOW_ITEM` 与 `STAY_AT_POSITION` 在复杂移动中的行为。
- Implement actual visual feedback and sound feedback triggers.
- 补齐视觉反馈和音效反馈的实际触发逻辑。
- Add automated tests for slot mapping, config parsing, serialization compatibility, interaction decisions, and sync revisions.
- 为槽位映射、配置解析、序列化兼容、交互决策和同步修订号添加自动化测试。
- Continue validating behavior consistency across Fabric, Forge, and NeoForge in creative mode, normal containers, hotbar swaps, dragging, and external item transfers.
- 继续实测 Fabric、Forge、NeoForge 在创造模式、普通容器、快捷栏交换、拖拽和外部物品转移中的一致性。
- Evaluate whether config hot reload or an in-game config screen is needed.
- 评估是否需要配置热重载或游戏内配置界面。
- Clean up unused helper methods and repeated platform-layer code.
- 清理未使用的 helper 方法和平台层重复代码。

## Known Limitations

## 已知限制

- Favorite slots currently only cover player inventory slots; chest, furnace, and other container-owned slots are not supported.
- 当前只支持玩家物品栏槽位收藏，不支持箱子、熔炉等容器自身槽位收藏。
- Server-authoritative sync exists, but it still needs more real multiplayer validation.
- 服务端权威同步已存在，但仍需要更多多人实际场景验证。
- `FavoritesManager` is still a singleton state manager; it can later evolve into clearer state service/repository interfaces.
- `FavoritesManager` 仍是单例状态管理器，后续可继续演进为更清晰的状态服务/仓库接口。
- No automated test suite is established yet; validation currently relies mainly on compilation and manual runtime checks.
- 自动化测试体系尚未建立，当前主要依赖编译和手动运行验证。
