# Neo Favorite Items Architecture Notes

# Neo Favorite Items 架构说明

Last updated: 2026-05-10

最后更新：2026-05-10

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
- `LockedEmptySlotFallback`: selects the safe fallback slot for incoming stacks blocked by a locked empty target, preferring hotbar slots before main-inventory slots
- `LockedEmptySlotFallback`：为被锁定空目标槽拒绝的放入物品选择安全回退槽，优先快捷栏，其次主背包
- `ClientFavoriteSyncService`: applies full and incremental syncs, rejects stale revisions, and detects revision gaps
- `ClientFavoriteSyncService`：应用客户端全量/增量同步、过滤过期修订并检测同步缺口
- `ClientDropGuard`: decides whether a selected hotbar stack drop should be blocked before the client plays drop animation
- `ClientDropGuard`：决定是否应在客户端播放丢弃动画前阻止当前手持快捷栏物品被丢弃
- `InventorySortingCompatService`: merges favorite player-inventory slots into third-party sorter locked-slot lists before sorters clear and rewrite inventory ranges; it also owns the common favorite-state swap helper used by Quark's hotbar changer compatibility
- `InventorySortingCompatService`：在第三方整理模组清空并重写排序范围前，把已收藏玩家背包槽合并进整理模组的锁槽列表；同时提供 Quark 快捷栏切换兼容所用的收藏状态交换 helper

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
- It supports client-local storage namespaces keyed by server address, world-save storage for integrated/dedicated servers, legacy client-path fallback, migration from the old game-directory server root, per-player login reads, cache-only in-play updates, and full-cache server-stop flushes.
- 支持按服务器地址分命名空间的客户端本地存储、单人/专用服务端世界存档目录、旧客户端路径兼容回退、从旧游戏根目录服务端路径迁移、玩家登录按 UUID 读取、游戏过程仅更新缓存，以及关服完整缓存回写。

### Config

### 配置层

- `ConfigManager` loads the current config into `NeoFavoriteItemsConfig`, records malformed lines, unknown entries, and invalid values, then rewrites the file when repair is needed.
- `ConfigManager` 会把当前配置读取到 `NeoFavoriteItemsConfig`，记录格式错误行、未知配置项和非法值，并在需要修复时重写配置文件。
- Readable values are preserved during repair; unreadable or missing entries are regenerated from defaults.
- 修复配置时会保留能读取的值；无法读取或缺失的配置项按默认值重新生成。

### Render

### Render / 渲染层

Location: `common/.../render`

位置：`common/.../render`

