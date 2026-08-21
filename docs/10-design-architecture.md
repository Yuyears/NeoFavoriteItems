# Neo Favorite Items Architecture Notes

# Neo Favorite Items 架构说明

Last updated: 2026-08-16

最后更新：2026-08-16

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
  src/main/java/.../fabric/mixin/compat/
    Optional third-party compatibility mixins
    可选第三方软联动 Mixin

forge/
  Forge entrypoint, network payloads, mixins, Forge events/key bindings/renderer
  Forge 入口、网络 payload、Mixin、Forge 事件/按键/渲染器
  src/main/java/.../forge/mixin/compat/
    Optional third-party compatibility mixins
    可选第三方软联动 Mixin

neoforge/
  NeoForge entrypoint, network payloads, mixins, NeoForge events/key bindings/renderer
  NeoForge 入口、网络 payload、Mixin、NeoForge 事件/按键/渲染器
  src/main/java/.../neoforge/mixin/compat/
    Optional third-party compatibility mixins
    可选第三方软联动 Mixin
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
- `InteractionGuardService.shouldCancelSwap`: centralizes the same two-endpoint source/target decision used by the server and all three clients.
- `InteractionGuardService.shouldCancelSwap`：集中服务端与三端客户端共用的双端来源/目标交换判定。
- `ServerFavoriteService`: handles server-side toggles, validation, revisions, bypass state, and server interaction protection
- `ServerFavoriteService`：处理服务端收藏切换、校验、修订号、旁路状态和服务端交互保护
- Rejected client container clicks, offhand-swap packets, and creative-slot-set packets trigger an authoritative full favorite sync through a loader-registered sender. Lower-level inventory and third-party server mutations do not trigger correction sync. Rejections for the same player in the same server tick are coalesced.
- 客户端容器点击、副手交换包或创造槽写入包被拒绝时，通过加载器注册的发送器回传权威全量锁状态；低层库存变更与第三方服务端逻辑拒绝不触发纠正同步。同一玩家同一服务端 tick 内的重复拒绝会被合并。
- `ScopedPlayerOperationService`: owns nestable per-thread player operation scopes used by server inventory guard bypass and death-drop preservation
- `ScopedPlayerOperationService`：管理可嵌套的逐线程玩家操作作用域，供服务端库存守卫旁路和死亡掉落保留使用
- `LockedEmptySlotFallback`: selects the safe fallback slot for incoming stacks blocked by a locked empty target, preferring hotbar slots before main-inventory slots
- `LockedEmptySlotFallback`：为被锁定空目标槽拒绝的放入物品选择安全回退槽，优先快捷栏，其次主背包
- `ClientFavoriteSyncService`: applies full and incremental syncs, rejects stale revisions, and detects revision gaps
- `ClientFavoriteSyncService`：应用客户端全量/增量同步、过滤过期修订并检测同步缺口
- `ClientDropGuard`: decides whether a selected hotbar stack drop should be blocked before the client plays drop animation
- `ClientDropGuard`：决定是否应在客户端播放丢弃动画前阻止当前手持快捷栏物品被丢弃
- `InventorySortingCompatService`: merges favorite player-inventory slots into third-party sorter locked-slot lists before sorters clear and rewrite inventory ranges; it also owns the common favorite-state swap helper used by Quark's hotbar changer compatibility
- `InventorySortingCompatService`：在第三方整理模组清空并重写排序范围前，把已收藏玩家背包槽合并进整理模组的锁槽列表；同时提供 Quark 快捷栏切换兼容所用的收藏状态交换 helper
- `ClientSortCompatService`: rewrites ClientSort collect/sort/transfer/stack-fill arrays at both ClientSort schema-validation and operation entrypoints. Collect filters locked slots including locked empty slots. Transfer and stack fill filter locked source and target arrays because ClientSort uses destination `safeInsert(srcStack)`, which can shrink the source stack without calling source-slot removal APIs. Sort preserves ClientSort's target-order semantics: it fixes locked slots, filters locked slots out of the sorted-source list and target-slot list, then pairs the remaining sources and targets in ClientSort's original target order so unlocked items stay compact and continuous around locked holes. It emits debug diagnostics only when a ClientSort array is actually rewritten.
- `ClientSortCompatService`：在 ClientSort schema 校验和实际操作入口改写 collect/sort/transfer/stack-fill 数组。collect 会过滤锁定槽（包含锁定空槽）。transfer 与 stack fill 会同时过滤锁定来源数组和目标数组，因为 ClientSort 使用目标槽 `safeInsert(srcStack)`，可能不经过来源槽移除 API 就缩减来源 stack。sort 会保留 ClientSort 的目标顺序语义：固定锁槽，把锁槽分别从已排序来源列表和目标槽列表中剔除，再按 ClientSort 原目标顺序重新配对剩余来源与目标，使未锁物品围绕锁定空洞保持紧凑连续。只有实际改写 ClientSort 数组时才输出 debug 诊断。
- `BetterExperienceCompatService`: detects whether Better Experience fast storage is about to use a locked player-inventory stack as its source, without replacing the mod's container search or insertion behavior.
- `BetterExperienceCompatService`：只检测 Better Experience 一键存储是否正要把已锁玩家背包 stack 作为来源，不替换该模组的容器搜索或放入逻辑。
- `InstantSwapCompatService`: defines Su's Instant Swap pass/block/lock-follow decisions and creates immutable operation plans containing the exact click sequence, favorite cycle, and menu-slot cycle for SWAP, PICKUP exchange, and hotbar-priority stash.
- `InstantSwapCompatService`：定义 Su's Instant Swap 的放行、阻止、锁跟随决策，并为 SWAP、PICKUP 交换和热栏优先暂存生成包含精确点击序列、锁循环与菜单槽循环的不可变操作计划。
- Su modifier arbitration is a single pure mode: `NONE`, `BYPASS`, or `MOVE_LOCKS`. If Ctrl and Alt overlap, most recently pressed modifier wins; no operation can enter both paths.
- Su 修饰键仲裁使用单一纯模式：`NONE`、`BYPASS` 或 `MOVE_LOCKS`。Ctrl 与 Alt 重叠时以后按下者为准，单次操作不会同时进入两条路径。

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
- `FavoritesManager` keeps client-local/synchronized state separate from UUID-keyed server state. Client code explicitly selects client state; server code selects a player UUID. This also applies to integrated servers where both sides use the same UUID.
- `FavoritesManager` 将客户端本地/同步状态与按 UUID 保存的服务端状态分离。客户端显式选择客户端状态，服务端选择玩家 UUID；集成服务端双端 UUID 相同时也不会共用集合。
- It supports client-local storage namespaces keyed by server address, world-save storage for integrated/dedicated servers, legacy client-path fallback, migration from the old game-directory server root, per-player login reads, cache-only in-play updates, and full-cache server-stop flushes.
- 支持按服务器地址分命名空间的客户端本地存储、单人/专用服务端世界存档目录、旧客户端路径兼容回退、从旧游戏根目录服务端路径迁移、玩家登录按 UUID 读取、游戏过程仅更新缓存，以及关服完整缓存回写。

