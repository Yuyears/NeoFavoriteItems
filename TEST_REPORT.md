# Test Report

# 测试报告

Date: 2026-05-05

日期：2026-05-05

## Purpose

## 用途

- This document records the automated test scope, coverage snapshot, build verification, and persistence-specific validation points for the current delivery.
- 本文档记录当前交付对应的自动化测试范围、覆盖率快照、构建验证结果，以及与持久化相关的专项验证点。

## Automated Tests

## 自动化测试

- Command: `.\gradle.bat --no-daemon --no-build-cache :common:test`
- 命令：`.\gradle.bat --no-daemon --no-build-cache :common:test`
- Result: passed
- 结果：通过
- Scope:
- 覆盖范围：
  - `FavoritesManagerTest`
  - `ClientFavoriteSyncServiceTest`
  - `ClientDropGuardTest`
  - `InteractionGuardServiceTest`
  - `ConfigManagerTest`
  - `DataPersistenceManagerTest`
  - `ReflectionHelperTest`
  - `PlatformFavoriteSupportTest`
  - `QuarkSortingCompatServiceTest`
- Interaction coverage additions in this round:
- 本轮新增的交互覆盖点：
  - locked empty offhand rejects incoming GUI/GUI-outside swap targets
  - 锁定空副手槽拒绝 GUI 内外副手交换放入
  - locked empty armor slots reject quick-move equipment targets
  - 锁定空护甲槽拒绝 Shift 点击装备放入目标
  - locked slots reject quick-move source removal
  - 锁定槽作为快速移动来源时拒绝取出
  - bypass key still allows incoming items when configured
  - 按配置启用旁路键时，旁路键仍可放行放入和来源取出行为
- AE2 compatibility note:
- AE2 兼容说明：
  - AE2 menu-layer hooks are compile/build verified. NeoForge in-game validation has covered terminal space-left-click `MOVE_REGION` into and out of locked player inventory slots.
  - AE2 菜单层钩子已通过编译/构建验证。NeoForge 实机验证已覆盖终端空格+左键 `MOVE_REGION` 对锁定玩家背包槽的放入与取出。
  - Fabric and Forge AE2 runtime behavior still require manual validation because AE2 is an optional runtime dependency and terminal behavior is not covered by common unit tests.
  - Fabric、Forge 的 AE2 运行时行为仍需手工验证，因为 AE2 是可选运行时依赖，且具体终端行为不在 common 单元测试覆盖范围内。
- JustDireThings compatibility note:
- JustDireThings 兼容说明：
  - Forge/NeoForge item-handler wrapper guards now leave read APIs transparent and protect only mutation APIs.
  - Forge/NeoForge item-handler 包装器守卫现在保持读取 API 透明，只保护变更 API。
  - Forge/NeoForge slot resolvers now recognize player inventory `SlotItemHandler(InvWrapper/RangedWrapper)` slots for overlay rendering and early click guards.
  - Forge/NeoForge 槽位解析器现在会识别玩家背包 `SlotItemHandler(InvWrapper/RangedWrapper)` 槽位，以支持 Overlay 渲染和提前点击拦截。
  - These paths are compile-verified; in-game JDT screen validation is still recommended.
  - 这些路径已通过编译验证，仍建议进行 JDT 界面实机复测。
- Quark compatibility note:
- Quark 兼容说明：
  - NeoForge Quark sorting compatibility is compile-verified and the common locked-slot merge helper is covered by `QuarkSortingCompatServiceTest`.
  - NeoForge Quark 排序兼容已通过编译验证，common 锁槽合并 helper 已由 `QuarkSortingCompatServiceTest` 覆盖。
  - NeoForge Quark hotbar changer compatibility is covered by `QuarkSortingCompatServiceTest` for favorite-state swaps between hotbar and main-inventory rows.
  - NeoForge Quark 快捷栏切换兼容已由 `QuarkSortingCompatServiceTest` 覆盖快捷栏与主背包行之间的收藏状态交换。
  - In-game NeoForge + Quark sorting and hotbar-changer validation is still recommended because the optional Quark runtime dependency is not part of common unit tests.
  - 仍建议进行 NeoForge + Quark 排序和快捷栏切换实机验证，因为可选 Quark 运行时依赖不属于 common 单元测试范围。
