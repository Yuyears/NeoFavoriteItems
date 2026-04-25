# Neo Favorite Items TODO

Last updated: 2026-04-24

最后更新：2026-04-24

## Checklist Summary

## 勾选简表

- [ ] P0: Validate Mouse Tweaks drag-click lock toggling on Fabric, Forge, and NeoForge.
- [ ] P0：实机验证 Fabric、Forge、NeoForge 的 Mouse Tweaks 拖动点击切换锁定。
- [ ] P0: Validate behavior consistency in creative mode, normal containers, hotbar swaps, dragging, and external item transfers.
- [ ] P0：实测创造模式、普通容器、快捷栏交换、拖拽和外部物品转移的一致性。
- [ ] P0: Validate multiplayer installation modes: client-only, server-only, and both-sides-installed.
- [ ] P0：验证多人安装模式：仅客户端、仅服务端、双端均安装。
- [ ] P1: Add automated tests for slot mapping, config parsing, serialization compatibility, interaction decisions, and sync revisions.
- [ ] P1：为槽位映射、配置解析、序列化兼容、交互决策和同步修订号添加自动化测试。
- [ ] P1: Improve bypass-key item movement tracking and define complex-move behavior.
- [ ] P1：完善旁路键下的物品移动追踪，并明确复杂移动行为。
- [ ] P2: Add user-provided custom overlay textures and texture color modes.
- [ ] P2：支持用户自定义 Overlay 纹理和纹理颜色模式。
- [ ] P2: Implement visual and sound feedback triggers.
- [ ] P2：补齐视觉反馈和音效反馈触发。
- [ ] P2: Evaluate config hot reload or an in-game config screen.
- [ ] P2：评估配置热重载或游戏内配置界面。
- [ ] P3: Clean up unused helpers and repeated platform-layer code.
- [ ] P3：清理未使用 helper 和平台层重复代码。

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
- Mouse Tweaks-style drag-click lock toggling through client `slotClicked` handling on Fabric, Forge, and NeoForge
- 通过 Fabric、Forge、NeoForge 客户端 `slotClicked` 处理支持 Mouse Tweaks 风格拖动点击切换锁定

## Priority Roadmap

## 优先级路线图

### P0 Validation And Correctness

### P0 验证与正确性

- In-game validation of Mouse Tweaks drag-click lock toggling on all three loaders.
- 在三个加载器中实机验证 Mouse Tweaks 拖动点击切换锁定。
- Continue validating behavior consistency across Fabric, Forge, and NeoForge in creative mode, normal containers, hotbar swaps, dragging, and external item transfers.
- 继续实测 Fabric、Forge、NeoForge 在创造模式、普通容器、快捷栏交换、拖拽和外部物品转移中的一致性。
- Validate multiplayer installation modes: client-only, server-only, and both-sides-installed.
- 验证多人安装模式：仅客户端、仅服务端、双端均安装。

### P1 Test Coverage And Core Semantics

### P1 测试覆盖与核心语义

- Add automated tests for slot mapping, config parsing, serialization compatibility, interaction decisions, and sync revisions.
- 为槽位映射、配置解析、序列化兼容、交互决策和同步修订号添加自动化测试。
- Improve item movement tracking while the bypass key is held, and define complex-move behavior for `FOLLOW_ITEM` and `STAY_AT_POSITION`.
- 完善旁路键下的物品移动追踪，明确 `FOLLOW_ITEM` 与 `STAY_AT_POSITION` 在复杂移动中的行为。

### P2 User-Facing Features

### P2 用户可见功能

- Add user-provided custom overlay textures from a dedicated game-directory folder, with config entries for locked/highlight texture selection and texture color mode.
- 支持用户在游戏目录下的专用文件夹提供自定义 Overlay 纹理，并提供已锁定/高亮纹理选择和纹理颜色模式配置。
- Custom texture color modes should include `LUMINANCE` for config-color tinting and `COLOR` for using the texture's original color without configurable recoloring.
- 自定义纹理颜色模式应包含 `LUMINANCE`（按配置色染色）和 `COLOR`（使用纹理原色，不再配置改色）。
- Implement actual visual feedback and sound feedback triggers.
- 补齐视觉反馈和音效反馈的实际触发逻辑。
- Evaluate whether config hot reload or an in-game config screen is needed.
- 评估是否需要配置热重载或游戏内配置界面。

### P3 Cleanup

### P3 清理

- Clean up unused helper methods and repeated platform-layer code.
- 清理未使用的 helper 方法和平台层重复代码。

## Long-Term Maintenance

## 长期维护

- Gradually modernize the overlay rendering pipeline during related updates instead of doing one large rewrite.
- 在后续相关更新中渐进式现代化 Overlay 渲染管线，避免一次性大规模重写。
- Move repeated style selection, texture metadata, sampling decisions, and render descriptors into loader-neutral common abstractions where practical.
- 在可行时，将重复的样式选择、纹理元数据、采样决策和渲染描述逐步迁移到平台无关的 common 抽象。
- Keep platform renderers focused on loader-specific `GuiGraphics` calls, HUD/screen hooks, and final draw submission.
- 让平台渲染器逐步聚焦于加载器特有的 `GuiGraphics` 调用、HUD/Screen 事件入口和最终绘制提交。

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

## Request Buffer

## 临时需求缓冲区

- Add new ad-hoc ideas here first, then promote them into the priority roadmap after review.
- 新的临时想法先放在这里，评审后再提升到优先级路线图。
