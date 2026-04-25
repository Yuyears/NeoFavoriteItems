# Neo Favorite Items Architecture Notes

# Neo Favorite Items 架构说明

Last updated: 2026-04-25

最后更新：2026-04-25

This document records the current project structure and implementation boundaries. It describes the repository as it exists now, not an older migration draft.

本文档记录当前项目结构和实现边界。它描述的是当前仓库状态，而不是早期迁移草案。

## Design Goals

## 设计目标

- Keep favorite/lock rules in loader-neutral code whenever possible.
- 让收藏/锁定规则尽量位于平台无关代码中。
- Use a unified logical slot model instead of treating GUI slot ids as persistent identities.
- 使用统一逻辑槽位模型，避免把 GUI 槽位 id 当作持久化身份。
- Keep loader modules focused on registration, key bindings, networking, mixins/events, and render entrypoints.
- 平台层只负责注册、按键、网络、Mixin/事件和渲染入口。
- Keep the client responsive while using server-authoritative state and sync when the server has the mod installed.
- 客户端保持响应速度，服务端安装时负责权威状态和同步。
- Share common-layer services across Fabric, Forge, and NeoForge wherever practical.
- Fabric、Forge、NeoForge 尽量共享 common 层服务。

## Module Structure

## 模块结构

```text
common/
  src/main/java/mycraft/yuyears/neofavoriteitems/
    domain/        Domain models
                   纯领域模型
    application/   Use-case and sync services
                   用例服务和同步服务
    integration/   Minecraft slot-to-logical-slot adapters
                   Minecraft 槽位到逻辑槽位的适配
    persistence/   Favorite data I/O
                   收藏数据读写
    render/        Overlay abstraction and color utilities
                   Overlay 抽象和颜色工具
    *.java         Config, state, common initialization, logging
                   配置、状态、公共初始化和日志
  src/main/resources/assets/neo_favorite_items/
    lang/          en_us.json, zh_cn.json
    textures/      Overlay PNG textures
                   Overlay PNG 材质

fabric/
  Fabric entrypoints, client entrypoint, network payloads, slot resolving, mixins, Fabric renderer
  Fabric 入口、客户端入口、网络 payload、槽位解析、Mixin、Fabric 渲染器

forge/
  Forge entrypoint, network payloads, mixins, Forge events/key bindings/renderer
  Forge 入口、网络 payload、Mixin、Forge 事件/按键/渲染器

neoforge/
  NeoForge entrypoint, network payloads, mixins, NeoForge events/key bindings/renderer
  NeoForge 入口、网络 payload、Mixin、NeoForge 事件/按键/渲染器
```

## Layer Responsibilities

## 分层职责

### Domain

### Domain / 领域层

Location: `common/.../domain`

位置：`common/.../domain`

- `LogicalSlotIndex`: unified player inventory slot index in the `0..40` range
- `LogicalSlotIndex`：统一玩家物品栏槽位索引，范围 `0..40`
- `InteractionType`: click, drop, quick move, drag, swap, and related interaction types
- `InteractionType`：点击、丢弃、快速移动、拖拽、交换等交互类型
- `InteractionDecision`: allow, allow-with-bypass, deny, and denial reason
- `InteractionDecision`：允许、旁路允许、拒绝及原因

This layer does not depend on loader APIs.

该层不依赖加载器 API。

### Application

### Application / 应用层

Location: `common/.../application`

位置：`common/.../application`

- `InteractionGuardService`: decides whether an interaction should be allowed based on favorite state, config, bypass key, and slot contents
- `InteractionGuardService`：根据收藏状态、配置、旁路键和槽位内容做拦截决策
- `InteractionGuardService.evaluateIncomingItem`: applies target-slot semantics for composite moves so locked slots can reject incoming stacks before vanilla removes the source item
- `InteractionGuardService.evaluateIncomingItem`：为复合移动提供目标槽语义，使锁定槽位能在原版移除来源物品前拒绝放入
- `ServerFavoriteService`: handles server-side toggles, validation, revisions, bypass state, and server interaction protection
- `ServerFavoriteService`：处理服务端收藏切换、校验、修订号、旁路状态和服务端交互保护
- `ClientFavoriteSyncService`: applies full and incremental syncs, rejects stale revisions, and detects revision gaps
- `ClientFavoriteSyncService`：应用客户端全量/增量同步、过滤过期修订并检测同步缺口
- `ClientDropGuard`: decides whether a selected hotbar stack drop should be blocked before the client plays drop animation
- `ClientDropGuard`：决定是否应在客户端播放丢弃动画前阻止当前手持快捷栏物品被丢弃