- Sophisticated Backpacks compatibility note:
- Sophisticated Backpacks 兼容说明：
  - NeoForge Sophisticated Backpacks screens use a dedicated `StorageScreenBase.findSlot(x, y)` container mouse state machine, including a coordinate fallback for player-inventory slots when the backpack screen does not expose empty slots through its own lookup.
  - NeoForge Sophisticated Backpacks 界面使用专用的 `StorageScreenBase.findSlot(x, y)` 容器鼠标状态机；当背包界面自身查找不暴露空玩家槽时，会回退到玩家背包槽坐标解析。
  - Vanilla and modded container screens now fall back through screen `findSlot(x, y)`, `hoveredSlot`, and the legacy coordinate scan. This is compile-verified for Quark/ModernUI-style inventory screen rewrites where raw `leftPos/topPos` coordinates may not identify the slot.
  - 原版和被 Mod 改写的容器界面现在会依次回退到界面 `findSlot(x, y)`、`hoveredSlot` 和旧坐标扫描。该路径已通过编译验证，用于 Quark/ModernUI 这类可能让原始 `leftPos/topPos` 坐标无法命中槽位的玩家背包改写场景。
  - Lock-operation `mouseClicked`/`mouseReleased` state machines are the base toggle path on Fabric, Forge, and NeoForge: the physical Alt-left-click toggles once, release resets state, and `mouseDragged` only consumes leaked vanilla drag while active.
  - Fabric、Forge、NeoForge 的锁定操作以 `mouseClicked`/`mouseReleased` 状态机作为基础 toggle 路径：物理 Alt+左键只切换一次，释放只重置状态，`mouseDragged` 只在 active 时消费漏进来的原版拖动。
  - Mouse Tweaks compatibility on all three loaders borrows Mouse Tweaks' refreshed slot-enter detection and routes newly entered slots into the loader toggle handler, without invoking Mouse Tweaks' own click path.
  - 三个平台的 Mouse Tweaks 兼容都会借用 Mouse Tweaks 刷新后的槽位进入检测，并把新进入的槽位路由到对应加载器的切换 handler，不调用 Mouse Tweaks 自己的点击路径。
  - `StorageScreenBase.slotClicked` is now hooked directly on NeoForge because SophisticatedCore overrides the vanilla `AbstractContainerScreen.slotClicked` method.
  - NeoForge 现在会直接钩住 `StorageScreenBase.slotClicked`，因为 SophisticatedCore 覆盖了原版 `AbstractContainerScreen.slotClicked` 方法。
  - `StorageScreenBase.slotClicked` and vanilla `AbstractContainerScreen.slotClicked` only cancel leaked `PICKUP`/`QUICK_MOVE` inventory actions while Alt is held; they do not toggle state.
  - 按住 Alt 时，`StorageScreenBase.slotClicked` 与原版 `AbstractContainerScreen.slotClicked` 只会取消漏进来的 `PICKUP`/`QUICK_MOVE` 库存动作，不会切换状态。
  - NeoForge has a low-level `MouseHandler` press entry for modpack cases where Sophisticated empty-slot clicks do not reach `ScreenEvent` or screen `mouseClicked`. When Mouse Tweaks is present, this entry primes/toggles the press target without canceling the raw press so Mouse Tweaks drag initialization is preserved.
  - NeoForge 针对整合包中 Sophisticated 空槽点击到不了 `ScreenEvent` 或界面 `mouseClicked` 的情况提供低层 `MouseHandler` 按下入口。存在 Mouse Tweaks 时，该入口只预激活/切换按下目标，不取消原始按下，因此保留 Mouse Tweaks 拖动初始化。
  - Current runtime limitation: Sophisticated empty-slot single-click toggling works through the low-level press path, but empty-slot drag marking is not guaranteed when Mouse Tweaks does not emit drag-enter samples for those empty slots.
  - 当前运行限制：Sophisticated 空槽单击切换可通过低层按下路径工作，但如果 Mouse Tweaks 不为这些空槽发出拖动进入采样，空槽拖动标记不作保证。
  - This client mixin path is compile-verified; in-game NeoForge + Sophisticated Backpacks validation is still recommended with the user modpack.
  - 该客户端 Mixin 路径已通过编译验证；仍建议在用户整合包中进行 NeoForge + Sophisticated Backpacks 实机复测。
