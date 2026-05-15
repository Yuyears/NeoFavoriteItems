# Test Report

# 测试报告

Date: 2026-05-10

日期：2026-05-10

## Purpose

## 用途

- This document records automated test scope, coverage snapshots, build verification, and compile/build evidence for the current delivery.
- 本文档记录当前交付对应的自动化测试范围、覆盖率快照、构建验证结果，以及编译/构建证据。

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
  - `InventorySortingCompatServiceTest`
  - `LockedEmptySlotFallbackTest`
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
  - locked empty slot fallback selection prefers empty unlocked hotbar slots, falls back to main inventory, skips locked targets, and reports no-slot cases
  - 锁定空槽放入回退选择会优先空且未锁定的快捷栏槽，其次主背包槽，并会跳过锁定目标和覆盖无可用槽场景
- AE2 compatibility note:
- AE2 兼容说明：
  - AE2 menu-layer hooks are compile/build verified.
  - AE2 菜单层钩子已通过编译/构建验证。
  - Optional-runtime behavior is not covered by common unit tests; manual evidence belongs in `31-report-verification.md`.
  - 可选运行时行为不在 common 单元测试覆盖范围内；手工验证证据归入 `31-report-verification.md`。
- JustDireThings compatibility note:
- JustDireThings 兼容说明：
  - Forge/NeoForge item-handler wrapper guards now leave read APIs transparent and protect only mutation APIs.
  - Forge/NeoForge item-handler 包装器守卫现在保持读取 API 透明，只保护变更 API。
  - Forge/NeoForge slot resolvers now recognize player inventory `SlotItemHandler(InvWrapper/RangedWrapper)` slots for overlay rendering and early click guards.
  - Forge/NeoForge 槽位解析器现在会识别玩家背包 `SlotItemHandler(InvWrapper/RangedWrapper)` 槽位，以支持 Overlay 渲染和提前点击拦截。
  - These paths are compile-verified.
  - 这些路径已通过编译验证。