### Integration

### Integration / 集成层

Location: `common/.../integration`

位置：`common/.../integration`

- `SlotMappingService`: converts between Minecraft player inventory indices and `LogicalSlotIndex`.
- `SlotMappingService`：在 Minecraft 玩家背包索引和 `LogicalSlotIndex` 之间转换。
- Current favorite/lock behavior only targets player inventory slots, not container-owned slots such as chests or furnaces.
- 当前收藏/锁定只面向玩家物品栏槽位，不面向箱子、熔炉等容器自身槽位。

### Persistence

### Persistence / 持久化层

Location: `common/.../persistence`

位置：`common/.../persistence`

- `DataPersistenceManager`: saves and loads favorite data by player UUID.
- `DataPersistenceManager`：按玩家 UUID 保存和加载收藏数据。
- It supports client-local storage namespaces keyed by server address, world-save storage for integrated/dedicated servers, legacy client-path fallback, and cached full-save/full-load server flows.
- 支持按服务器地址分命名空间的客户端本地存储、单人/专用服务端世界存档目录、旧客户端路径兼容回退，以及带缓存的服务端完整读写流程。

### Render

### Render / 渲染层

Location: `common/.../render`

位置：`common/.../render`

- `OverlayRenderer`: common base class and color/resource utilities for platform renderers.
- `OverlayRenderer`：平台渲染器的公共基类和颜色/资源工具。
- Loader modules resolve common textures into loader-specific rendering calls.
- 平台模块负责把公共材质解析为各自加载器可用的渲染调用。

## Loader Responsibilities

## 平台职责

### Fabric

### Fabric

- `NeoFavoriteItemsFabric`: common initialization, server lifecycle, player join/leave handling, and server full sync.
- `NeoFavoriteItemsFabric`：公共初始化、服务端生命周期、玩家加入/离开、服务端全量同步。
- `NeoFavoriteItemsFabricClient`: client config, key bindings, overlay, client sync receivers, and per-tick client persistence synchronization.
- `NeoFavoriteItemsFabricClient`：客户端配置、按键、Overlay、客户端同步接收，以及逐 tick 的客户端持久化上下文同步。
- `FabricFavoriteNetworking`: Toggle, Full Sync, Delta Sync, and Bypass Key State payloads.
- `FabricFavoriteNetworking`：Toggle、Full Sync、Delta Sync、Bypass Key State payload。
- Mixins cover normal containers, creative slots, inventory interactions, client `slotClicked` lock toggles, and player inventory mutation protection.
- Mixin 覆盖普通容器、创造模式槽位、物品栏交互、客户端 `slotClicked` 锁定切换和玩家背包变更保护。
- Player inventory mutation protection includes standard `Slot` mutation APIs such as `safeInsert`, `safeTake`, `tryRemove`, `remove`, `set`, and `setByPlayer`, avoiding risky global item-read hiding while covering custom menus that respect Minecraft slot semantics.
- 玩家背包变更保护覆盖 `safeInsert`、`safeTake`、`tryRemove`、`remove`、`set` 和 `setByPlayer` 等标准 `Slot` 变更 API，在不隐藏全局物品读取的前提下覆盖遵循 Minecraft 槽位语义的自定义菜单。
- Optional compatibility mixins are isolated in non-required compat configs. AE2 compatibility targets shared abstractions (`AEBaseMenu` and `MEStorageMenu`) and avoids early `ModList`/class-presence decisions, following the Mouse Tweaks-style runtime mixin application pattern.
- 可选兼容 Mixin 隔离在非 required 的 compat 配置中。AE2 兼容目标限定在公共抽象（`AEBaseMenu` 与 `MEStorageMenu`），并避免过早依赖 `ModList`/类存在性判断，采用更接近 Mouse Tweaks 的运行时 Mixin 应用方式。

