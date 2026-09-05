# Neo Favorite Items

`Neo Favorite Items` is a multi-loader inventory favorite/lock mod for Minecraft 1.21.1. It lets players mark their own inventory slots as favorite slots, then uses interaction guards, overlay hints, and persistence to reduce accidental item operations.

`Neo Favorite Items` 是一个面向 Minecraft 1.21.1 的多加载器物品栏收藏/锁定模组。玩家可以把自己的物品栏槽位标记为收藏槽位，并通过交互拦截、Overlay 提示和数据持久化减少误操作。

- Mod ID: `neo_favorite_items`
- Mod ID：`neo_favorite_items`
- Package: `mycraft.yuyears.neofavoriteitems`
- 包名：`mycraft.yuyears.neofavoriteitems`
- Version: `0.0.2`
- 版本：`0.0.2`
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
- In-game configuration screen with bindable key, profile/layer editing, live client updates, manual reload, and automatic external-change detection.
- 提供可绑定按键打开的局内配置页，支持 Profile/Layer 编辑、客户端配置即时生效、手动 Reload 和外部修改自动检测。
- Overlay rendering supports up to four ordered layers per profile, custom PNG/JPG assets, arbitrary bounded dimensions, anchors, offsets, scale, rotation, clipping, overflow, z-index, and multiple color modes.
- Overlay 支持每个 Profile 最多四个有序渲染层、自定义 PNG/JPG 材质、受限任意尺寸、锚点、偏移、缩放、旋转、裁剪、溢出、z-index 和多种着色模式。
- Configurable sound feedback supports registered sound ID search, lazy paging, subtitle tooltips, and preview playback with cooldown.
- 音效反馈支持注册音效 ID 搜索、懒加载分页、subtitle 提示和带冷却的预览播放。
- Built-in overlay textures include `border`, `classic`, `framework`, `highlight`, `brackets`, `lock`, `mark`, `tag`, `star`, `heart`, and `arrow_down`; custom assets are loaded from `NFI Assets`.
- 支持的 PNG Overlay 材质：`border`、`classic`、`framework`、`highlight`、`brackets`、`lock`、`mark`、`tag`、`star`。
- Overlay colors support both `0xAARRGGBB` and `luv(L, u, v, alpha)` formats.
- Overlay 颜色支持 `0xAARRGGBB`、`rgba(r,g,b,a)` 和 `luv(L, u, v, alpha)` 写法。
- Configurable guards can block click, drop, quick move, Shift-click, drag, and swap actions.
- 可配置的交互守卫可阻止点击、丢弃、快速移动、Shift 点击、拖拽和交换等行为。
- Composite moves now guard both sides of the operation: locked slots are treated as blocked sources when an item would be removed and blocked targets when an item would be inserted. This covers GUI and GUI-outside offhand swaps plus quick-moving equipment into locked armor/offhand slots.
- 复合移动现在会同时保护操作两端：锁定槽位在物品被取出时视为受保护来源，在物品被放入时视为受保护目标。该规则覆盖 GUI 内外副手交换，以及 Shift 点击装备进入已锁定护甲/副手槽。
- Slot-level guards also cover standard `Slot` APIs such as safe insert, safe take, remove, and set operations so custom menus that use Minecraft slot semantics are protected without per-screen compatibility code.
- 槽位级守卫还覆盖 `safeInsert`、`safeTake`、`remove` 和 `set` 等标准 `Slot` API，因此使用 Minecraft 槽位语义的自定义菜单无需逐个界面适配也能受到保护。
- When an external block tries to place an item into a locked empty selected slot, the server reroutes the item to an empty unlocked hotbar slot, then an empty unlocked main-inventory slot, and finally drops it if no safe slot exists.
- 当外部方块尝试把物品放入已锁定的空当前槽时，服务端会先改放到空且未锁定的快捷栏槽，再改放到空且未锁定的主背包槽；若都没有可用位置，则把物品掉落到地上。
- Forge/NeoForge item-handler wrappers for player inventory no longer hide locked stacks from read APIs, so custom GUIs such as JustDireThings still render the actual player items while extraction, insertion, and direct writes remain guarded.
- Forge/NeoForge 的玩家背包 item-handler 包装器不再在读取 API 中隐藏锁定物品，因此 JustDireThings 等自定义 GUI 仍会显示真实玩家物品，同时提取、放入和直接写入仍会被拦截。
- Forge/NeoForge slot resolution also recognizes player slots exposed as `SlotItemHandler(InvWrapper/RangedWrapper)`, enabling overlays and early click guards in item-handler-backed GUIs.
- Forge/NeoForge 的槽位解析也能识别以 `SlotItemHandler(InvWrapper/RangedWrapper)` 暴露的玩家槽位，因此基于 item handler 的 GUI 中也会渲染 Overlay 并提前拦截点击。
- AE2 terminal `MOVE_REGION` is handled through AE2's shared menu abstractions when AE2 is present, covering space-left-click transfers without adapting each terminal screen separately.
- 安装 AE2 时，AE2 终端的 `MOVE_REGION` 会通过 AE2 公共菜单抽象层处理，覆盖空格+左键转移，而不需要逐个终端界面适配。
- Inventory sorting compatibility uses each sorter mod's own locked-slot concept when available: Quark and Inventory Tweaks ReFoxed receive favorite player-inventory slots as sorting-locked slots so sorting skips them instead of clearing or reordering them.
- 整理模组兼容会优先复用整理模组自身的锁槽概念：Quark 与 Inventory Tweaks ReFoxed 会在排序前收到已收藏玩家背包槽作为排序锁槽，使排序跳过这些槽位，而不是清空或重排它们。
- Better Experience fast storage on NeoForge skips only locked player-inventory source stacks, leaving its nearby-container and target insertion logic unchanged.
- NeoForge 上的 Better Experience 一键存储只跳过已锁玩家背包来源 stack，附近容器搜索和目标放入逻辑保持该模组原样。
- NeoForge Quark hotbar changer swaps are handled separately from sorting: when Quark's `Z` hotbar switch exchanges hotbar slots with main-inventory rows, the item exchange bypasses the generic lock guard and the favorite state moves with the exchanged stack.
- NeoForge 的 Quark 快捷栏切换会与排序分开处理：当 Quark 的 `Z` 键快捷栏切换在快捷栏与主背包行之间交换物品时，该物品交换会绕过通用锁槽守卫，并让收藏状态跟随被交换的物品移动。
- Mouse Tweaks-style drag clicks are supported on all three loaders. The compatibility layer borrows Mouse Tweaks' slot-enter detection, then routes newly entered player-inventory slots to NeoFavoriteItems' own lock toggle path instead of Mouse Tweaks' click semantics.
- 三个平台均支持 Mouse Tweaks 风格的拖动点击。兼容层只借用 Mouse Tweaks 的槽位进入检测，然后把新进入的玩家背包槽交给 NeoFavoriteItems 自己的锁定切换路径，而不是执行 Mouse Tweaks 的点击语义。
- On NeoForge, the normal `AbstractContainerScreen` fallback for Mouse Tweaks simulated clicks remains active alongside the dedicated Sophisticated screen path. Both paths now reuse slot-location helpers rather than Mouse Tweaks' event flow, which preserves vanilla GUI drag-lock toggling while keeping the Sophisticated empty-slot fix.
- 在 NeoForge 上，普通 `AbstractContainerScreen` 的 Mouse Tweaks 模拟点击 fallback 会与 Sophisticated 专用路径同时保留：两条路径现在都只复用槽位定位 helper，而不依赖 Mouse Tweaks 的事件流，这样既能维持原版 GUI 的拖拽锁定切换，也不会回退 Sophisticated 空槽修复。
- NeoForge modpacks that suppress Sophisticated empty-slot screen events are handled by a low-level press entry. With Mouse Tweaks installed, that entry does not cancel the raw press, so normal Mouse Tweaks drag detection remains available; Sophisticated empty-slot drag marking still depends on Mouse Tweaks exposing drag-enter samples for those empty slots.
- NeoForge 整合包中若 Sophisticated 空槽屏幕事件被压掉，会由低层按下入口补偿。安装 Mouse Tweaks 时，该入口不会取消原始按下，因此普通 Mouse Tweaks 拖动检测仍可工作；Sophisticated 空槽拖动标记仍取决于 Mouse Tweaks 是否为这些空槽暴露拖动进入采样。
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
  - Server-authoritative data that older builds accidentally wrote under the game directory `data/neo_favorite_items/players/<uuid>.dat` is migrated to the active world directory on first successful read, then the old file is removed.
  - 旧版本若曾把服务端权威数据误写到游戏根目录 `data/neo_favorite_items/players/<uuid>.dat`，首次成功读取后会迁移到当前世界目录，并删除旧文件。
  - Legacy `itemfavorites/...` client data is migrated to the new directory on first successful read, then the old file is removed.
  - 为兼容旧版本，首次成功读取旧的 `itemfavorites/...` 客户端数据后，会迁移到新目录并删除旧文件。
