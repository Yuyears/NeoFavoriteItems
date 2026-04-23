# Neo Favorite Items

`Neo Favorite Items` is a multi-loader inventory favorite/lock mod for Minecraft 1.21.1. It lets players mark their own inventory slots as favorite slots, then uses interaction guards, overlay hints, and persistence to reduce accidental item operations.

`Neo Favorite Items` 是一个面向 Minecraft 1.21.1 的多加载器物品栏收藏/锁定模组。玩家可以把自己的物品栏槽位标记为收藏槽位，并通过交互拦截、Overlay 提示和数据持久化减少误操作。

- Mod ID: `neo_favorite_items`
- Mod ID：`neo_favorite_items`
- Package: `mycraft.yuyears.neofavoriteitems`
- 包名：`mycraft.yuyears.neofavoriteitems`
- Version: `0.0.1-alpha-build1`
- 版本：`0.0.1-alpha-build1`
- Java: 21
- Java：21
- Build system: Gradle Kotlin DSL + Architectury Loom
- 构建系统：Gradle Kotlin DSL + Architectury Loom
- Supported loaders: Fabric, Forge, NeoForge
- 支持加载器：Fabric、Forge、NeoForge

## Feature Overview

## 功能概览

- Hold the lock-operation key and click a player inventory slot to toggle its favorite state.
- 按住锁定操作键后点击玩家物品栏槽位，可切换收藏状态。
- Default key bindings:
- 默认按键：
  - Lock operation: `Left Alt`
  - 锁定操作键：`Left Alt`
  - Bypass lock: `Left Control`
  - 旁路键：`Left Control`
- Favorite state uses a unified logical slot index:
- 收藏状态使用统一逻辑槽位索引：
  - `0..8`: hotbar
  - `0..8`：快捷栏
  - `9..35`: main inventory
  - `9..35`：主背包
  - `36..39`: armor
  - `36..39`：护甲栏
  - `40`: offhand
  - `40`：副手
- Favorite slots render overlays in GUI screens and on the hotbar HUD.
- 已收藏槽位会在 GUI 和快捷栏 HUD 中显示 Overlay。
- Supported PNG overlay textures: `border`, `classic`, `framework`, `highlight`, `brackets`, `lock`, `mark`, `tag`, `star`.
- 支持的 PNG Overlay 材质：`border`、`classic`、`framework`、`highlight`、`brackets`、`lock`、`mark`、`tag`、`star`。
- Overlay colors support both `0xAARRGGBB` and `luv(L, u, v, alpha)` formats.
- Overlay 颜色支持 `0xAARRGGBB` 和 `luv(L, u, v, alpha)` 两种写法。
- Configurable guards can block click, drop, quick move, Shift-click, drag, and swap actions.
- 可配置的交互守卫可阻止点击、丢弃、快速移动、Shift 点击、拖拽和交换等行为。
- When the mod is installed on the server, favorite state and sync are server-authoritative while the client remains responsive locally.
- 服务端安装本模组时，收藏状态与同步由服务端权威处理，同时客户端仍保持本地响应。
- Config files and in-game text provide English and Simplified Chinese resources.
- 配置文件和游戏内文本提供英文与简体中文资源。

## Project Structure

## 项目结构

```text
.
├─ common/       Loader-neutral code, config, state, persistence, render abstraction, resources
│                平台无关代码、配置、状态、持久化、渲染抽象和资源
├─ fabric/       Fabric entrypoints, networking, mixins, key bindings, rendering adapters
│                Fabric 入口、网络、Mixin、按键和渲染适配
├─ forge/        Forge entrypoint, networking, mixins, events, key bindings, rendering adapters
│                Forge 入口、网络、Mixin、事件、按键和渲染适配
├─ neoforge/     NeoForge entrypoint, networking, mixins, events, key bindings, rendering adapters
│                NeoForge 入口、网络、Mixin、事件、按键和渲染适配
├─ gradle/       Gradle Wrapper
├─ build.gradle.kts
├─ settings.gradle.kts
└─ gradle.properties
```