### Forge

### Forge

- `NeoFavoriteItemsForge`: mod entrypoint, event bus wiring, key bindings, GUI layer, player login/logout, and server sync.
- `NeoFavoriteItemsForge`：Mod 入口、事件总线、按键、GUI Layer、玩家登录/登出、服务端同步。
- `ForgeFavoriteNetworking`: Forge SimpleChannel packet registration and send/receive handling.
- `ForgeFavoriteNetworking`：Forge SimpleChannel 网络包注册与收发。
- Mixins and Forge adapters reuse common decision services, including client `slotClicked` handling for normal clicks and Mouse Tweaks simulated drag clicks.
- Mixin 和 Forge 适配类复用 common 决策服务，包括普通点击与 Mouse Tweaks 模拟拖动点击的客户端 `slotClicked` 处理。

### NeoForge

### NeoForge

- `NeoFavoriteItemsNeoForge`: mod entrypoint, NeoForge events, key bindings, GUI layer, payload handler, and player login/logout.
- `NeoFavoriteItemsNeoForge`：Mod 入口、NeoForge 事件、按键、GUI Layer、payload handler、玩家登录/登出。
- `NeoForgeFavoriteNetworking`: NeoForge payload registration and send/receive handling.
- `NeoForgeFavoriteNetworking`：NeoForge payload 注册与收发。
- Mixins and NeoForge adapters reuse common decision services, including client `slotClicked` handling for normal clicks and Mouse Tweaks simulated drag clicks.
- Mixin 和 NeoForge 适配类复用 common 决策服务，包括普通点击与 Mouse Tweaks 模拟拖动点击的客户端 `slotClicked` 处理。

## Runtime Flows

## 核心流程

### Toggle Favorite

### 切换收藏

1. The client detects the lock-operation key and the target player inventory slot.
2. 客户端检测锁定操作键和目标玩家物品栏槽位。
3. Platform slot resolving converts the target to `LogicalSlotIndex`.
4. 平台槽位解析把目标转换为 `LogicalSlotIndex`。
5. If server support is available, the client sends a Toggle request.
6. 有服务端支持时发送 Toggle 请求。
7. `ServerFavoriteService` validates empty-slot rules, updates favorite state, saves data, and creates a revision.
8. `ServerFavoriteService` 校验空槽配置、更新收藏状态、保存数据并生成修订号。
9. The server sends a full or incremental sync.
10. 服务端发送全量或增量同步。
11. The client applies the sync through `ClientFavoriteSyncService` and refreshes local presentation.
12. 客户端通过 `ClientFavoriteSyncService` 应用同步结果并刷新本地展示。

### Interaction Guard

### 交互拦截

