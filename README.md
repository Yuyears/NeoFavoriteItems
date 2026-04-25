# Neo Favorite Items

`Neo Favorite Items` is a multi-loader inventory favorite/lock mod for Minecraft 1.21.1. It lets players mark their own inventory slots as favorite slots, then uses interaction guards, overlay hints, and persistence to reduce accidental item operations.

`Neo Favorite Items` 是一个面向 Minecraft 1.21.1 的多加载器物品栏收藏/锁定模组。玩家可以把自己的物品栏槽位标记为收藏槽位，并通过交互拦截、Overlay 提示和数据持久化减少误操作。

- Mod ID: `neo_favorite_items`
- Mod ID：`neo_favorite_items`
- Package: `mycraft.yuyears.neofavoriteitems`
- 包名：`mycraft.yuyears.neofavoriteitems`
- Version: `0.0.1-beta-hotfix-build2`
- 版本：`0.0.1-beta-hotfix-build2`
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
- Composite moves now guard both sides of the operation: locked slots are treated as blocked sources when an item would be removed and blocked targets when an item would be inserted. This covers GUI and GUI-outside offhand swaps plus quick-moving equipment into locked armor/offhand slots.
- 复合移动现在会同时保护操作两端：锁定槽位在物品被取出时视为受保护来源，在物品被放入时视为受保护目标。该规则覆盖 GUI 内外副手交换，以及 Shift 点击装备进入已锁定护甲/副手槽。
- Slot-level guards also cover standard `Slot` APIs such as safe insert, safe take, remove, and set operations so custom menus that use Minecraft slot semantics are protected without per-screen compatibility code.
- 槽位级守卫还覆盖 `safeInsert`、`safeTake`、`remove` 和 `set` 等标准 `Slot` API，因此使用 Minecraft 槽位语义的自定义菜单无需逐个界面适配也能受到保护。
- AE2 terminal `MOVE_REGION` is handled through AE2's shared menu abstractions when AE2 is present, covering space-left-click transfers without adapting each terminal screen separately.
- 安装 AE2 时，AE2 终端的 `MOVE_REGION` 会通过 AE2 公共菜单抽象层处理，覆盖空格+左键转移，而不需要逐个终端界面适配。
- Mouse Tweaks-style drag clicks are supported on all three loaders: holding the lock-operation key and dragging across player inventory slots toggles each slot reached by the simulated click flow.
- 三个平台均支持 Mouse Tweaks 风格的拖动点击：按住锁定操作键拖过玩家物品栏槽位时，会按模拟点击流程切换经过的每个槽位。
- When the mod is installed on the server, favorite state and sync are server-authoritative while the client remains responsive locally.
- 服务端安装本模组时，收藏状态与同步由服务端权威处理，同时客户端仍保持本地响应。
- Outside GUI screens, dropping a locked selected hotbar stack is blocked on the client before the drop animation is played.
- 在 GUI 外，若当前手持的快捷栏物品已锁定，客户端会在播放丢弃动画前直接阻止丢弃。
- The mod now supports optional installation on both sides: client-only keeps local lock behavior, server-only allows join without client features, and dual-install enables server-authoritative sync.
- 模组现已支持真正的双端可选安装：仅客户端安装时保留本地锁定功能，仅服务端安装时允许原版客户端加入但无模组功能，双端安装时启用服务端权威同步。
- Persistence now follows installation mode:
- 持久化现已按安装模式分流：
  - Client-only multiplayer stores data under `favoriteitems/<server-address>/players/<uuid>.dat` in the client game directory, falling back to the active remote address when the server list entry is temporarily unavailable.
  - 仅客户端联机时，数据保存在客户端根目录 `favoriteitems/<服务器地址>/players/<uuid>.dat`；如果服务器列表条目暂不可用，会回退使用当前连接远端地址。
  - Dual-install singleplayer and dedicated-server play store data under `<world>/data/neo_favorite_items/players/<uuid>.dat`.
  - 双端安装时，无论单人还是多人服务器，数据都保存在 `<世界目录>/data/neo_favorite_items/players/<uuid>.dat`。
  - Legacy `itemfavorites/...` client data is migrated to the new directory on first successful read, then the old file is removed.
  - 为兼容旧版本，首次成功读取旧的 `itemfavorites/...` 客户端数据后，会迁移到新目录并删除旧文件。
- Persistence triggers are split by lifecycle:
- 持久化触发时机按生命周期拆分：
  - World/server start: initialize context and preload stored favorite files.
  - 世界/服务端启动：初始化持久化上下文并预载已有收藏数据文件。
  - Player join: load only that player's favorite data.
  - 玩家进入世界：只读取该玩家数据。
  - Player leave: save that player's current state incrementally.
  - 玩家退出世界：增量保存该玩家当前状态。
  - World/server stop: perform a full save flush for cached player data.
  - 世界/服务端关闭：对缓存中的玩家数据执行一次完整保存收尾。
- Server-only installation keeps login compatible with unmodded clients by checking the target player's advertised payload/channel support before sending sync packets.
- 仅服务端安装时，服务端会在发送同步包前检查目标玩家连接声明的 payload/channel 支持，从而保持未安装客户端的登录兼容性。
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

- `fabric/build/libs/neo_favorite_items-fabric-0.0.1-beta-hotfix-<build_number>.jar`
- `forge/build/libs/neo_favorite_items-forge-0.0.1-beta-hotfix-<build_number>.jar`
- `neoforge/build/libs/neo_favorite_items-neoforge-0.0.1-beta-hotfix-<build_number>.jar`
- `build/result/neo_favorite_items-<loader>-0.0.1-beta-hotfix-<build_number>.jar`

`build_number` is stored in `gradle.properties` and automatically increments when running `build`, `assemble`, `jar`, or `remapJar` tasks. When `mod_version` changes, the next increment resets the build number to `build1`. It does not increment for `compileJava`, `runClient`, `help`, or `--dry-run`. Use `-Pskip_build_number_increment=true` when a release rebuild must keep the current build number.

`build_number` 保存在 `gradle.properties` 中，执行 `build`、`assemble`、`jar` 或 `remapJar` 任务时会自动递增。当 `mod_version` 变化时，下一次递增会把构建号重置为 `build1`。执行 `compileJava`、`runClient`、`help` 或 `--dry-run` 时不会递增。如果发布重构建需要保持当前构建号，可使用 `-Pskip_build_number_increment=true`。

The result-copy task uses lazy task-path dependencies so focused commands such as `.\gradle.bat :common:test` work with Gradle configuration-on-demand.

构建结果复制任务使用惰性任务路径依赖，因此 `.\gradle.bat :common:test` 等聚焦命令可在 Gradle configure-on-demand 下正常运行。

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
- `TEST_REPORT.md`: unit test summary and JaCoCo coverage snapshot
- `TEST_REPORT.md`：单元测试摘要和 JaCoCo 覆盖率快照
- `VERIFICATION_REPORT.md`: fix verification notes and integration validation summary
- `VERIFICATION_REPORT.md`：问题修复验证说明与集成验证摘要
- `TODO.md`: remaining work and validation items
- `TODO.md`：仍需完成或验证的事项
- `README.md`: user and developer entrypoint
- `README.md`：面向使用和开发入口的简要说明

## License

## 许可证

GNU General Public License v3.0

GNU 通用公共许可证第 3 版（GPL-3.0）
