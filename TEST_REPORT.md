# Test Report

# 测试报告

Date: 2026-04-26

日期：2026-04-26

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
- Existing persistence coverage:
- 既有持久化覆盖点：
  - client-only storage namespace by server address
  - 单端客户端按服务器地址分存储目录
  - dual-install world-save path selection
  - 双端安装时的世界存档路径选择
  - legacy `itemfavorites` fallback loading and migration cleanup
  - 旧 `itemfavorites` 数据的回退读取与迁移清理
  - cached preload/full-save round trip
  - 缓存预载与完整保存回路
  - client storage namespace selection from server-list address, remote connection address, and default fallback
  - 客户端存储命名空间按服务器列表地址、远端连接地址和默认回退的选择逻辑

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
- Verified that preload plus full-save cache flow preserves player favorite sets
- 已验证预载加完整保存的缓存流程能够保持玩家收藏状态

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