- `OverlayRenderer`: common base class and color/resource utilities for platform renderers.
- `OverlayRenderer`：平台渲染器的公共基类和颜色/资源工具。
- `OverlayRenderDescriptor`: loader-neutral render description for style, tint, opacity, and foreground placement.
- `OverlayRenderDescriptor`：平台无关的渲染描述，统一表达样式、染色、透明度和前景位置。
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
- Mixins cover normal containers, creative slots, inventory interactions, the unified Alt lock-operation mouse state machine, leaked `slotClicked` cancellation while the lock-operation key is held, and player inventory mutation protection.
- Mixin 覆盖普通容器、创造模式槽位、物品栏交互、统一 Alt 锁定操作鼠标状态机、按住锁定操作键时漏进来的 `slotClicked` 取消，以及玩家背包变更保护。
- Player inventory mutation protection includes standard `Slot` mutation APIs such as `safeInsert`, `safeTake`, `tryRemove`, `remove`, `set`, and `setByPlayer`, avoiding risky global item-read hiding while covering custom menus that respect Minecraft slot semantics.
- 玩家背包变更保护覆盖 `safeInsert`、`safeTake`、`tryRemove`、`remove`、`set` 和 `setByPlayer` 等标准 `Slot` 变更 API，在不隐藏全局物品读取的前提下覆盖遵循 Minecraft 槽位语义的自定义菜单。
- Forge/NeoForge additionally guard player-inventory item-handler wrappers (`InvWrapper` and `RangedWrapper`) only at mutation boundaries such as validity checks, extraction, insertion, and direct writes. Read APIs such as `getStackInSlot` and `getSlotLimit` are left untouched so item-handler-backed custom GUIs keep rendering real player inventory contents.
- Forge/NeoForge 还会在玩家背包 item-handler 包装器（`InvWrapper` 与 `RangedWrapper`）的变更边界进行保护，例如有效性检查、提取、放入和直接写入。`getStackInSlot` 与 `getSlotLimit` 等读取 API 不再被改写，以保证基于 item handler 的自定义 GUI 继续显示真实玩家背包内容。
- Forge/NeoForge slot resolvers map `SlotItemHandler` instances backed by `InvWrapper` or `RangedWrapper` back to player inventory indices. This covers JustDireThings-style screens where player slots are item-handler slots rather than vanilla `Slot(playerInventory, ...)` instances.
- Forge/NeoForge 的槽位解析器会把由 `InvWrapper` 或 `RangedWrapper` 支撑的 `SlotItemHandler` 映射回玩家背包索引。该路径覆盖 JustDireThings 这类把玩家槽实现为 item-handler 槽，而不是原版 `Slot(playerInventory, ...)` 的界面。
- Optional compatibility mixins are isolated in non-required compat configs. AE2 compatibility targets shared abstractions (`AEBaseMenu` and `MEStorageMenu`) and avoids early `ModList`/class-presence decisions, following the Mouse Tweaks-style runtime mixin application pattern.
- 可选兼容 Mixin 隔离在非 required 的 compat 配置中。AE2 兼容目标限定在公共抽象（`AEBaseMenu` 与 `MEStorageMenu`），并避免过早依赖 `ModList`/类存在性判断，采用更接近 Mouse Tweaks 的运行时 Mixin 应用方式。

### Forge

### Forge

- `NeoFavoriteItemsForge`: mod entrypoint, event bus wiring, key bindings, GUI layer, player login/logout, and server sync.
- `NeoFavoriteItemsForge`：Mod 入口、事件总线、按键、GUI Layer、玩家登录/登出、服务端同步。
- `ForgeFavoriteNetworking`: Forge SimpleChannel packet registration and send/receive handling.
- `ForgeFavoriteNetworking`：Forge SimpleChannel 网络包注册与收发。
- Mixins and Forge adapters reuse common decision services. Lock-operation toggles have a single Alt-state-machine exit: the physical click and each sampled drag slot call the loader slot-toggle handler directly, while leaked `slotClicked` inventory actions are only canceled.
- Mixin 和 Forge 适配类复用 common 决策服务。锁定操作的基础状态机只处理 Alt 物理点击、事件拦截与释放收尾；Mouse Tweaks 存在时，拖动槽位进入检测由兼容层转发到同一个加载器槽位切换 handler，漏进来的 `slotClicked` 库存动作只会被取消。

### NeoForge

### NeoForge