- Persistence triggers are split by lifecycle:
- 持久化触发时机按生命周期拆分：
  - World/server start: initialize the persistence context without preloading every player file.
  - 世界/服务端启动：只初始化持久化上下文，不全量预载所有玩家文件。
  - Player join: load that player's favorite data into the server cache and send a full sync.
  - 玩家进入世界：读取该玩家数据到服务端缓存，并发送全量同步。
  - During play: server-side favorite changes update the cache without immediately writing the player file.
  - 游戏过程中：服务端收藏变更只更新缓存，不立即写玩家文件。
  - Player leave: save that player's current state while keeping the cached entry available for the final server-stop flush.
  - 玩家退出世界：保存该玩家当前状态，同时保留缓存项供关服最终统一保存。
  - World/server stop: perform a final save flush for cached player data.
  - 世界/服务端关闭：对缓存玩家数据执行一次最终保存收尾。
- Death handling follows vanilla inventory semantics:
- 死亡处理遵循原版背包语义：
  - When `keepInventory=true`, the server marks respawn inventory restoration as an internal scoped operation, then copies the old player inventory into the respawned player while lock guards honor that marker.
  - `keepInventory=true` 时，服务端会把重生库存恢复标记为内部作用域操作，随后在锁槽守卫识别该标识并放行的情况下，把旧玩家背包复制到重生后的玩家。
  - By default, when `keepInventory=false`, favorite slots are cleared and saved because the items left the player inventory through death drops.
  - 默认情况下，`keepInventory=false` 时，由于物品通过死亡掉落离开玩家背包，收藏槽位会被清空并保存。
  - When `[deathBehavior] preserveLockedSlotContents=true`, non-empty locked player-inventory slots are temporarily hidden from vanilla death drops and restored to the respawned player even if `keepInventory=false`.
  - 当 `[deathBehavior] preserveLockedSlotContents=true` 时，即使 `keepInventory=false`，非空锁定玩家背包槽也会被临时从原版死亡掉落中隐藏，并恢复到重生后的玩家。
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
- `application`: `InteractionGuardService`, `ServerFavoriteService`, `ScopedPlayerOperationService`, `ClientFavoriteSyncService`, `InventorySortingCompatService`, `LockedEmptySlotFallback`
- `application`：`InteractionGuardService`、`ServerFavoriteService`、`ScopedPlayerOperationService`、`ClientFavoriteSyncService`、`InventorySortingCompatService`、`LockedEmptySlotFallback`
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