- Sorting compatibility note:
- 整理兼容说明：
  - NeoForge Quark sorting compatibility and Inventory Tweaks ReFoxed player-sort compatibility both use the common favorite-slot merge helper covered by `InventorySortingCompatServiceTest`.
  - NeoForge Quark 排序兼容与 Inventory Tweaks ReFoxed 玩家背包整理兼容都复用 common 收藏锁槽合并 helper，并由 `InventorySortingCompatServiceTest` 覆盖。
  - NeoForge Quark hotbar changer compatibility is covered by `InventorySortingCompatServiceTest` for favorite-state swaps between hotbar and main-inventory rows.
  - NeoForge Quark 快捷栏切换兼容已由 `InventorySortingCompatServiceTest` 覆盖快捷栏与主背包行之间的收藏状态交换。
  - Optional sorter runtime dependencies are not part of common unit tests; manual evidence belongs in `31-report-verification.md`.
  - 可选整理模组运行时依赖不属于 common 单元测试范围；手工验证证据归入 `31-report-verification.md`。
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
  - NeoForge locked-slot GUI protection uses official `ScreenEvent.MouseButtonPressed.Pre` and `ScreenEvent.MouseButtonReleased.Pre`; release guarding covers creative inventory cursor placement into locked empty player slots.
  - NeoForge 锁槽 GUI 保护使用官方 `ScreenEvent.MouseButtonPressed.Pre` 与 `ScreenEvent.MouseButtonReleased.Pre`；释放阶段守卫覆盖创造物品栏中光标物品放入锁定空玩家槽的路径。
  - Server-side Sophisticated menu clicks now distinguish storage-owned slots from the embedded 36 player-inventory slots before resolving item-handler-backed player indices, preventing memory/storage slots from being pseudo-locked by matching player inventory index numbers.
  - 服务端 Sophisticated 菜单点击现在会先区分存储自身槽与内嵌的 36 个玩家背包槽，再解析 item-handler-backed 玩家索引，避免记忆/存储槽因为槽号碰巧对应玩家背包索引而出现伪锁定。
  - NeoForge has a low-level `MouseHandler` press entry for modpack cases where Sophisticated empty-slot clicks do not reach `ScreenEvent` or screen `mouseClicked`. When Mouse Tweaks is present, this entry primes/toggles the press target without canceling the raw press so Mouse Tweaks drag initialization is preserved.
  - NeoForge 针对整合包中 Sophisticated 空槽点击到不了 `ScreenEvent` 或界面 `mouseClicked` 的情况提供低层 `MouseHandler` 按下入口。存在 Mouse Tweaks 时，该入口只预激活/切换按下目标，不取消原始按下，因此保留 Mouse Tweaks 拖动初始化。
  - Current runtime limitation: Sophisticated empty-slot single-click toggling works through the low-level press path, but empty-slot drag marking is not guaranteed when Mouse Tweaks does not emit drag-enter samples for those empty slots.
  - 当前运行限制：Sophisticated 空槽单击切换可通过低层按下路径工作，但如果 Mouse Tweaks 不为这些空槽发出拖动进入采样，空槽拖动标记不作保证。
  - This client mixin path is compile-verified.
  - 该客户端 Mixin 路径已通过编译验证。
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
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`: passed after locked-empty-slot fallback routing tests were added.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`：锁定空槽回退路由测试加入后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`: passed after adding the three-loader `Player.setItemInHand` reroute mixins.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`：加入三端 `Player.setItemInHand` 回退 Mixin 后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`: passed after the locked-empty-slot fallback implementation and documentation update.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`：锁定空槽回退实现与文档更新后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test :fabric:compileJava :forge:compileJava :neoforge:compileJava`: passed after changing lock/bypass key polling to read the current key binding instead of relying on reflective fallback paths.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test :fabric:compileJava :forge:compileJava :neoforge:compileJava`：锁定键/旁路键轮询改为读取当前按键绑定后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test :fabric:compileJava :forge:compileJava :neoforge:compileJava`: passed after separating direct-write fallback from GUI cursor/Slot placement guards.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test :fabric:compileJava :forge:compileJava :neoforge:compileJava`：直接写入回退与 GUI 光标/Slot 放入守卫分离后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test :neoforge:compileJava`: passed after adding NeoForge official mouse-release guarding for creative cursor placement.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test :neoforge:compileJava`：为 NeoForge 创造物品栏光标放入补充官方鼠标释放守卫后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`: passed after the NeoForge mouse-release guard and documentation update.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`：NeoForge 鼠标释放守卫与文档更新后通过。
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
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`: passed after the Sophisticated server menu pseudo-lock fix.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`：Sophisticated 服务端菜单伪锁定修复后通过。
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`: passed after the Sophisticated server menu pseudo-lock fix.
  - `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`：Sophisticated 服务端菜单伪锁定修复后通过。

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

## Automated Persistence Coverage

## 自动化持久化覆盖

- Client-only storage resolves to `favoriteitems/<sanitized-server-address>/players/<uuid>.dat`.
- 单端客户端存储路径解析为 `favoriteitems/<净化后的服务器地址>/players/<uuid>.dat`。
- Dual-install storage resolves to `<world>/data/neo_favorite_items/players/<uuid>.dat`.
- 双端安装存储路径解析为 `<世界目录>/data/neo_favorite_items/players/<uuid>.dat`。
- Legacy `itemfavorites/...` data is migrated into the new directory and the old file is removed after a successful read.
- 旧 `itemfavorites/...` 数据会在成功读取后迁移到新目录，并删除旧文件。
- Player-login loads populate the cache, in-play cache updates avoid immediate file writes, and full-save flush preserves player favorite sets.
- 玩家登录读取会填充缓存，游戏过程缓存更新不会立即写文件，完整保存回路能够保持玩家收藏状态。

## Runtime Validation Boundary

## 运行时验证边界

- Runtime/manual validation results are tracked in `31-report-verification.md`.
- 运行时/手工验证结果统一记录在 `31-report-verification.md`。
- This test report may mention runtime gaps only to explain why a behavior is not covered by automated tests.
- 本测试报告只在解释自动化测试无法覆盖某行为时提及运行时缺口。

## Notes

## 说明

- This document covers automated verification in the current workspace. Runtime validation status is maintained in `31-report-verification.md`.
- 本文档覆盖当前工作区内的自动化验证。运行时验证状态维护在 `31-report-verification.md`。