- Overlay rendering note:
- Overlay 渲染说明：
  - `ConfigManagerTest` verifies that the removed `renderForegroundContrastBackdrop` option is no longer generated and is stripped during config repair.
  - `ConfigManagerTest` 验证已移除的 `renderForegroundContrastBackdrop` 配置项不再生成，并会在配置修复时被清理。
  - Fabric, Forge, and NeoForge renderers now consume common `OverlayRenderDescriptor` decisions and are compile-verified.
  - Fabric、Forge、NeoForge 渲染器现在消费 common `OverlayRenderDescriptor` 决策，并已通过编译验证。
  - ModernUI source review shows rounded tooltip shadow is produced by its tooltip shader/render state, so the low-alpha contrast backing was removed instead of kept as an ineffective workaround.
  - ModernUI 源码检查表明圆角 tooltip 阴影由 tooltip shader/render state 生成，因此已删除低透明度对比底，不保留这个无效绕路方案。
- Existing persistence coverage:
- 既有持久化覆盖点：
  - client-only storage namespace by server address
  - 单端客户端按服务器地址分存储目录
  - dual-install world-save path selection
  - 双端安装时的世界存档路径选择
  - legacy `itemfavorites` fallback loading and migration cleanup
  - 旧 `itemfavorites` 数据的回退读取与迁移清理
  - per-player cache update without immediate file writes, plus final full-save flush
  - 按玩家更新缓存但不立即写文件，以及最终完整保存回路
  - client storage namespace selection from server-list address, remote connection address, and default fallback
  - 客户端存储命名空间按服务器列表地址、远端连接地址和默认回退的选择逻辑
  - migration of server-authoritative files from the old game-directory `data/neo_favorite_items` root into the active world directory, including removal of the old file after successful migration
  - 将旧游戏根目录 `data/neo_favorite_items` 下的服务端权威文件迁移到当前世界目录，并验证成功迁移后删除旧文件
  - clearing server data also removes old game-directory server files to avoid future duplicate reads
  - 清理服务端数据时也会删除旧游戏根目录服务端文件，避免后续重复读取

## Coverage

## 覆盖率

- Report command: `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:jacocoTestReport`
- 报告命令：`.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:jacocoTestReport`
- JaCoCo report path: `common/build/reports/jacoco/test/html/index.html`
- JaCoCo 报告路径：`common/build/reports/jacoco/test/html/index.html`
- Coverage snapshot:
- 覆盖率快照：
  - Instruction: 1982 covered / 4896 total (`40.48%`)
  - 指令覆盖：1982 / 4896（`40.48%`）
  - Branch: 110 covered / 467 total (`23.55%`)
  - 分支覆盖：110 / 467（`23.55%`）
  - Line: 471 covered / 1102 total (`42.74%`)
  - 行覆盖：471 / 1102（`42.74%`）
  - Method: 112 covered / 227 total (`49.34%`)
  - 方法覆盖：112 / 227（`49.34%`）
  - Class: 24 covered / 29 total (`82.76%`)
  - 类覆盖：24 / 29（`82.76%`）

## Build Verification

## 构建验证