- `NeoFavoriteItemsNeoForge`: mod entrypoint, NeoForge events, key bindings, GUI layer, payload handler, and player login/logout.
- `NeoFavoriteItemsNeoForge`：Mod 入口、NeoForge 事件、按键、GUI Layer、payload handler、玩家登录/登出。
- `NeoForgeFavoriteNetworking`: NeoForge payload registration and send/receive handling.
- `NeoForgeFavoriteNetworking`：NeoForge payload 注册与收发。
- Mixins and NeoForge adapters reuse common decision services. Lock-operation toggles have a single Alt-state-machine exit: the physical click and each sampled drag slot call the loader slot-toggle handler directly, while leaked `slotClicked` inventory actions are only canceled.
- Mixin 和 NeoForge 适配类复用 common 决策服务。锁定操作的基础状态机只处理 Alt 物理点击、事件拦截与释放收尾；Mouse Tweaks 存在时，拖动槽位进入检测由兼容层转发到同一个加载器槽位切换 handler，漏进来的 `slotClicked` 库存动作只会被取消。
- Container mouse state machines on all three loaders consume Alt-left-click or Alt-left-drag before vanilla can turn it into `PICKUP` or `QUICK_MOVE`. NeoForge Sophisticated Backpacks uses a dedicated pure slot-location branch that avoids `StorageScreenBase.findSlot`, because that method carries storage-screen click semantics such as filtered/all-similar selection and empty-slot behavior.
- 三个平台的容器鼠标状态机会在原版把 Alt+左键或 Alt+左键拖动转换成 `PICKUP`/`QUICK_MOVE` 前消费掉。NeoForge Sophisticated Backpacks 使用专用的纯槽位定位分支，避免调用 `StorageScreenBase.findSlot`，因为该方法包含过滤/同类选择和空槽点击等存储界面业务语义。
- NeoForge Sophisticated compatibility resolves player inventory slots through `StorageContainerMenuBase` indexing: storage slots come first, then the 36 player-inventory slots, followed by optional extra/upgrade slots. Server menu click protection ignores Sophisticated storage-owned slots even when their item-handler slot numbers overlap player inventory indices.
- NeoForge 的 Sophisticated 兼容通过 `StorageContainerMenuBase` 的索引体系定位玩家物品栏槽位：先是存储槽，然后是 36 个玩家背包槽，之后才是可选 extra/upgrade 槽。服务端菜单点击保护会忽略 Sophisticated 自己的存储槽，即使这些 item-handler 槽号与玩家背包索引重叠也不会误拦。
- NeoForge has one additional low-level `MouseHandler` press entry for modpacks where empty-slot clicks do not reach `ScreenEvent` or `mouseClicked`. When Mouse Tweaks is present, this entry primes and toggles the press target without canceling the raw press so Mouse Tweaks can still initialize drag detection.
- NeoForge 额外提供一个低层 `MouseHandler` 按下入口，用于处理部分整合包中空槽点击到不了 `ScreenEvent` 或 `mouseClicked` 的情况。存在 Mouse Tweaks 时，该入口只预激活并切换按下目标，不取消原始按下，从而让 Mouse Tweaks 继续初始化拖动检测。
- The optional NeoForge compatibility mixins for Quark target `SortingHandler.sortInventory(Container, int, int, int[])` and `ChangeHotbarMessage.swap(Container, int, int)`. Sorting augments Quark's locked slot array with favorite player-inventory indices. Hotbar changing runs the swap under a scoped inventory-guard bypass, then swaps favorite state and sends a full favorite sync.
- NeoForge 的可选 Quark 兼容 Mixin 目标为 `SortingHandler.sortInventory(Container, int, int, int[])` 与 `ChangeHotbarMessage.swap(Container, int, int)`。排序路径会把已收藏玩家背包索引补入 Quark 的排序锁定槽数组；快捷栏切换路径会在有作用域的库存守卫旁路下执行交换，随后交换收藏状态并发送全量收藏同步。

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
7. `ServerFavoriteService` validates empty-slot rules through `FavoriteLockRules`, updates favorite state, caches data, and creates a revision.
8. `ServerFavoriteService` 通过 `FavoriteLockRules` 校验空槽配置、更新收藏状态、缓存数据并生成修订号。
9. The server sends a full or incremental sync.
10. 服务端发送全量或增量同步。
11. The client applies the sync through `ClientFavoriteSyncService` and refreshes local presentation.
12. 客户端通过 `ClientFavoriteSyncService` 应用同步结果并刷新本地展示。
13. Empty slots can only keep favorite locks when `lockEmptySlots=true` and `autoUnlockEmptySlots=false`; if automatic empty unlock is enabled, newly empty slots are cleared and new empty-slot locks are rejected.
14. 只有 `lockEmptySlots=true` 且 `autoUnlockEmptySlots=false` 时，空槽才会保留收藏锁定；启用空槽自动解锁后，新变空的槽位会清除收藏，新的空槽锁定也会被拒绝。

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
15. Forge/NeoForge item-handler-backed player inventory views keep read APIs transparent while guarding extraction, insertion, and direct mutation. This protects external transfers without making locked slots look empty in custom screens such as JustDireThings.
16. Forge/NeoForge 中基于 item handler 的玩家背包视图保持读取 API 透明，只拦截提取、放入和直接变更。这样既能保护外部转移，也不会让 JustDireThings 等自定义界面中的锁定槽显示为空。
17. Item-handler-backed player slots are resolved before overlay rendering, client `slotClicked` handling, container mouse state machines, and server menu click handling. This keeps JDT-style GUI behavior consistent with vanilla inventory slots.
18. 基于 item handler 的玩家槽位会在 Overlay 渲染、客户端 `slotClicked`、容器鼠标状态机和服务端菜单点击处理中先被解析，因此 JDT 风格 GUI 的行为与原版玩家槽保持一致。
19. AE2 `MOVE_REGION` uses AE2 menu-layer compatibility: player-slot sources are blocked in `AEBaseMenu.quickMoveStack`, and network-to-player region moves are canceled from `MEStorageMenu.handleNetworkInteraction` when they would target a locked player inventory slot.
20. AE2 的 `MOVE_REGION` 通过 AE2 菜单层兼容处理：玩家槽作为来源时在 `AEBaseMenu.quickMoveStack` 拦截，网络物品批量移入玩家背包时在 `MEStorageMenu.handleNetworkInteraction` 预检查锁定目标槽并取消。
21. Sorter compatibility feeds favorite player-inventory slots into sorter-owned locked-slot mechanisms where available. NeoForge currently covers Quark inventory sorting and Inventory Tweaks ReFoxed server-side player sorting this way.
22. 整理兼容会在可用时把已收藏玩家背包槽交给整理模组自身的锁槽机制。NeoForge 目前以这种方式覆盖 Quark 背包整理和 Inventory Tweaks ReFoxed 服务端玩家背包整理。
23. NeoForge Quark hotbar changer compatibility treats each Quark `Z` key row swap as an atomic item swap: generic inventory set guards are bypassed only for that swap, favorite state is swapped between the two player inventory indices, persistence is cached, and the client receives a full favorite sync.
24. NeoForge 的 Quark 快捷栏切换兼容会把每次 Quark `Z` 键行交换视为原子物品交换：通用库存写入守卫只在该交换范围内旁路，收藏状态会在两个玩家背包索引之间交换，随后缓存持久化并向客户端发送全量收藏同步。
25. The guard deliberately does not hide `Slot.getItem`, `Inventory.getItem`, item-handler `getStackInSlot`, or item-handler slot-limit reads, because read interception can break menu synchronization, rendering, and third-party inspection logic.
26. 守卫刻意不隐藏 `Slot.getItem`、`Inventory.getItem`、item-handler `getStackInSlot` 或 item-handler 槽位上限读取，因为读取拦截可能破坏菜单同步、渲染和第三方检查逻辑。
27. Direct external writes into a locked empty player slot are not silently canceled after the source item has already been removed. Dedicated `Inventory.setItem` and main-hand `Player.setItemInHand` reroute entries move the incoming stack to the first empty unlocked hotbar slot, then the first empty unlocked main-inventory slot, and drop it when no fallback slot exists.
28. 对已锁定空玩家槽的外部直接写入不会在来源物品已被移除后静默取消。专用的 `Inventory.setItem` 与主手 `Player.setItemInHand` 回退入口会把 incoming stack 改放到第一个空且未锁定的快捷栏槽，再改放到第一个空且未锁定的主背包槽；没有回退槽时掉落。
29. GUI cursor placement and normal `Slot` APIs remain pure guards: `mayPlace`, `safeInsert`, `set`, and `setByPlayer` reject locked empty slots without rerouting the carried stack. This preserves the expected locked-slot experience and avoids duplicating cursor stacks.
30. GUI 光标放入和普通 `Slot` API 仍保持纯拦截：`mayPlace`、`safeInsert`、`set` 与 `setByPlayer` 会拒绝锁定空槽，但不会改道光标物品。这样保留锁槽体验，并避免复制光标物品。
31. NeoForge GUI mouse guarding uses official `ScreenEvent.MouseButtonPressed.Pre` and `ScreenEvent.MouseButtonReleased.Pre` hooks. The release hook covers creative inventory cursor placement, where vanilla can defer the `PICKUP` slot action until mouse release.
32. NeoForge GUI 鼠标守卫使用官方 `ScreenEvent.MouseButtonPressed.Pre` 与 `ScreenEvent.MouseButtonReleased.Pre` 事件。释放阶段守卫覆盖创造物品栏光标放入路径，因为原版可能把 `PICKUP` 槽位动作延迟到鼠标释放时执行。
33. Bypass-key state is polled on the client and synced to the server.
34. 旁路键状态由客户端按键轮询同步到服务端。
35. When the lock-operation key is held, the state machine has explicit exits: `beginPress` activates the operation and toggles the press target, `toggleEnteredSlot` handles Mouse Tweaks drag-enter targets, `toggleLeakedSlotClick` handles Sophisticated leaked click actions, and `consumeActiveDrag` only suppresses leaked vanilla drag while active.
36. 按住锁定操作键时，状态机有明确出口：`beginPress` 激活操作并切换按下目标，`toggleEnteredSlot` 处理 Mouse Tweaks 拖动进入目标，`toggleLeakedSlotClick` 处理 Sophisticated 漏出的点击动作，`consumeActiveDrag` 只在 active 时吞掉漏出的原版拖动。
37. Mouse Tweaks compatibility on all three loaders refreshes Mouse Tweaks' screen bookkeeping, borrows its `getSlotUnderMouse` and `oldSelectedSlot` slot-enter detection, then consumes the Alt drag event before Mouse Tweaks can run its own click semantics. Newly entered slots are handed to this mod's lock-operation toggle exit, keeping Mouse Tweaks optional without inheriting its empty-slot, carried-stack, shift, or config rules.
38. 三个平台的 Mouse Tweaks 兼容都会先刷新 Mouse Tweaks 的界面账本，借用它的 `getSlotUnderMouse` 与 `oldSelectedSlot` 槽位进入检测，然后在 Mouse Tweaks 执行自身点击语义前消费 Alt 拖动事件。新进入的槽位会交给本模组的锁定操作切换出口，因此 Mouse Tweaks 仍是可选兼容，但不会继承它的空槽、光标物品、Shift 或配置规则。
39. In NeoForge Sophisticated screens, empty-slot single clicks may be recovered through the low-level press entry when other GUI/input mods suppress higher-level events. Empty-slot drag marking is not guaranteed because it depends on Mouse Tweaks emitting slot-enter samples for those Sophisticated empty slots.
40. 在 NeoForge 的 Sophisticated 系列界面中，如果其他 GUI/输入模组压掉高层事件，空槽单击可由低层按下入口恢复。空槽拖动标记不作保证，因为它依赖 Mouse Tweaks 是否为这些 Sophisticated 空槽发出槽位进入采样。

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

