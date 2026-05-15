# Neo Favorite Items Roadmap

# Neo Favorite Items 路线图

Last updated: 2026-05-10

最后更新：2026-05-10

## Checklist Summary

## 勾选简表

- [ ] P0: Validate Mouse Tweaks drag-click lock toggling on Fabric, Forge, and NeoForge.
- [ ] P0：实机验证 Fabric、Forge、NeoForge 的 Mouse Tweaks 拖动点击切换锁定。
- [ ] P0: Re-validate NeoForge vanilla GUI Mouse Tweaks drag-lock toggling after restoring the normal-container fallback that was regressed by the Sophisticated empty-slot fix.
- [ ] P0：在恢复普通容器 fallback 后，重新验证 NeoForge 原版 GUI 的 Mouse Tweaks 拖拽锁定；该路径此前曾被 Sophisticated 空槽修复误伤。
- [ ] P0: Validate behavior consistency in creative mode, normal containers, hotbar swaps, dragging, and external item transfers.
- [ ] P0：实测创造模式、普通容器、快捷栏交换、拖拽和外部物品转移的一致性。
- [ ] P0: Runtime validate NeoForge Quark inventory sorting with locked main-inventory slots.
- [ ] P0：实机验证 NeoForge + Quark 背包排序遇到已锁主背包槽时会跳过锁槽。
- [ ] P0: Runtime validate NeoForge Inventory Tweaks ReFoxed player sorting with locked hotbar and main-inventory slots.
- [ ] P0：实机验证 NeoForge + Inventory Tweaks ReFoxed 玩家背包整理遇到已锁快捷栏和主背包槽时会跳过锁槽。
- [ ] P0: Runtime validate NeoForge Quark hotbar changer (`Z`) swaps with locked hotbar and main-inventory slots.
- [ ] P0：实机验证 NeoForge + Quark 快捷栏切换（`Z`）在锁定快捷栏和主背包槽时的交换行为。
- [ ] P0: Runtime validate NeoForge Sophisticated Backpacks Alt-left-click lock toggling after the `QUICK_MOVE` click-path fix.
- [ ] P0：实机验证 NeoForge + Sophisticated Backpacks 在修复 `QUICK_MOVE` 点击路径后的 Alt+左键锁定切换。
- [ ] P0: Validate multiplayer installation modes: client-only, server-only, and both-sides-installed.
- [ ] P0：验证多人安装模式：仅客户端、仅服务端、双端均安装。
- [ ] P1: Add automated tests for slot mapping, config parsing, serialization compatibility, interaction decisions, and sync revisions.
- [ ] P1：为槽位映射、配置解析、序列化兼容、交互决策和同步修订号添加自动化测试。
- [ ] P1: Improve bypass-key item movement tracking and define complex-move behavior.
- [ ] P1：完善旁路键下的物品移动追踪，并明确复杂移动行为。
- [ ] P2: Add user-provided custom overlay textures and texture color modes.
- [ ] P2：支持用户自定义 Overlay 纹理和纹理颜色模式。
- [ ] P2: Implement visual and sound feedback triggers.
- [ ] P2：补齐视觉反馈和音效反馈触发。
- [ ] P2: Evaluate config hot reload or an in-game config screen.
- [ ] P2：评估配置热重载或游戏内配置界面。
- [ ] P2: Split server-affecting configuration and client-only configuration into separate config files.
- [ ] P2：将会影响服务端行为的配置与纯客户端配置拆分到两个独立配置文件中。
- [ ] P3: Clean up unused helpers and repeated platform-layer code.
- [ ] P3：清理未使用 helper 和平台层重复代码。

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
- Favorite state management and basic persistence
- 收藏状态管理和基础持久化
- Fabric, Forge, and NeoForge entrypoints with client key binding registration
- Fabric、Forge、NeoForge 入口与客户端按键注册
- Overlay render entrypoints and hotbar HUD overlays for all three loaders
- 三个平台 Overlay 渲染入口和快捷栏 HUD Overlay
- Nine PNG overlay textures and configurable color rendering
- 九种 Overlay PNG 材质和配置色渲染
- Unified interaction decisions through `InteractionGuardService`
- `InteractionGuardService` 统一交互决策
- Client polling for lock-operation and bypass-key states
- 客户端锁定操作键、旁路键状态轮询
- Server-side toggle handling, revisions, full/incremental sync, and bypass-key state sync
- 服务端收藏切换、修订号、全量/增量同步和旁路键状态同步
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
- Death lifecycle handling now preserves locked slot contents through `keepInventory=true` respawns and clears favorite state after `keepInventory=false` death drops.
- 死亡生命周期处理现在会在 `keepInventory=true` 重生时保留锁定槽内容，并在 `keepInventory=false` 死亡掉落后清空收藏状态。
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
- Locked empty slot fallback is separated from GUI cursor placement: `Slot` mutation guards still only reject the carried stack, while the fallback path is reserved for direct inventory writes and main-hand replacement.
- 锁定空槽回退已与 GUI 光标放入分离：`Slot` 变更守卫仍只拒绝光标物品，回退路径仅用于直接背包写入和主手替换。
- NeoForge cursor placement into locked empty slots now also guards the official mouse-release screen event, covering creative inventory placement paths that defer `PICKUP` handling until release without adding a dedicated creative-screen mixin.
- NeoForge 锁定空槽的光标放入现在也会守卫官方鼠标释放界面事件，覆盖创造物品栏把 `PICKUP` 处理延迟到释放阶段的路径，并且不新增专用创造界面 Mixin。