1. A platform mixin/event captures click, quick move, drag, drop, swap, or external movement.
2. 平台 Mixin/事件捕获点击、快速移动、拖拽、丢弃、交换或外部移动。
3. Only the player's own inventory slots are processed.
4. 只处理玩家自己的物品栏槽位。
5. `InteractionGuardService` returns a decision based on config and favorite state.
6. `InteractionGuardService` 根据配置和收藏状态返回决策。
7. The platform layer cancels or allows the interaction according to that decision.
8. 平台层按决策取消交互或允许继续。
9. Composite moves evaluate both ends: the source slot uses normal removal semantics and the destination slot uses incoming-item semantics.
10. 复合移动会同时检查两端：来源槽使用普通取出语义，目标槽使用放入语义。
11. Offhand swaps check the hovered/selected slot and slot `40`; quick-moving equipment checks the corresponding armor/offhand target before allowing the source move.
12. 副手交换会同时检查悬停/当前槽与 `40` 号副手槽；Shift 点击装备会先检查对应护甲/副手目标槽再允许来源移动。
13. Standard slot mutations are guarded at `Slot` API boundaries so custom menu flows that call `safeInsert`, `safeTake`, `tryRemove`, `remove`, `set`, or `setByPlayer` still reuse the common decision service.
14. 标准槽位变更会在 `Slot` API 边界被守卫，因此调用 `safeInsert`、`safeTake`、`tryRemove`、`remove`、`set` 或 `setByPlayer` 的自定义菜单流程仍会复用 common 决策服务。
15. AE2 `MOVE_REGION` uses AE2 menu-layer compatibility: player-slot sources are blocked in `AEBaseMenu.quickMoveStack`, and network-to-player region moves are canceled from `MEStorageMenu.handleNetworkInteraction` when they would target a locked player inventory slot.
16. AE2 的 `MOVE_REGION` 通过 AE2 菜单层兼容处理：玩家槽作为来源时在 `AEBaseMenu.quickMoveStack` 拦截，网络物品批量移入玩家背包时在 `MEStorageMenu.handleNetworkInteraction` 预检查锁定目标槽并取消。
17. The guard deliberately does not hide `Slot.getItem` or `Inventory.getItem`, because global read interception can break menu synchronization and third-party inspection logic.
18. 守卫刻意不隐藏 `Slot.getItem` 或 `Inventory.getItem`，因为全局读取拦截可能破坏菜单同步和第三方检查逻辑。
19. Bypass-key state is polled on the client and synced to the server.
20. 旁路键状态由客户端按键轮询同步到服务端。
21. When the lock-operation key is held, client `slotClicked` mixins consume left-click pickup events and toggle the reached player-inventory slot. This also covers Mouse Tweaks drag clicks because it simulates movement by invoking `slotClicked` for each entered slot.
22. 按住锁定操作键时，客户端 `slotClicked` Mixin 会消费左键 PICKUP 事件并切换经过的玩家物品栏槽位。Mouse Tweaks 的拖动点击通过为每个进入的槽位调用 `slotClicked` 实现，因此同样会进入该流程。

### Installation Modes

### 安装模式

1. Client only: the client keeps local favorites, local guards, and overlays. No server sync packets are sent when the remote side does not advertise the mod channel.
2. 仅客户端安装：客户端保留本地收藏、交互守卫与 Overlay；当远端未声明模组通道时不会发送同步包。
3. Server only: vanilla or unmodded clients may join. Server-side login still loads favorite data, but platform networking checks the player's advertised payload/channel support before sending full or incremental sync packets.
4. 仅服务端安装：原版或未安装客户端可加入服务器。服务端登录流程仍读取收藏数据，但平台网络层会先检查该玩家连接声明的 payload/channel 支持，再发送全量或增量同步包。
5. Both sides installed: favorite state becomes server-authoritative with full/incremental sync and bypass-key state sync.
6. 双端安装：收藏状态由服务端权威管理，并启用全量/增量同步和旁路键状态同步。

### Persistence Lifecycle

### 持久化生命周期

1. Server/world start initializes the persistence context and preloads cached player data from the active storage root.
2. 服务端/世界启动时会初始化持久化上下文，并从当前存储根目录预载玩家数据缓存。
3. Player login performs a partial load for only that player's UUID.
4. 玩家进入世界时，只对该玩家 UUID 执行部分读取。
5. Player logout performs an incremental save for only that player's current state.
6. 玩家退出世界时，只对该玩家当前状态执行增量保存。
7. Server/world stop flushes online-player state and then writes the full cache back to disk.
8. 服务端/世界关闭时，会先收集在线玩家状态，再把完整缓存回写到磁盘。
9. Client-only multiplayer uses `favoriteitems/<sanitized-server-address>/players/<uuid>.dat`; if the server list entry is not available yet, the client falls back to the active connection remote address before using the default namespace.
10. 仅客户端联机模式使用 `favoriteitems/<净化后的服务器地址>/players/<uuid>.dat`；如果服务器列表条目暂不可用，客户端会先回退到当前连接远端地址，最后才使用默认命名空间。
11. Dual-install singleplayer and dedicated-server modes use `<world>/data/neo_favorite_items/players/<uuid>.dat`.
12. 双端安装下的单人与多人服务端模式统一使用 `<世界目录>/data/neo_favorite_items/players/<uuid>.dat`。

### Hotbar Drop Guard

### 快捷栏丢弃拦截