1. Server/world start initializes the persistence context without preloading every player file.
2. 服务端/世界启动时只初始化持久化上下文，不全量预载所有玩家文件。
3. Player login loads only that player's UUID into the server cache and then sends a full sync.
4. 玩家进入世界时，只读取该玩家 UUID 的数据到服务端缓存，然后发送全量同步。
5. Favorite changes during play update the server cache without immediately writing the player file.
6. 游戏过程中的收藏变更只更新服务端缓存，不立即写玩家文件。
7. Player logout performs an incremental save for that player's current state while keeping the cache entry available.
8. 玩家退出世界时，对该玩家当前状态执行增量保存，同时保留缓存项。
9. Server/world stop flushes online-player state and then writes the full cache back to disk.
10. 服务端/世界关闭时，会先收集在线玩家状态，再把完整缓存回写到磁盘。
11. Client-only multiplayer uses `favoriteitems/<sanitized-server-address>/players/<uuid>.dat`; if the server list entry is not available yet, the client falls back to the active connection remote address before using the default namespace.
12. 仅客户端联机模式使用 `favoriteitems/<净化后的服务器地址>/players/<uuid>.dat`；如果服务器列表条目暂不可用，客户端会先回退到当前连接远端地址，最后才使用默认命名空间。
13. Dual-install singleplayer and dedicated-server modes use `<world>/data/neo_favorite_items/players/<uuid>.dat`.
14. 双端安装下的单人与多人服务端模式统一使用 `<世界目录>/data/neo_favorite_items/players/<uuid>.dat`。
15. If a previous build wrote server-authoritative data under `<game>/data/neo_favorite_items/players/<uuid>.dat`, the first successful per-player load migrates it to the active world directory and deletes the old file.
16. 如果旧版本曾把服务端权威数据写入 `<游戏目录>/data/neo_favorite_items/players/<uuid>.dat`，首次成功按玩家读取时会迁移到当前世界目录并删除旧文件。