### Config

### 配置层

- `ConfigManager` loads split common/client config files into one `NeoFavoriteItemsConfig`, records malformed lines, unknown entries, and invalid values, then rewrites the affected file when repair is needed.
- `ConfigManager` 会把拆分后的 common/client 配置文件读取到同一个 `NeoFavoriteItemsConfig`，记录格式错误行、未知配置项和非法值，并在需要修复时重写受影响的文件。
- Generated config files carry paired English and Simplified Chinese comments for behavior-sensitive options, including locked-empty-slot pickup, bypass key scope, slot movement, death preservation, and client-only presentation settings.
- 生成的配置文件会为行为敏感配置提供中英双语注释，包括锁定空槽拾取、旁路键作用范围、槽位移动、死亡保留以及仅客户端表现配置。
- `neo-favorite-items-common.toml` contains server-authoritative or rule-affecting sections: `general`, `lockBehavior`, `slotBehavior`, `deathBehavior`, and `debug`.
- `neo-favorite-items-common.toml` 包含服务端权威或影响规则的配置段：`general`、`lockBehavior`、`slotBehavior`、`deathBehavior` 与 `debug`。
- `neo-favorite-items-client.toml` contains client-preferred presentation and feedback sections: `overlay` and `feedback`.
- `neo-favorite-items-client.toml` 包含客户端优先生效的显示与反馈配置段：`overlay` 与 `feedback`。
- Legacy `neo-favorite-items.toml` is read only as a one-time migration source when either split file is missing, then deleted after the split files are written successfully.
- 旧版 `neo-favorite-items.toml` 只在任一拆分文件缺失时作为一次性迁移来源读取；拆分文件成功写出后会删除旧文件。
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
- Optional compatibility mixins are isolated in non-required compat configs and the per-loader `mixin/compat` packages. Compatibility mixin classes use the `<ModOrFeature><TargetOrScenario>CompatMixin` naming pattern, such as `Ae2MenuCompatMixin`, `MouseTweaksMainCompatMixin`, and `ClientSortSortHandlerCompatMixin`. AE2 compatibility targets shared abstractions (`AEBaseMenu` and `MEStorageMenu`) and avoids early `ModList`/class-presence decisions, following the Mouse Tweaks-style runtime mixin application pattern.
- 可选兼容 Mixin 隔离在非 required 的 compat 配置和各平台 `mixin/compat` 包中。兼容 Mixin 类使用 `<模组或功能><目标或场景>CompatMixin` 命名模式，例如 `Ae2MenuCompatMixin`、`MouseTweaksMainCompatMixin` 与 `ClientSortSortHandlerCompatMixin`。AE2 兼容目标限定在公共抽象（`AEBaseMenu` 与 `MEStorageMenu`），并避免过早依赖 `ModList`/类存在性判断，采用更接近 Mouse Tweaks 的运行时 Mixin 应用方式。
- NeoForge Su's Instant Swap compatibility targets its three `SwapEngine` click emitters. Unlocked and bypassed operations remain upstream-owned. With the lock-operation key held, locked pairs cancel the upstream click and send one validated operation request; the server executes the complete click plan in one task, verifies the resulting stack cycle, moves favorite state, increments the revision, and sends authoritative sync. Client-only installations retain local post-state validation.
- Creative-mode Su's Instant Swap uses separate direct client-inventory mutation paths. Default swaps skip locked pairs, bypass remains upstream-owned, and the lock-operation key moves lock pairs after the upstream mutation. Creative item updates and lock moves are separate packets, so corrective full sync limits ghost-lock persistence but does not make this path atomic.
- Su's Instant Swap 的创造模式使用独立的客户端物品栏直接修改路径。默认交换跳过锁定对应组，旁路仍由上游执行，锁定键在上游变更后移动对应锁。创造物品更新与锁移动使用独立数据包，因此纠正全量同步只能缩短幽灵锁持续时间，不能使该路径原子化。
- NeoForge 的 Su's Instant Swap 兼容只作用于 `SwapEngine` 的三个点击发射器。未锁和旁路操作完全保留上游实现，包括整行校验、背包 PICKUP 交换和热栏优先暂存。按住锁定键时，只有经过校验的槽循环至少包含一个服务端权威锁槽，才会准备五 tick 服务端事务。事务只授权精确匹配的原版点击，PICKUP 序列还要求空光标；只有实际 stack 符合预期两槽或三槽循环时才提交锁状态。授权或 post-state 校验失败时，完成 payload 会请求全量纠正。仅客户端安装时也执行相同 post-state 校验。
- 生存 Alt 与 Ctrl 路径完全分离：Ctrl 只调用上游交换并保持锁位置；Alt 取消上游点击，由服务端执行本次完整交换并在 scoped bypass 内移动锁。两者只共享纯槽位映射与操作计划 helper，不共享事务状态。
- 创造 Alt 使用独立服务端 pair/row 批次，在服务端旁路保护下同时交换物品和锁；创造 Ctrl 保持上游客户端路径。创造物品更新不再与后续锁移动包竞争。

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
- Wrench Finder compatibility shim removed after the upstream mod fixed its locked-source duplication bug; standard server-side inventory guards remain authoritative.
- Wrench Finder 兼容临时层已在上游修复锁定来源复制问题后移除；标准服务端库存守卫继续作为权威保护。

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
   Menu `SWAP` checks its hotbar/offhand player endpoint before filtering container-owned clicked slots. Container slots remain unlocked, but they cannot be exchanged with a protected player slot unless bypass is active.
   菜单 `SWAP` 会在过滤容器自有点击槽前检查快捷栏/副手玩家端。容器槽本身仍不受锁定，但未启用旁路时不能与受保护玩家槽交换。
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
25. ClientSort compatibility rewrites ClientSort slot arrays at validation and operation boundaries instead of globally bypassing `mayPickup`, `mayPlace`, or `canPlaceItem`: collect requests drop locked player-inventory menu slots, including empty locked slots that ClientSort probes with a synthetic placement item; transfer and stack-fill requests drop locked slots from both source and target arrays before ClientSort can shrink a source stack through destination `safeInsert`; sort requests preserve ClientSort's one-to-one mapping by fixing locked slots and re-pairing the remaining sorted sources with remaining targets in ClientSort's target order. This keeps ClientSort's own schema validation satisfied without freezing the whole inventory when one large sort cycle contains a locked slot, without leaving avoidable gaps in unlocked target slots, and without letting ClientSort move locked stacks.
26. ClientSort 兼容会在 ClientSort 校验和操作边界改写槽位数组，而不是全局旁路 `mayPickup`、`mayPlace` 或 `canPlaceItem`：collect 请求会移除锁定玩家背包 menu slot，包括 ClientSort 使用合成测试物品探测的锁定空槽；transfer 与 stack-fill 请求会在 ClientSort 通过目标槽 `safeInsert` 缩减来源 stack 前，从来源和目标数组同时移除锁定槽；sort 请求会固定锁槽，并按 ClientSort 的目标顺序把剩余已排序来源重新配对到剩余目标槽。这样既满足 ClientSort 自身 schema 校验，也避免一个大型排序 cycle 含有锁槽时冻结整个背包整理，避免未锁目标槽出现可避免的空洞，并且不会让 ClientSort 移动锁槽物品。
27. Better Experience fast-storage compatibility is intentionally narrow: it only skips Better Experience's per-stack transfer call when the source `ItemStack` object is the same object stored in a locked player-inventory slot. Nearby container discovery and target insertion remain Better Experience behavior.
28. Better Experience 一键存储兼容刻意保持窄范围：只有当来源 `ItemStack` 对象就是已锁玩家背包槽内的同一对象时，才跳过 Better Experience 的单次 stack 转移调用。附近容器发现与目标放入仍保持 Better Experience 原逻辑。
29. The guard deliberately does not hide `Slot.getItem`, `Inventory.getItem`, item-handler `getStackInSlot`, or item-handler slot-limit reads, because read interception can break menu synchronization, rendering, and third-party inspection logic.
30. 守卫刻意不隐藏 `Slot.getItem`、`Inventory.getItem`、item-handler `getStackInSlot` 或 item-handler 槽位上限读取，因为读取拦截可能破坏菜单同步、渲染和第三方检查逻辑。
31. Direct external writes into a locked empty player slot are not silently canceled after the source item has already been removed. Dedicated `Inventory.setItem` and main-hand `Player.setItemInHand` reroute entries move the incoming stack to the first empty unlocked hotbar slot, then the first empty unlocked main-inventory slot, and drop it when no fallback slot exists.
32. 对已锁定空玩家槽的外部直接写入不会在来源物品已被移除后静默取消。专用的 `Inventory.setItem` 与主手 `Player.setItemInHand` 回退入口会把 incoming stack 改放到第一个空且未锁定的快捷栏槽，再改放到第一个空且未锁定的主背包槽；没有回退槽时掉落。
33. Ground-item pickup and other vanilla `Inventory.add(...)` paths do not wait until `setItem` to reject locked empty slots. Instead, `Inventory.getFreeSlot()` is adjusted on the server so locked empty main-inventory slots are skipped when `allowItemsIntoLockedEmptySlots=false`; vanilla then inserts into the next available unlocked empty slot, or keeps the incoming stack unconsumed when no valid free slot exists.
34. 地面掉落物拾取和其他原版 `Inventory.add(...)` 路径不会等到 `setItem` 阶段才拒绝锁定空槽。服务端会修正 `Inventory.getFreeSlot()`，使 `allowItemsIntoLockedEmptySlots=false` 时跳过已锁定的空主背包槽；随后原版会把物品放入下一个可用未锁空槽，若没有有效空槽则保留 incoming stack 不被消耗。
35. In-place stack growth is guarded at both vanilla merge selectors: `AbstractContainerMenu.moveItemStackTo` treats locked occupied player slots as full, while `Inventory.getSlotWithRemainingSpace` preserves vanilla selected/offhand/main-inventory order but skips locked occupied targets.
36. 原地堆叠增长在两个原版合并选择点受保护：`AbstractContainerMenu.moveItemStackTo` 将锁定非空玩家槽视为已满；`Inventory.getSlotWithRemainingSpace` 保持原版“当前槽、副手、主背包”顺序，但跳过锁定非空目标。
35. GUI cursor placement and normal `Slot` APIs remain pure guards: `mayPlace`, `safeInsert`, `set`, and `setByPlayer` reject locked empty slots without rerouting the carried stack. This preserves the expected locked-slot experience and avoids duplicating cursor stacks.
36. GUI 光标放入和普通 `Slot` API 仍保持纯拦截：`mayPlace`、`safeInsert`、`set` 与 `setByPlayer` 会拒绝锁定空槽，但不会改道光标物品。这样保留锁槽体验，并避免复制光标物品。
37. NeoForge GUI mouse guarding uses official `ScreenEvent.MouseButtonPressed.Pre` and `ScreenEvent.MouseButtonReleased.Pre` hooks. The release hook covers creative inventory cursor placement, where vanilla can defer the `PICKUP` slot action until mouse release.
38. NeoForge GUI 鼠标守卫使用官方 `ScreenEvent.MouseButtonPressed.Pre` 与 `ScreenEvent.MouseButtonReleased.Pre` 事件。释放阶段守卫覆盖创造物品栏光标放入路径，因为原版可能把 `PICKUP` 槽位动作延迟到鼠标释放时执行。
39. Bypass-key state is polled on the client and synced to the server.
40. 旁路键状态由客户端按键轮询同步到服务端。
41. When the lock-operation key is held, the state machine has explicit exits: `beginPress` activates the operation and toggles the press target, `toggleEnteredSlot` handles Mouse Tweaks drag-enter targets, `toggleLeakedSlotClick` handles Sophisticated leaked click actions, and `consumeActiveDrag` only suppresses leaked vanilla drag while active.
42. 按住锁定操作键时，状态机有明确出口：`beginPress` 激活操作并切换按下目标，`toggleEnteredSlot` 处理 Mouse Tweaks 拖动进入目标，`toggleLeakedSlotClick` 处理 Sophisticated 漏出的点击动作，`consumeActiveDrag` 只在 active 时吞掉漏出的原版拖动。
43. Mouse Tweaks compatibility on all three loaders refreshes Mouse Tweaks' screen bookkeeping, borrows its `getSlotUnderMouse` and `oldSelectedSlot` slot-enter detection, then consumes the Alt drag event before Mouse Tweaks can run its own click semantics. Newly entered slots are handed to this mod's lock-operation toggle exit, keeping Mouse Tweaks optional without inheriting its empty-slot, carried-stack, shift, or config rules.
44. 三个平台的 Mouse Tweaks 兼容都会先刷新 Mouse Tweaks 的界面账本，借用它的 `getSlotUnderMouse` 与 `oldSelectedSlot` 槽位进入检测，然后在 Mouse Tweaks 执行自身点击语义前消费 Alt 拖动事件。新进入的槽位会交给本模组的锁定操作切换出口，因此 Mouse Tweaks 仍是可选兼容，但不会继承它的空槽、光标物品、Shift 或配置规则。
45. In NeoForge Sophisticated screens, empty-slot single clicks may be recovered through the low-level press entry when other GUI/input mods suppress higher-level events. Empty-slot drag marking is not guaranteed because it depends on Mouse Tweaks emitting slot-enter samples for those Sophisticated empty slots.
46. 在 NeoForge 的 Sophisticated 系列界面中，如果其他 GUI/输入模组压掉高层事件，空槽单击可由低层按下入口恢复。空槽拖动标记不作保证，因为它依赖 Mouse Tweaks 是否为这些 Sophisticated 空槽发出槽位进入采样。

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
3. Loader `ServerPlayer.restoreFrom` mixins ask `ServerFavoriteService` whether the respawn inventory-copy scope should be treated as an internal inventory operation before vanilla copies items.
4. 三个平台的 `ServerPlayer.restoreFrom` Mixin 会在原版复制物品前询问 `ServerFavoriteService`，判断本次重生库存复制是否应被视为内部库存操作。
5. The bypass policy combines runtime preservation state and the death config: it is enabled only on the server, and only when `keepEverything`, the world's `keepInventory` gamerule, or `deathBehavior.preserveLockedSlotContents` indicates that some inventory content should survive.
6. 绕过策略会综合运行时保留状态与死亡配置：仅服务端、且 `keepEverything`、世界规则 `keepInventory` 或 `deathBehavior.preserveLockedSlotContents` 表明存在需要保留的背包内容时才启用。
7. With vanilla inventory preservation (`keepInventory=true` or `keepEverything=true`), vanilla restoration and the loader clone/copy fallback run under the same scoped guard bypass and restore the full old inventory.
8. 原版背包保留（`keepInventory=true` 或 `keepEverything=true`）时，原版恢复与平台 clone/copy 兜底恢复都会处于同一个作用域守卫绕过中，并恢复完整旧背包。
9. With `deathBehavior.preserveLockedSlotContents=true` and `keepInventory=false`, the death-drop scope is opened around `Player.dropEquipment` so it covers the player override's `Inventory.dropAll()` call. Locked non-empty player-inventory stacks are temporarily removed before vanilla `Inventory.dropAll()` runs, then restored after it returns, so vanilla and loader death-drop events still see their normal flow while locked stacks remain available for clone/copy restoration.
10. 当 `deathBehavior.preserveLockedSlotContents=true` 且 `keepInventory=false` 时，死亡掉落作用域会包住 `Player.dropEquipment`，确保覆盖玩家 override 中的 `Inventory.dropAll()` 调用。非空锁定玩家背包栈会在原版 `Inventory.dropAll()` 执行前被临时移出，并在返回后恢复，因此原版与加载器死亡掉落事件仍保持正常流程，同时锁定栈仍可供 clone/copy 恢复。
11. When runtime state does not preserve inventory and the death config is disabled, no bypass or death-drop preservation marker is applied; favorite state is cleared and saved for that player, because the locked items have left the player inventory through death drops.
12. 运行时不保留背包且死亡配置关闭时，不会打绕过或死亡掉落保留标识；该玩家收藏状态会被清空并保存，因为锁定物品已经通过死亡掉落离开玩家背包。

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
- The inspected nearby-container quick-stack behavior for the Confluence modpack is provided by the Better Experience addon, so compatibility is attached to Better Experience's fast-storage source stack boundary.
- 当前排查到的汇流整合包附近容器一键存储行为由附属 Better Experience 提供，因此兼容挂在 Better Experience 一键存储的来源 stack 边界。