## Priority Roadmap

## 优先级路线图

### P0 Validation And Correctness

### P0 验证与正确性

- In-game validation of Mouse Tweaks drag-click lock toggling on all three loaders.
- 在三个加载器中实机验证 Mouse Tweaks 拖动点击切换锁定。
- Continue validating behavior consistency across Fabric and Forge in creative mode, normal containers, hotbar swaps, dragging, and external item transfers. NeoForge has validated the high-risk drop, offhand, armor quick-move, and AE2 `MOVE_REGION` paths listed above.
- 继续实测 Fabric、Forge 在创造模式、普通容器、快捷栏交换、拖拽和外部物品转移中的一致性。NeoForge 已验证上方列出的高风险丢弃、副手、护甲快速移动和 AE2 `MOVE_REGION` 路径。
- Re-test AE2 terminal space-left-click MOVE_REGION behavior against the shared-menu compatibility hook on Fabric/Forge when available.
- 后续在 Fabric/Forge 上复测 AE2 终端空格+左键 MOVE_REGION 是否已被公共菜单层兼容钩子覆盖。
- Runtime-test Quark sorting on NeoForge with locked player main-inventory slots, including existing Quark locked slots if a menu supplies them.
- 在 NeoForge 实机复测 Quark 排序遇到已锁玩家主背包槽时的行为，并覆盖菜单自身提供 Quark 锁定槽的情况。
- Runtime-test Inventory Tweaks ReFoxed player sorting on NeoForge with locked hotbar and main-inventory slots, including its own `/LOCKED` and `/FROZEN` rule slots.
- 在 NeoForge 实机复测 Inventory Tweaks ReFoxed 玩家背包整理遇到已锁快捷栏和主背包槽时的行为，并覆盖其自身 `/LOCKED` 与 `/FROZEN` 规则槽。
- Runtime-test Quark hotbar changer on NeoForge by locking hotbar slot `0`, swapping with main-inventory rows `9`, `18`, or `27`, and confirming both item stacks and favorite state exchange cleanly without ghost items.
- 在 NeoForge 实机复测 Quark 快捷栏切换：锁定快捷栏 `0` 号槽，与主背包行 `9`、`18` 或 `27` 交换，确认两边物品与收藏状态都能干净交换且不留下幽灵物品。
- Runtime-test Sophisticated Backpacks on NeoForge by opening a backpack, holding Alt, left-clicking player inventory slots, and confirming the favorite lock toggles without quick-moving the stack.
- 在 NeoForge 实机复测 Sophisticated Backpacks：打开背包，按住 Alt 左键玩家背包槽，确认收藏锁定会切换且物品不会被快速移动。
- Validate multiplayer installation modes on Fabric and Forge: client-only, server-only, and both-sides-installed. NeoForge is validated.
- 验证 Fabric、Forge 多人安装模式：仅客户端、仅服务端、双端均安装。NeoForge 已验证。

### P1 Test Coverage And Core Semantics

### P1 测试覆盖与核心语义