1. Outside GUI screens, vanilla triggers selected-slot drops from `Minecraft.handleKeybinds`.
2. 在 GUI 外，原版通过 `Minecraft.handleKeybinds` 触发当前手持槽位的丢弃。
3. Loader-specific client mixins redirect the `LocalPlayer.drop(boolean)` call.
4. 三个平台的客户端 Mixin 会重定向 `LocalPlayer.drop(boolean)` 调用。
5. `ClientDropGuard` checks favorite state and bypass-key rules before the drop packet or animation is emitted.
6. `ClientDropGuard` 会在发出丢弃数据包或播放动画前检查收藏状态和旁路规则。

### Overlay Rendering

### Overlay 渲染

1. The platform renderer enumerates GUI slots or hotbar slots.
2. 平台渲染器枚举 GUI 槽位或快捷栏槽位。
3. Slots are mapped to `LogicalSlotIndex`.
4. 槽位映射到 `LogicalSlotIndex`。
5. Overlay style is selected from favorite state, lock-operation key state, bypass-key state, and config.
6. 根据收藏状态、锁定操作键、旁路键和配置选择 Overlay 样式。
7. PNG textures are sampled at their actual dimensions and scaled to 16x16 slots.
8. PNG 材质按实际尺寸采样并缩放到 16x16 槽位。
9. Config decides whether overlays render in front of or behind item icons.
10. 根据配置决定绘制在物品图标前方或后方。

## Current Constraints

## 当前约束

- Favorite identity only covers player inventory slots `0..40`.
- 收藏身份只覆盖玩家物品栏 `0..40`。
- Container-owned slot favorites are not implemented.
- 容器自身槽位收藏尚未实现。
- Item-movement policy has configuration entrypoints, but complex move tracking while bypass is held still needs work.
- 物品移动策略已有配置入口，旁路状态下的复杂移动追踪仍需继续完善。
- Visual feedback and actual sound playback still need implementation.
- 视觉反馈和音效实际播放仍需补齐。
- Automated unit tests now cover favorites state, client sync, drop guard, config loading, persistence, and reflection cache behavior.
- 现已建立自动化单元测试，覆盖收藏状态、客户端同步、丢弃拦截、配置加载、持久化和反射缓存行为。
- Interaction decision tests now cover locked incoming targets for offhand swaps and armor quick-move equipment.
- 交互决策测试现已覆盖副手交换和护甲 Shift 装备中的锁定目标槽放入判定。
- Interaction decision tests also cover locked quick-move source removal and bypass behavior.
- 交互决策测试也已覆盖锁定槽作为快速移动来源时的取出拒绝，以及旁路键放行行为。
- AE2 compatibility is intentionally common-abstraction based. The NeoForge AE2 terminal scenario has been validated for space-left-click `MOVE_REGION` into and out of locked player inventory slots; Fabric and Forge still need runtime checks.
- AE2 兼容刻意基于公共抽象层实现。NeoForge 的 AE2 终端场景已验证空格+左键 `MOVE_REGION` 对锁定玩家背包槽的放入与取出；Fabric 和 Forge 仍需运行时验证。

## Maintenance Rules

## 维护约定

- Prefer putting new logic in `common`; keep loader layers as thin adapters.
- 新逻辑优先放入 `common`，平台层保持薄适配。
- Persistence and networking should pass logical slots or player inventory indices, not GUI slot ids.
- 持久化和网络只传递逻辑槽位或玩家背包索引，不传递 GUI slot id。
- Prefer safe, semantic API boundaries before adding mod-specific compatibility. Do not globally hide `getItem` reads unless a future fix proves the synchronization impact is acceptable.
- 优先选择安全的语义 API 边界，再考虑模组特定兼容；除非后续修复能证明同步影响可控，否则不要全局隐藏 `getItem` 读取。
- When adding an Overlay style, update:
- 新增 Overlay 样式时，同时更新：
  - `NeoFavoriteItemsConfig.OverlayStyle`
  - `OverlayRenderer` resource paths
  - `OverlayRenderer` 资源路径
  - all three platform renderers
  - 三个平台渲染器
  - `common/src/main/resources/assets/neo_favorite_items/textures`
- When adding a config option, update:
- 新增配置项时，同时更新：
  - `NeoFavoriteItemsConfig`
  - `ConfigManager`
  - language files or config comments
  - 语言文件或配置注释
  - the configuration summary in `README.md`
  - `README.md` 的配置摘要