- Latest local verification:
- 最新本地验证：
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`: passed
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`：通过
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :neoforge:compileJava`: passed
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :neoforge:compileJava`：通过
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :neoforge:processResources`: passed after rerunning outside the sandbox because the first sandboxed run could not initialize Gradle's Windows native-platform library.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :neoforge:processResources`：通过；首次沙盒内运行无法初始化 Gradle 的 Windows native-platform 库，沙盒外重跑后成功。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`: passed
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`：通过
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`: passed after the three-loader Mouse Tweaks/lock-click synchronization changes.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`：三端 Mouse Tweaks/锁定点击同步修改后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`: passed after synchronizing Fabric, Forge, and NeoForge lock-operation click architecture.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`：同步 Fabric、Forge、NeoForge 锁定操作点击架构后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`: passed after the three-loader click architecture and documentation updates.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`：三端点击架构与文档更新后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :neoforge:compileJava`: passed after NeoForge state-machine naming cleanup.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :neoforge:compileJava`：NeoForge 状态机命名整理后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`: passed after NeoForge state-machine naming cleanup.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`：NeoForge 状态机命名整理后通过。

- Command: `.\gradle.bat --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`
- 命令：`.\gradle.bat --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`
- Result: passed
- 结果：通过
- Scope includes conditional AE2 compatibility mixins for Fabric, Forge, and NeoForge.
- 范围包含 Fabric、Forge、NeoForge 三端条件加载的 AE2 兼容 Mixin。
- NeoForge resource metadata now registers `neo_favorite_items.neoforge.compat.mixins.json`.
- NeoForge 资源元数据现在会注册 `neo_favorite_items.neoforge.compat.mixins.json`。
- Integration command: `.\gradle.bat --no-daemon --no-build-cache -Pskip_build_number_increment=true build`
- 集成命令：`.\gradle.bat --no-daemon --no-build-cache -Pskip_build_number_increment=true build`
- Result: passed
- 结果：通过

## Persistence Validation Focus

## 持久化专项验证

- Verified that client-only storage resolves to `favoriteitems/<sanitized-server-address>/players/<uuid>.dat`
- 已验证单端客户端存储路径为 `favoriteitems/<净化后的服务器地址>/players/<uuid>.dat`
- Verified that dual-install storage resolves to `<world>/data/neo_favorite_items/players/<uuid>.dat`
- 已验证双端安装存储路径为 `<世界目录>/data/neo_favorite_items/players/<uuid>.dat`
- Verified that legacy `itemfavorites/...` data is migrated into the new directory and the old file is removed after a successful read
- 已验证旧 `itemfavorites/...` 数据在成功读取后会迁移到新目录，并删除旧文件
- Verified that player-login loads populate the cache, in-play cache updates avoid immediate file writes, and full-save flush preserves player favorite sets
- 已验证玩家登录读取会填充缓存，游戏过程缓存更新不会立即写文件，完整保存回路能够保持玩家收藏状态

## Manual Runtime Validation

## 手工运行验证

- NeoForge dedicated-server installation modes passed:
- NeoForge 真实专用服务端安装模式已通过：
  - client-only
  - 仅客户端安装
  - server-only
  - 仅服务端安装
  - both-sides-installed
  - 双端均安装
- NeoForge high-risk interaction checks passed:
- NeoForge 高风险交互检查已通过：
  - normal drop and bypass-key drop behavior
  - 普通丢弃与按住旁路键时的行为
  - GUI and GUI-outside offhand swap with locked empty and non-empty offhand slots
  - GUI 内外副手交换，覆盖锁定空副手槽和锁定非空副手槽
  - shift-click equippable armor into locked empty armor slots
  - 锁定空护甲槽 Shift 点击可装备护甲
  - AE2 terminal space-left-click `MOVE_REGION` into and out of locked player inventory slots
  - AE2 终端空格+左键 `MOVE_REGION` 对锁定玩家背包槽的放入与取出

## Notes

## 说明

- These results cover automated verification in the current workspace plus the NeoForge manual runtime checks listed above. Fabric and Forge still need in-game multiplayer matrix testing.
- 以上结果覆盖当前工作区内的自动化验证，以及上方列出的 NeoForge 手工运行验证。Fabric 和 Forge 仍需实际游戏中的多人联机矩阵测试。