- Add automated tests for slot mapping, config parsing, serialization compatibility, interaction decisions, and sync revisions.
- 为槽位映射、配置解析、序列化兼容、交互决策和同步修订号添加自动化测试。
- Improve item movement tracking while the bypass key is held, and define complex-move behavior for `FOLLOW_ITEM` and `STAY_AT_POSITION`.
- 完善旁路键下的物品移动追踪，明确 `FOLLOW_ITEM` 与 `STAY_AT_POSITION` 在复杂移动中的行为。

### P2 User-Facing Features

### P2 用户可见功能

- Add user-provided custom overlay textures from a dedicated game-directory folder, with config entries for locked/highlight texture selection and texture color mode.
- 支持用户在游戏目录下的专用文件夹提供自定义 Overlay 纹理，并提供已锁定/高亮纹理选择和纹理颜色模式配置。
- Custom texture color modes should include `LUMINANCE` for config-color tinting and `COLOR` for using the texture's original color without configurable recoloring.
- 自定义纹理颜色模式应包含 `LUMINANCE`（按配置色染色）和 `COLOR`（使用纹理原色，不再配置改色）。
- Implement actual visual feedback and sound feedback triggers.
- 补齐视觉反馈和音效反馈的实际触发逻辑。
- Evaluate whether config hot reload or an in-game config screen is needed.
- 评估是否需要配置热重载或游戏内配置界面。
- Split server-affecting settings, such as lock rules, persistence/sync behavior, and interaction protection, from client-only settings, such as overlays, local feedback, and key-related presentation, into separate config files.
- 将会影响服务端行为的配置（如锁定规则、持久化/同步行为、交互保护）与纯客户端配置（如 Overlay、本地反馈、按键相关展示）拆分到独立配置文件中。

### P3 Cleanup

### P3 清理

- Clean up unused helper methods and repeated platform-layer code.
- 清理未使用的 helper 方法和平台层重复代码。

## Long-Term Maintenance

## 长期维护

- Gradually modernize the overlay rendering pipeline during related updates instead of doing one large rewrite.
- 在后续相关更新中渐进式现代化 Overlay 渲染管线，避免一次性大规模重写。
- Continue moving texture metadata and sampling decisions into loader-neutral common abstractions where practical.
- 在可行时，继续将纹理元数据和采样决策逐步迁移到平台无关的 common 抽象。
- Keep platform renderers focused on loader-specific `GuiGraphics` calls, HUD/screen hooks, and final draw submission.
- 让平台渲染器逐步聚焦于加载器特有的 `GuiGraphics` 调用、HUD/Screen 事件入口和最终绘制提交。

## Known Limitations

## 已知限制

- Favorite slots currently only cover player inventory slots; chest, furnace, and other container-owned slots are not supported.
- 当前只支持玩家物品栏槽位收藏，不支持箱子、熔炉等容器自身槽位收藏。
- Server-authoritative sync exists, and NeoForge dedicated-server installation modes are validated; Fabric and Forge still need real multiplayer matrix validation.
- 服务端权威同步已存在，NeoForge 真实专用服务端安装模式已验证；Fabric 和 Forge 仍需要多人矩阵实测。
- In NeoForge Sophisticated screens, empty player-inventory slots can be toggled by single click through the low-level press path, but Mouse Tweaks does not currently emit drag-enter samples for Sophisticated empty slots in the tested modpack, so empty-slot drag marking there is intentionally left as a known limitation.
- 在 NeoForge 的 Sophisticated 系列界面中，玩家背包空槽可通过单击低层按下路径切换；但在已测试整合包中 Mouse Tweaks 不会为 Sophisticated 空槽发出拖动进入采样，因此该界面内空槽拖动标记暂作为已知限制保留。
- `FavoritesManager` is still a singleton state manager; it can later evolve into clearer state service/repository interfaces.
- `FavoritesManager` 仍是单例状态管理器，后续可继续演进为更清晰的状态服务/仓库接口。
- Automated tests cover core common-layer behavior. NeoForge has completed the listed high-risk in-game checks, while Fabric and Forge still require manual runtime matrix validation.
- 自动化测试已覆盖 common 层核心行为。NeoForge 已完成上述高风险实机检查，Fabric 和 Forge 仍需手工运行矩阵验证。

## Request Buffer

## 临时需求缓冲区

- Add new ad-hoc ideas here first, then promote them into the priority roadmap after review.
- 新的临时想法先放在这里，评审后再提升到优先级路线图。
