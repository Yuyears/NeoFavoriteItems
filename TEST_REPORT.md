# Test Report

# 测试报告

Date: 2026-04-25

日期：2026-04-25

## Purpose

## 用途

- This document records the automated test scope, coverage snapshot, build verification, and persistence-specific validation points for the current delivery.
- 本文档记录当前交付对应的自动化测试范围、覆盖率快照、构建验证结果，以及与持久化相关的专项验证点。

## Automated Tests

## 自动化测试

- Command: `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`
- 命令：`.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:test`
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
  - bypass key still allows incoming items when configured
  - 按配置启用旁路键时，旁路键仍可放行放入行为
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

- Command: `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:compileJava :fabric:compileJava`
- 命令：`.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :common:compileJava :fabric:compileJava`
- Result: passed
- 结果：通过
- Command: `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`
- 命令：`.\gradle.bat --configure-on-demand --no-daemon --no-build-cache :fabric:compileJava :forge:compileJava :neoforge:compileJava`
- Result: passed
- 结果：通过
- Integration command: `.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`
- 集成命令：`.\gradle.bat --configure-on-demand --no-daemon --no-build-cache -Pskip_build_number_increment=true build`
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

## Notes

## 说明

- These results cover automated verification in the current workspace. They do not replace in-game multiplayer matrix testing.
- 以上结果对应当前工作区内的自动化验证，不替代实际游戏中的多人联机矩阵测试。
