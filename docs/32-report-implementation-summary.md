# Implementation Summary

# 实现摘要

Last updated: 2026-08-16

最后更新：2026-08-16

## Purpose

## 用途

- This document records completed implementation scope. It is historical project status, not the active backlog.
- 本文档记录已完成的实现范围。它属于历史项目状态，不作为当前待办列表。

## Completed

## 已完成

- Multi-module Gradle Kotlin DSL project structure: `common`, `fabric`, `forge`, `neoforge`
- 多模块 Gradle Kotlin DSL 项目结构：`common`、`fabric`、`forge`、`neoforge`
- Minecraft 1.21.1 / Java 21 / Architectury Loom build foundation
- Minecraft 1.21.1 / Java 21 / Architectury Loom 构建基础
- Unified logical slot model: `LogicalSlotIndex`
- 统一逻辑槽位模型：`LogicalSlotIndex`
- Config system with English and Simplified Chinese comments
- 配置系统和中英文配置注释
- Config files are split into `neo-favorite-items-common.toml` for server/rule-affecting options and `neo-favorite-items-client.toml` for client presentation/feedback options; legacy `neo-favorite-items.toml` is migrated once and deleted after successful split-file generation.
- 配置文件已拆分为服务端/规则项使用的 `neo-favorite-items-common.toml` 与客户端显示/反馈项使用的 `neo-favorite-items-client.toml`；旧 `neo-favorite-items.toml` 会一次性迁移，并在拆分文件成功生成后删除。
- Generated config templates now include paired English and Simplified Chinese explanations for behavior-sensitive options such as locked-empty-slot pickup, bypass-key scope, slot movement, death preservation, and client-only presentation settings.
- 生成的配置模板现在为行为敏感选项补充了中英双语说明，例如锁定空槽拾取、旁路键作用范围、槽位移动、死亡保留和仅客户端表现设置。
- Favorite state management and basic persistence
- 收藏状态管理和基础持久化
- Fabric, Forge, and NeoForge entrypoints with client key binding registration
- Fabric、Forge、NeoForge 入口与客户端按键注册
- Overlay render entrypoints and hotbar HUD overlays for all three loaders
- 三个平台 Overlay 渲染入口和快捷栏 HUD Overlay
- 三端 HUD 快捷栏锁 Overlay 在 GUI 打开期间保持显示，并与 GUI 槽位 Overlay 共用当前客户端锁状态。
- Nine PNG overlay textures and configurable color rendering
- 九种 Overlay PNG 材质和配置色渲染
- Unified interaction decisions through `InteractionGuardService`
- `InteractionGuardService` 统一交互决策
- Client polling for lock-operation and bypass-key states
- 客户端锁定操作键、旁路键状态轮询
- Server-side toggle handling, revisions, full/incremental sync, and bypass-key state sync
- 服务端收藏切换、修订号、全量/增量同步和旁路键状态同步
- 服务端拒绝客户端容器点击、副手交换包或创造槽写入包时，发送按玩家、按 tick 合并的纠正全量同步；低层库存与第三方服务端变更拒绝不触发该同步。
- Su 兼容层现使用独立 ModifierMode 仲裁 Ctrl/Alt；生存 Alt 与创造 Alt 各自使用服务端操作入口，Ctrl 不进入锁移动或服务端事务路径。
- Player login/logout load-save flow for all three loaders
- 三个平台玩家登录/登出加载保存流程
- Mouse Tweaks-style drag-click lock toggling now uses the same optional compatibility pattern on Fabric, Forge, and NeoForge: Mouse Tweaks' slot-enter detection is reused, newly entered slots are forwarded to this mod's toggle path, and `slotClicked` hooks only cancel leaked inventory clicks while the lock-operation key is held.
- Fabric、Forge、NeoForge 的 Mouse Tweaks 风格拖动点击切换锁定现在使用同一套可选兼容模式：复用 Mouse Tweaks 的槽位进入检测，把新进入的槽位转发到本模组的切换路径，`slotClicked` 钩子只取消按住锁定操作键时漏进来的库存点击。
- Composite-move guards for offhand swaps and quick-moving equipment into locked armor/offhand targets
- 针对副手交换和 Shift 点击装备进入锁定护甲/副手目标槽的复合移动保护
- Automated interaction-decision tests for locked incoming offhand and armor targets
- 为锁定副手/护甲放入目标补充自动化交互决策测试
- Generic slot-level guards for standard `Slot` mutation APIs: `safeInsert`, `safeTake`, `tryRemove`, `remove`, `set`, and `setByPlayer`
- 为标准 `Slot` 变更 API 补充通用槽位级守卫：`safeInsert`、`safeTake`、`tryRemove`、`remove`、`set` 和 `setByPlayer`
- Conditional AE2 shared-menu compatibility for terminal `MOVE_REGION` transfers through `AEBaseMenu` and `MEStorageMenu`
- 通过 `AEBaseMenu` 与 `MEStorageMenu` 补充条件加载的 AE2 公共菜单层兼容，处理终端 `MOVE_REGION` 转移
- NeoForge dedicated-server installation modes validated: client-only, server-only, and both-sides-installed.
- NeoForge 真实专用服务端安装模式已验证：仅客户端、仅服务端、双端均安装。
- NeoForge interaction validation completed for normal drop, bypass-key drop, GUI/GUI-outside offhand swaps with locked empty and non-empty offhand slots, shift-click equippable armor into locked empty armor slots, and AE2 terminal space-left-click `MOVE_REGION` into and out of locked player inventory slots.
- NeoForge 交互实机验证已覆盖：普通丢弃、按住旁路键丢弃、GUI 内外副手交换（覆盖锁定空副手槽和锁定非空副手槽）、锁定空护甲槽 Shift 点击可装备护甲，以及 AE2 终端空格+左键 `MOVE_REGION` 对锁定玩家背包槽的放入与取出。
- Gradle result-copy task no longer breaks focused `:common:test` runs under configuration-on-demand.
- Gradle 构建结果复制任务不再破坏 configure-on-demand 下聚焦执行的 `:common:test`。
- Death lifecycle handling now marks vanilla `ServerPlayer.restoreFrom` inventory restoration as an internal scoped operation whenever `keepEverything`, the world `keepInventory` gamerule, or `[deathBehavior] preserveLockedSlotContents` requires inventory preservation. The death-drop preservation marker is opened around `Player.dropEquipment`, covering the player inventory `dropAll()` call; locked stacks are temporarily removed before vanilla `dropAll()` and restored afterward so third-party death-drop listeners stay on the normal loader event path while the original player still carries the locked stacks for respawn restoration.
- 死亡生命周期处理现在会在 `keepEverything`、世界规则 `keepInventory` 或 `[deathBehavior] preserveLockedSlotContents` 要求保留背包内容时，把原版 `ServerPlayer.restoreFrom` 背包恢复标记为内部作用域操作。死亡掉落保留标识会包住 `Player.dropEquipment`，覆盖玩家背包 `dropAll()` 调用；锁定栈会在原版 `dropAll()` 前被临时移出并在之后恢复，使第三方死亡掉落监听器保持在正常加载器事件路径上，同时原玩家仍携带锁定栈供重生恢复。
- Scoped player operation bookkeeping has been extracted into `ScopedPlayerOperationService`, leaving `ServerFavoriteService` as the stable server-facing facade used by loader mixins and networking code.
- 玩家操作作用域计数已抽入 `ScopedPlayerOperationService`，`ServerFavoriteService` 继续作为平台 Mixin 与网络代码使用的稳定服务端 facade。
- Server-authoritative persistence now resolves to the active world directory and migrates then deletes old files from the previous game-directory `data/neo_favorite_items` root.
- 服务端权威持久化现在解析到当前世界目录，并会迁移随后删除旧游戏根目录 `data/neo_favorite_items` 下的文件。
- NeoForge Quark sorting compatibility now augments Quark's sorting-locked slot list with favorite player-inventory slots.
- NeoForge Quark 排序兼容现在会把已收藏玩家背包槽补入 Quark 的排序锁定槽列表。
- NeoForge Inventory Tweaks ReFoxed player-sort compatibility now augments the mod's locked/frozen slot list with favorite player-inventory slots.
- NeoForge Inventory Tweaks ReFoxed 玩家背包整理兼容现在会把已收藏玩家背包槽补入该模组的 locked/frozen 槽列表。
- NeoForge Quark hotbar changer compatibility now swaps hotbar row items under a scoped guard bypass, moves favorite state with the exchanged stack, and syncs the updated favorite state back to the client.
- NeoForge Quark 快捷栏切换兼容现在会在有作用域的守卫旁路下交换快捷栏行物品，让收藏状态跟随被交换物品移动，并把更新后的收藏状态同步回客户端。
- Lock-operation left-click now uses a unified container mouse state machine on all three loaders. The physical Alt-left-click toggles once at `mouseClicked`, `mouseDragged` only consumes leaked vanilla drag while active, and Mouse Tweaks compatibility is the only drag-slot toggle source when Mouse Tweaks is present. NeoForge additionally resolves Sophisticated screens through its dedicated screen state machine and pure slot-location helpers.
- 三个平台的锁定操作左键现在统一走容器鼠标状态机。物理 Alt+左键在 `mouseClicked` 切换一次，`mouseDragged` 只在 active 时消费漏进来的原版拖动；存在 Mouse Tweaks 时，拖动槽位切换只来自 Mouse Tweaks 兼容层。NeoForge 额外通过专用界面状态机与纯槽位定位 helper 兼容 Sophisticated 系列界面。
- NeoForge lock-operation state machine cleanup clarified the exits: `beginPress` toggles the physical press target, `toggleEnteredSlot` handles Mouse Tweaks slot-enter drag targets, `toggleLeakedSlotClick` handles Sophisticated leaked click actions, and `consumeActiveDrag` only suppresses leaked vanilla drag without sampling.
- NeoForge 锁定操作状态机整理后明确了出口职责：`beginPress` 切换物理按下目标，`toggleEnteredSlot` 处理 Mouse Tweaks 槽位进入拖动目标，`toggleLeakedSlotClick` 处理 Sophisticated 漏出的点击动作，`consumeActiveDrag` 只吞掉漏出的原版拖动而不采样。
- Overlay rendering now uses a common `OverlayRenderDescriptor` for style/tint/foreground decisions. The attempted low-alpha contrast backing for ModernUI tooltip blur was removed because ModernUI draws tooltip shadow in the tooltip render state above slot overlays.
- Overlay 渲染现在使用 common `OverlayRenderDescriptor` 统一描述样式、染色和前景决策。此前尝试的 ModernUI tooltip 低透明度对比底已撤回，因为 ModernUI 会在槽位 Overlay 上方的 tooltip 渲染状态中绘制阴影。
- Direct external insertion into locked empty selected slots now reroutes the incoming item to the first empty unlocked hotbar slot, then the first empty unlocked main-inventory slot, and drops it if no fallback slot exists. This covers `Inventory.setItem` paths such as Integrated Dynamics Squeezer and main-hand `Player.setItemInHand` paths such as Actually Additions Display Stand.
- 对锁定空当前槽的外部直接放入现在会把 incoming item 改放到第一个空且未锁定的快捷栏槽，再改放到第一个空且未锁定的主背包槽；没有回退槽时掉落。该路径覆盖 Integrated Dynamics Squeezer 这类 `Inventory.setItem` 调用，以及 Actually Additions Display Stand 这类主手 `Player.setItemInHand` 调用。
- Vanilla incoming-item insertion now corrects server-side `Inventory.getFreeSlot()` results before `Inventory.add(...)` consumes a stack. Locked empty main-inventory slots are skipped when `allowItemsIntoLockedEmptySlots=false`, so picked-up ground items move to another valid empty slot or remain unpicked when none exists.
- 原版 incoming item 放入现在会在 `Inventory.add(...)` 消耗 stack 前修正服务端 `Inventory.getFreeSlot()` 结果。`allowItemsIntoLockedEmptySlots=false` 时会跳过锁定空主背包槽，因此拾取的地面掉落物会进入其他有效空槽；没有有效空槽时则保持未拾取。
- Client and integrated-server favorite state now use separate storage contexts even when both sides have the same player UUID.
- 客户端与集成服务端收藏状态现使用独立存储上下文，即使双端玩家 UUID 相同也不会串状态。
- Vanilla occupied-stack merging now skips locked player targets through narrow hooks in `AbstractContainerMenu.moveItemStackTo` and `Inventory.getSlotWithRemainingSpace`, preserving vanilla target order and unlocked behavior.
- 原版非空堆叠合并现通过 `AbstractContainerMenu.moveItemStackTo` 与 `Inventory.getSlotWithRemainingSpace` 的窄钩子跳过锁定玩家目标，同时保持原版目标顺序和未锁行为。
- Two-endpoint swap policy is shared by the server and Fabric/Forge/NeoForge client handlers.
- 双端交换策略现由服务端与 Fabric/Forge/NeoForge 客户端 handler 共用。
- Server `ClickType.SWAP` protection now checks the player hotbar/offhand partner before excluding container-owned clicked slots, closing partial-mutation risk for external-container-to-locked-player-slot swaps.
- 服务端 `ClickType.SWAP` 保护现在会先检查玩家快捷栏/副手交换端，再排除容器自有点击槽，从而封闭外部容器与锁定玩家槽交换时的部分变更风险。
- NeoForge now has optional Su's Instant Swap compatibility at `SwapEngine`'s three click emitters. Locked operations are blocked by default, bypass leaves lock positions unchanged, and the lock-operation key authorizes only Su's exact short-lived vanilla click sequence when the server confirms an authoritative locked endpoint. Lock state moves only after post-state stacks match the resulting two-slot or three-slot item flow. Su's verifier, fallback, row loop, and hotbar-priority behavior remain upstream-owned.
- NeoForge 现已在 `SwapEngine` 三个点击发射器提供可选 Su's Instant Swap 兼容。涉及锁槽的操作默认阻止；旁路键保持锁位置；锁定键只有在服务端确认存在权威锁端点时，才临时授权 Su 精确的原版点击序列；post-state stack 符合实际两槽或三槽物品流向后才移动锁状态。Su 的校验、回退、整行循环和热栏优先行为仍由上游实现负责。
- Locked empty slot fallback is separated from GUI cursor placement: `Slot` mutation guards still only reject the carried stack, while the fallback path is reserved for direct inventory writes and main-hand replacement.
- 锁定空槽回退已与 GUI 光标放入分离：`Slot` 变更守卫仍只拒绝光标物品，回退路径仅用于直接背包写入和主手替换。
- NeoForge cursor placement into locked empty slots now also guards the official mouse-release screen event, covering creative inventory placement paths that defer `PICKUP` handling until release without adding a dedicated creative-screen mixin.
- NeoForge 锁定空槽的光标放入现在也会守卫官方鼠标释放界面事件，覆盖创造物品栏把 `PICKUP` 处理延迟到释放阶段的路径，并且不新增专用创造界面 Mixin。
- Third-party soft-compatibility mixins are now grouped under each loader's `mixin/compat` package, loaded only by non-required compat mixin configs, and named with the `<ModOrFeature><TargetOrScenario>CompatMixin` convention.
- 第三方软联动 Mixin 现在统一放在各平台 `mixin/compat` 包中，只由非 required 的兼容 Mixin 配置加载，并统一使用 `<模组或功能><目标或场景>CompatMixin` 命名约定。
- Unregistered historical `SophisticatedInventoryHelperCompatMixin` classes were removed because current protection favors lower-level mutation guards and targeted Sophisticated Sorter/screen compatibility over replacing SophisticatedCore's broad inventory helper.
- 未注册的历史 `SophisticatedInventoryHelperCompatMixin` 已删除，因为当前保护策略优先使用更低层的变更守卫和有针对性的 Sophisticated Sorter/界面兼容，而不是替换 SophisticatedCore 的宽泛库存 helper。
- Forge and NeoForge compat mixin plugins now check early-discovered mod files through `LoadingModList` before falling back to runtime `ModList`.
- Forge 与 NeoForge 兼容 Mixin plugin 现在会先通过 `LoadingModList` 检查早期发现的模组文件，再回退到运行期 `ModList`。
- Eight byte-identical loader-neutral core mixins now live in `common` and load through one shared required mixin config registered by Fabric, Forge, and NeoForge. Loader-specific client, item-handler, and optional compatibility hooks remain local.
- 8 个逐字节相同的平台无关核心 Mixin 现统一位于 `common`，并通过三端共同注册的一份 required Mixin 配置加载；平台客户端、物品处理器及可选兼容钩子继续留在各端。
- GitHub Actions now runs the common test suite, compiles Fabric/Forge/NeoForge, and processes all three loaders' resources on Java 21 with build-number mutation disabled.
- GitHub Actions 现使用 Java 21 执行 common 测试、编译 Fabric/Forge/NeoForge 并处理三端资源，同时禁用构建号变更。
- The low-cost/high-return maintenance list is closed: CI and core-mixin consolidation shipped; known mutation-boundary testing is an architecture rule; MixinExtras was rejected until loader dependencies converge or real redirect conflicts justify its runtime packaging cost.
- 低成本高收益维护清单已收口：CI 与核心 Mixin 合并已交付；已知变更边界回归测试已固化为架构规则；MixinExtras 延后到三端依赖收敛或真实 Redirect 冲突足以覆盖运行时打包成本时再引入。
- Gradle cache defaults are project-local through `gradle.bat` (`.gradle-home`), while `GRADLE_USER_HOME` and `GRADLE_HOME` remain available for local overrides. Common, Forge, and NeoForge compile classpaths include Fabric loader as compile-only annotation metadata so javac can resolve `net.fabricmc.api.EnvType` from referenced annotated classes without packaging Fabric loader into non-Fabric jars. Forge config registration now uses the constructor-injected `FMLJavaModLoadingContext` instead of deprecated static lookup.
- Gradle 缓存默认通过 `gradle.bat` 指向项目内 `.gradle-home`，同时保留 `GRADLE_USER_HOME` 与 `GRADLE_HOME` 供本地覆盖。common、Forge 与 NeoForge 编译 classpath 以 compile-only 方式包含 Fabric loader 注解元数据，使 javac 能解析被引用注解类中的 `net.fabricmc.api.EnvType`，但不会把 Fabric loader 打入非 Fabric 产物。Forge 配置注册现在使用构造函数注入的 `FMLJavaModLoadingContext`，不再使用已弃用的静态查找。
- ClientSort compatibility now uses optional three-loader mixins on ClientSort's schema validator and operation entrypoints: collect skips locked player-inventory menu slots, including locked empty slots that ClientSort would otherwise reject through placement checks; transfer and stack-fill filter locked source and target slots so destination `safeInsert(srcStack)` cannot shrink a locked source stack; sort fixes locked slots in place and re-pairs remaining sorted sources with remaining target slots in ClientSort's target order. The common logic is covered by `ClientSortCompatServiceTest`, and debug diagnostics are emitted when arrays are rewritten.
- ClientSort 兼容现在通过三端可选 Mixin 作用于 ClientSort 的 schema validator 与操作入口：collect 会跳过锁定玩家背包 menu slot，包括 ClientSort 原本会通过放入校验拒绝的锁定空槽；transfer 与 stack-fill 会过滤锁定来源槽和目标槽，避免目标槽 `safeInsert(srcStack)` 缩减锁定来源 stack；sort 会固定锁槽，并按 ClientSort 的目标顺序把剩余已排序来源重新配对到剩余目标槽。公共逻辑由 `ClientSortCompatServiceTest` 覆盖，数组被改写时会输出 debug 诊断。
- Better Experience fast-storage compatibility is added on NeoForge with a deliberately narrow source-slot guard: when its `StorageManager.saveAll(Player)` is about to transfer an `ItemStack` object that is still the exact stack stored in a locked player-inventory slot, that one transfer call is skipped.
- NeoForge 新增 Better Experience 一键存储兼容，并刻意保持为窄范围来源槽保护：当其 `StorageManager.saveAll(Player)` 正要转移的 `ItemStack` 对象仍是已锁玩家背包槽中的同一 stack 时，只跳过这一笔转移调用。
- The temporary Wrench Finder compatibility shim was removed after the upstream mod fixed its locked-source duplication bug; no Wrench Finder-specific runtime hook remains.
- Wrench Finder 上游修复锁定来源复制问题后，项目已移除临时兼容层；不再保留 Wrench Finder 专用运行时钩子。