## Maintenance Rules

## 维护约定

- Prefer putting new logic in `common`; keep loader layers as thin adapters.
- 新逻辑优先放入 `common`，平台层保持薄适配。
- Loader-neutral core mixins live under `common/.../mixin` and are registered by `neo_favorite_items.common.mixins.json`. Loader configs retain client hooks and true platform adapters.
- 平台无关核心 Mixin 位于 `common/.../mixin`，由 `neo_favorite_items.common.mixins.json` 注册；各平台配置只保留客户端钩子与真实平台适配。
- Keep optional compatibility mixins loader-local while their `IMixinConfigPlugin` ownership or target availability checks differ. Consolidate only when source, mappings, dependencies, and loading policy all match.
- 可选兼容 Mixin 的 `IMixinConfigPlugin` 归属或目标存在性检查不同时继续留在平台层；只有源码、映射、依赖和加载策略均一致时才合并。
- Do not add or package MixinExtras solely to replace an isolated vanilla Mixin injection. Adopt it across loaders only after dependency versions converge or measured redirect conflicts outweigh the new runtime dependency.
- 不为替换单个原生 Mixin 注入而新增或打包 MixinExtras；只有三端依赖版本收敛，或已测得的 Redirect 冲突收益超过新增运行时依赖成本时才采用。
- Persistence and networking should pass logical slots or player inventory indices, not GUI slot ids.
- 持久化和网络只传递逻辑槽位或玩家背包索引，不传递 GUI slot id。
- Prefer safe, semantic API boundaries before adding mod-specific compatibility. Do not globally hide `getItem` reads unless a future fix proves the synchronization impact is acceptable.
- 优先选择安全的语义 API 边界，再考虑模组特定兼容；除非后续修复能证明同步影响可控，否则不要全局隐藏 `getItem` 读取。
- Prefer sorter locked-slot integration over replacing third-party container insertion algorithms. Third-party storage/container-owned slots must stay outside NeoFavoriteItems' lock enforcement unless they are resolved to the player's own `Inventory`.
- 优先通过整理模组自身的锁槽机制做兼容，避免替换第三方容器插入算法。第三方存储/容器自身槽位必须保持在 NeoFavoriteItems 锁定约束之外，除非能解析为玩家自己的 `Inventory`。
- Put third-party compatibility mixins under the loader's `mixin/compat` package, register them only in the non-required `*.compat.mixins.json`, and name them with the `<ModOrFeature><TargetOrScenario>CompatMixin` pattern.
- 第三方兼容 Mixin 必须放在对应平台的 `mixin/compat` 包中，只注册到非 required 的 `*.compat.mixins.json`，并使用 `<模组或功能><目标或场景>CompatMixin` 命名模式。
- In Forge/NeoForge compat mixin plugins, prefer `LoadingModList.get().getModFileById(modId)` during early loading and use `ModList.get().isLoaded(modId)` only as a later fallback. Fabric compat can use Fabric Loader or class-provider presence checks.
- Forge/NeoForge 兼容 Mixin plugin 中，早期加载阶段优先使用 `LoadingModList.get().getModFileById(modId)`，只把 `ModList.get().isLoaded(modId)` 作为后续回退。Fabric 兼容可使用 Fabric Loader 或 class-provider 存在性检查。
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