Common source code lives under `common/src/main/java/mycraft/yuyears/neofavoriteitems` and is organized into:

公共源码位于 `common/src/main/java/mycraft/yuyears/neofavoriteitems`，主要分为：

- `domain`: `LogicalSlotIndex`, `InteractionType`, `InteractionDecision`
- `domain`：`LogicalSlotIndex`、`InteractionType`、`InteractionDecision`
- `application`: `InteractionGuardService`, `ServerFavoriteService`, `ClientFavoriteSyncService`
- `application`：`InteractionGuardService`、`ServerFavoriteService`、`ClientFavoriteSyncService`
- `integration`: `SlotMappingService`
- `integration`：`SlotMappingService`
- `persistence`: `DataPersistenceManager`
- `persistence`：`DataPersistenceManager`
- `render`: `OverlayRenderer`
- `render`：`OverlayRenderer`
- Root package: `ConfigManager`, `FavoritesManager`, `NeoFavoriteItemsConfig`, `DebugLogger`
- 根包：`ConfigManager`、`FavoritesManager`、`NeoFavoriteItemsConfig`、`DebugLogger`

Common resources live under `common/src/main/resources/assets/neo_favorite_items`; loader modules merge these resources during `processResources`.

公共资源位于 `common/src/main/resources/assets/neo_favorite_items`，平台模块会在 `processResources` 阶段合并这些资源。

## Build

## 构建

```powershell
# Build all loaders
# 构建所有加载器
.\gradle.bat build

# Build one loader
# 单独构建
.\gradle.bat :fabric:build
.\gradle.bat :forge:build
.\gradle.bat :neoforge:build

# Compile Java only
# 只编译 Java
.\gradle.bat :common:compileJava :fabric:compileJava :forge:compileJava :neoforge:compileJava
```

Build outputs:

构建产物：

- `fabric/build/libs/neo_favorite_items-fabric-0.0.1-alpha-build1.jar`
- `forge/build/libs/neo_favorite_items-forge-0.0.1-alpha-build1.jar`
- `neoforge/build/libs/neo_favorite_items-neoforge-0.0.1-alpha-build1.jar`

## Development Runs

## 开发运行

```powershell
.\gradle.bat :fabric:runClient
.\gradle.bat :forge:runClient
.\gradle.bat :neoforge:runClient
```

## Configuration

## 配置

The first run generates:

首次运行后会生成：

```text
config/neo-favorite-items.toml
```

Main configuration groups:

主要配置分组：

- `general`: empty-slot locking, automatic empty-slot unlock, whether items may enter locked empty slots
- `general`：空槽锁定、空槽自动解锁、是否允许物品进入锁定空槽
- `lockBehavior`: guard rules for interaction types and bypass-key behavior
- `lockBehavior`：不同交互类型的拦截策略和旁路键行为
- `slotBehavior`: whether favorite state follows items or stays at slot positions
- `slotBehavior`：收藏状态随物品移动或固定在槽位
- `overlay`: overlay style, color, opacity, and foreground rendering switches
- `overlay`：Overlay 样式、颜色、透明度和前景渲染开关
- `feedback`: text and sound feedback settings
- `feedback`：文本、音效等反馈设置
- `debug`: diagnostic logging switch
- `debug`：诊断日志开关

Key bindings are managed through Minecraft Controls and are not written to the mod config file.

按键绑定通过 Minecraft 控制设置管理，不写入模组配置文件。

## Documents

## 文档

- `ARCHITECTURE_DESIGN.md`: current architecture, layer responsibilities, and runtime flows
- `ARCHITECTURE_DESIGN.md`：当前架构、分层职责和关键运行流程
- `TODO.md`: remaining work and validation items
- `TODO.md`：仍需完成或验证的事项
- `README.md`: user and developer entrypoint
- `README.md`：面向使用和开发入口的简要说明

## License

## 许可证

GNU General Public License v3.0

GNU 通用公共许可证第 3 版（GPL-3.0）