Third-party soft-compatibility mixins live under each loader's `mixin/compat` package and are loaded only from the non-required `*.compat.mixins.json` files. Compatibility mixin classes use `<ModOrFeature><TargetOrScenario>CompatMixin`, for example `Ae2MenuCompatMixin`.

第三方软联动 Mixin 位于各平台的 `mixin/compat` 包中，并且只通过非 required 的 `*.compat.mixins.json` 加载。兼容 Mixin 类使用 `<模组或功能><目标或场景>CompatMixin` 命名，例如 `Ae2MenuCompatMixin`。

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

- `fabric/build/libs/neo_favorite_items-1.21.1-fabric-release-0.0.2-build1.jar`
- `forge/build/libs/neo_favorite_items-1.21.1-forge-release-0.0.2-build1.jar`
- `neoforge/build/libs/neo_favorite_items-1.21.1-neoforge-release-0.0.2-build1.jar`
- `build/result/neo_favorite_items-<minecraft>-<loader>-<channel>-<modVersion>-<build>.jar`

`build_number` and `release_channel` are read-only defaults from `gradle.properties` and may be overridden explicitly, for example `-Pbuild_number=build5 -Prelease_channel=dev`. CI supplies its own build number. Gradle never rewrites `gradle.properties`.

`build_number` 和 `release_channel` 从 `gradle.properties` 读取默认值，也可显式覆盖，例如 `-Pbuild_number=build5 -Prelease_channel=dev`。CI 提供自己的构建号。Gradle 不会回写 `gradle.properties`。

The result-copy task uses lazy task-path dependencies so focused commands such as `.\gradle.bat :common:test` work with Gradle configuration-on-demand.

构建结果复制任务使用惰性任务路径依赖，因此 `.\gradle.bat :common:test` 等聚焦命令可在 Gradle configure-on-demand 下正常运行。