### Death And Respawn

### 死亡与重生

1. Loader player-clone/copy events route death lifecycle handling into `PlatformFavoriteSupport`.
2. 三个平台的玩家 clone/copy 事件会把死亡生命周期处理路由到 `PlatformFavoriteSupport`。
3. When `keepInventory=true`, the new player inventory is restored from the old player inventory under a scoped server inventory-guard bypass. This prevents lock guards from treating vanilla respawn restoration as a user item move.
4. `keepInventory=true` 时，新玩家背包会在有作用域的服务端背包守卫绕过上下文中从旧玩家背包恢复，避免锁槽守卫把原版重生恢复误判为玩家物品移动。
5. When `keepInventory=false`, favorite state is cleared and saved for that player, because the locked items have left the player inventory through death drops.
6. `keepInventory=false` 时，该玩家收藏状态会被清空并保存，因为锁定物品已经通过死亡掉落离开玩家背包。

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
5. Common code builds an `OverlayRenderDescriptor` from favorite state, lock-operation key state, bypass-key state, and config.
6. common 代码根据收藏状态、锁定操作键、旁路键和配置构建 `OverlayRenderDescriptor`。
7. PNG textures are sampled at their actual dimensions and scaled to 16x16 slots.
8. PNG 材质按实际尺寸采样并缩放到 16x16 槽位。
9. Config decides whether overlays render in front of or behind item icons.
10. 根据配置决定绘制在物品图标前方或后方。
11. ModernUI rounded tooltips draw their shadow as part of the tooltip render state, so slot overlays rendered before tooltip submission are intentionally covered by the tooltip bounds and shadow spread.
12. ModernUI 圆角 tooltip 会把阴影作为 tooltip 渲染状态的一部分绘制，因此在 tooltip 提交前绘制的槽位 Overlay 会被 tooltip 范围和阴影扩散正常覆盖。

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
- Automated coverage details belong in `30-report-tests.md`.
- 自动化覆盖细节归入 `30-report-tests.md`。
- Runtime validation status belongs in `31-report-verification.md`.
- 运行时验证状态归入 `31-report-verification.md`。
- AE2 compatibility is intentionally common-abstraction based, and sorter compatibility should continue to use each sorter mod's locked-slot concept where available.
- AE2 兼容刻意基于公共抽象层实现；整理兼容应继续优先复用整理模组自身的锁槽概念。
- NeoForge Sophisticated empty-slot drag marking is a known compatibility limitation in modpacks where Mouse Tweaks does not emit drag-enter samples for those empty slots; single-click empty-slot toggling remains supported.
- NeoForge 的 Sophisticated 空槽拖动标记在 Mouse Tweaks 不为这些空槽发出拖动进入采样的整合包中属于已知兼容限制；空槽单击切换仍受支持。

## Maintenance Rules

## 维护约定

- Prefer putting new logic in `common`; keep loader layers as thin adapters.
- 新逻辑优先放入 `common`，平台层保持薄适配。
- Persistence and networking should pass logical slots or player inventory indices, not GUI slot ids.
- 持久化和网络只传递逻辑槽位或玩家背包索引，不传递 GUI slot id。
- Prefer safe, semantic API boundaries before adding mod-specific compatibility. Do not globally hide `getItem` reads unless a future fix proves the synchronization impact is acceptable.
- 优先选择安全的语义 API 边界，再考虑模组特定兼容；除非后续修复能证明同步影响可控，否则不要全局隐藏 `getItem` 读取。
- Prefer sorter locked-slot integration over replacing third-party container insertion algorithms. Third-party storage/container-owned slots must stay outside NeoFavoriteItems' lock enforcement unless they are resolved to the player's own `Inventory`.
- 优先通过整理模组自身的锁槽机制做兼容，避免替换第三方容器插入算法。第三方存储/容器自身槽位必须保持在 NeoFavoriteItems 锁定约束之外，除非能解析为玩家自己的 `Inventory`。
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