`.\gradle.bat` defaults `GRADLE_USER_HOME` to the project-local `.gradle-home` cache. Developers can override it with the `GRADLE_USER_HOME` environment variable, and can override the Gradle installation with `GRADLE_HOME`.

`.\gradle.bat` 默认把 `GRADLE_USER_HOME` 指向项目内 `.gradle-home` 缓存。开发者可通过 `GRADLE_USER_HOME` 环境变量改用本地自定义缓存，也可通过 `GRADLE_HOME` 指定 Gradle 安装。

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
config/neo-favorite-items-common.toml
config/neo-favorite-items-client.toml
```

Main configuration groups:

主要配置分组：

- `neo-favorite-items-common.toml`: server-authoritative or rule-affecting options. On a server, these values are the source of truth for lock behavior.
- `neo-favorite-items-common.toml`：服务端权威或影响规则的配置。服务端存在时，这些值是锁定行为的准则。
- `general`: empty-slot locking, automatic empty-slot unlock, whether items may enter locked empty slots
- `general`：空槽锁定、空槽自动解锁、是否允许物品进入锁定空槽
  - `autoUnlockEmptySlots=true` prevents empty slots from keeping favorite locks; `lockEmptySlots` only allows empty-slot locks when automatic empty unlock is disabled.
  - `autoUnlockEmptySlots=true` 时空槽不会保留收藏锁定；`lockEmptySlots` 只在关闭自动空槽解锁时才允许锁空槽。
  - `allowItemsIntoLockedEmptySlots` only applies when empty-slot locks are actually kept.
  - `allowItemsIntoLockedEmptySlots` 只在空槽锁定确实会被保留时生效。
  - When `allowItemsIntoLockedEmptySlots=false`, vanilla pickup/free-slot selection skips locked empty main-inventory slots before the dropped stack is consumed.
  - 当 `allowItemsIntoLockedEmptySlots=false` 时，原版拾取/空槽选择会在消耗掉落 stack 前跳过锁定空主背包槽。
- `lockBehavior`: guard rules for interaction types and bypass-key behavior
- `lockBehavior`：不同交互类型的拦截策略和旁路键行为
- `slotBehavior`: whether favorite state follows items or stays at slot positions
- `slotBehavior`：收藏状态随物品移动或固定在槽位
- `deathBehavior`: whether locked slot contents survive death even when `keepInventory=false`
- `deathBehavior`：是否在 `keepInventory=false` 时仍让锁定槽内容随死亡重生保留
- `debug`: diagnostic logging switch
- `debug`：诊断日志开关
- `neo-favorite-items-client.toml`: client-preferred presentation and feedback options.
- `neo-favorite-items-client.toml`：客户端优先生效的显示和反馈配置。
- `overlay`: overlay style, color, opacity, and foreground rendering switches
- `overlay`：Overlay 样式、颜色、透明度和前景渲染开关
- `feedback`: text and sound feedback settings
- `feedback`：文本、音效等反馈设置

When the config file is missing, the mod generates a complete default file. When an existing config has malformed lines, unknown keys, invalid values, or missing entries, readable values are kept in memory and the file is regenerated with those values plus defaults for unreadable or missing entries.

当配置文件不存在时，模组会生成完整默认配置。若已有配置存在格式错误、未知配置项、非法值或缺失项，能读取的值会先保留，随后用这些值加上未读取项的默认值重新生成配置文件。

Existing single-file `config/neo-favorite-items.toml` configs are read once as a migration source, split into the new common/client files, and then removed after the migration succeeds.

已有的单文件 `config/neo-favorite-items.toml` 会作为迁移来源读取一次，拆分写入新的 common/client 文件；迁移成功后会删除旧文件。

Key bindings are managed through Minecraft Controls and are not written to the mod config file.

按键绑定通过 Minecraft 控制设置管理，不写入模组配置文件。

## Documents

## 文档

- `docs/00-guide-documentation.md`: documentation catalog and naming rules
- `docs/00-guide-documentation.md`：文档目录和命名规范
- `docs/10-design-architecture.md`: current architecture, layer responsibilities, and runtime flows
- `docs/10-design-architecture.md`：当前架构、分层职责和关键运行流程
- `docs/20-plan-roadmap.md`: prioritized remaining work and validation items
- `docs/20-plan-roadmap.md`：按优先级整理的剩余工作和验证项
- `README.md`: user and developer entrypoint
- `README.md`：面向使用和开发入口的简要说明

## License

## 许可证

GNU General Public License v3.0

GNU 通用公共许可证第 3 版（GPL-3.0）
