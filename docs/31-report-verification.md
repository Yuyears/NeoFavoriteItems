# Verification Report

# 修复验证报告

Date: 2026-08-16

日期：2026-08-16

## Purpose

## 用途

- This document summarizes what was fixed in this round, what behavior is expected after the fixes, and which real-game validations still need to be completed.
- 本文档汇总本轮已修复的问题、修复后的预期行为，以及仍需在真实游戏环境中完成的验证项。

## Fixed Issues

## 已修复问题

1. Server/player revision and bypass maps are now concurrent-safe, and client sync revision updates are synchronized.
2. 服务端玩家修订号和旁路状态映射已改为并发安全实现，客户端同步修订更新也已加同步保护。
3. Favorite serialization/deserialization now uses UTF-8 consistently and logs malformed entries instead of swallowing them silently.
4. 收藏数据的序列化/反序列化已统一使用 UTF-8，并会记录异常条目日志，不再静默吞掉错误。
5. Favorite data writes now use a temporary file plus replace move to reduce corruption risk.
6. 收藏数据写入改为“临时文件 + 替换移动”，以降低文件损坏风险。
7. Config parsing now records load issues and preserves defaults when invalid values are encountered.
8. 配置解析现在会记录加载问题，并在遇到非法值时保留默认配置。
9. Reflection lookup now supports cache expiration and explicit invalidation.
10. 反射查找现在支持缓存过期和显式失效。
11. GUI-outside selected hotbar drops are intercepted on the client before `LocalPlayer.drop(boolean)` can trigger the misleading drop animation.
12. 在 GUI 外，当前手持且被锁定的快捷栏物品会在 `LocalPlayer.drop(boolean)` 播放误导性丢弃动画前被客户端拦截。
13. Forge/NeoForge networking now treats the mod channel as optional, allowing client-only and server-only installation modes.
14. Forge/NeoForge 网络通道现在是可选的，支持仅客户端安装和仅服务端安装。
15. Persistence now splits correctly between client-only server-address directories and server-authoritative world-save directories.
16. 持久化现在已正确区分单端客户端按服务器地址存储，以及服务端权威模式按世界存档目录存储。
17. Persistence lifecycle now follows world start context initialization, player join per-player read into cache, in-play cache updates, player leave incremental save, and world stop full-cache save.
18. 持久化生命周期现在遵循“世界启动初始化上下文、玩家进入按玩家读取到缓存、游戏过程更新缓存、玩家退出增量保存、世界关闭完整缓存保存”。
19. Legacy `itemfavorites/...` client data is now migrated into the new path on first successful read, and the old file is removed afterward.
20. 旧 `itemfavorites/...` 客户端数据现在会在首次成功读取后迁移到新路径，并删除旧文件。
21. Server-side full and incremental sync sends now check whether each player connection advertises the mod payload/channel before sending, preventing login failure for clients without the mod.
22. 服务端全量和增量同步发包现在会先检查玩家连接是否声明了本模组 payload/channel，避免未安装客户端登录失败。
23. Client-only persistence now treats a storage namespace change as a world-context reload, saving the previous namespace before loading the server-address or remote-address namespace.
24. 仅客户端持久化现在会把存储命名空间变化视为世界上下文重载，先保存旧命名空间，再加载服务器地址或远端地址命名空间。
25. Locked incoming targets are now rejected before composite moves proceed, preventing offhand-swap and armor quick-move desync/item loss cases.
26. 锁定的放入目标槽现在会在复合移动继续前被拒绝，避免副手交换和护甲 Shift 装备导致不同步或吞物品。
27. GUI and GUI-outside offhand swaps now check both the selected/hovered slot and the offhand slot before client prediction or server mutation.
28. GUI 内外副手交换现在会在客户端预测或服务端变更前同时检查当前/悬停槽与副手槽。
29. Standard `Slot` mutation APIs now route through the same guard decisions for player inventory slots, covering `safeInsert`, `safeTake`, `tryRemove`, `remove`, `set`, and `setByPlayer` without globally hiding item reads.
30. 标准 `Slot` 变更 API 现在会对玩家背包槽位复用同一套守卫决策，覆盖 `safeInsert`、`safeTake`、`tryRemove`、`remove`、`set` 和 `setByPlayer`，且不全局隐藏物品读取。
31. AE2 terminal `MOVE_REGION` transfers now have a conditional shared-menu compatibility layer: `AEBaseMenu.quickMoveStack` protects locked player-slot sources, and `MEStorageMenu.handleNetworkInteraction` rejects network-to-player region moves that would target locked player inventory slots.
32. AE2 终端 `MOVE_REGION` 转移现在补充了条件加载的公共菜单层兼容：`AEBaseMenu.quickMoveStack` 保护锁定玩家槽来源，`MEStorageMenu.handleNetworkInteraction` 会拒绝把网络物品批量移入锁定玩家背包目标槽的操作。
33. NeoForge now registers the optional compat mixin config from `neoforge.mods.toml`, so AE2 shared-menu hooks are actually available at runtime instead of only being packaged.
34. NeoForge 现在会从 `neoforge.mods.toml` 注册可选 compat Mixin 配置，因此 AE2 公共菜单层钩子会在运行时实际可用，而不是只被打包进 jar。
35. AE2 compat mixins now avoid early `ModList` or class-presence checks and follow the Mouse Tweaks-style approach of letting the optional mixin config apply when the target class is transformed.
36. AE2 兼容 Mixin 现在避免过早使用 `ModList` 或类存在性判断，改为接近 Mouse Tweaks 的方式，在目标类转换时由可选 Mixin 配置应用。
37. The Gradle result-copy task now uses lazy task-path dependencies and no longer breaks focused common-module test runs under configuration-on-demand.
38. Gradle 构建结果复制任务现在使用惰性任务路径依赖，不再破坏 configure-on-demand 下聚焦执行的 common 模块测试。
39. Forge/NeoForge `InvWrapper` and `RangedWrapper` guards no longer hide locked player inventory stacks from read APIs, fixing custom GUIs such as JustDireThings that rendered locked slots as empty.
40. Forge/NeoForge 的 `InvWrapper` 与 `RangedWrapper` 守卫不再通过读取 API 隐藏锁定玩家背包物品，修复 JustDireThings 等自定义 GUI 中锁定槽显示为空的问题。
41. Forge/NeoForge slot resolvers now recognize `SlotItemHandler` instances backed by player-inventory `InvWrapper` or `RangedWrapper`, so overlays and early click guards apply in JustDireThings-style GUIs.
42. Forge/NeoForge 槽位解析器现在能识别由玩家背包 `InvWrapper` 或 `RangedWrapper` 支撑的 `SlotItemHandler`，因此 JustDireThings 风格 GUI 中也会渲染 Overlay 并提前拦截点击。
43. Death respawn handling now distinguishes vanilla lifecycle copies from player item movement: vanilla inventory preservation restores the old inventory under a scoped guard bypass, while normal `keepInventory=false` deaths clear and save favorite state unless the death-preservation config is enabled.
44. 死亡重生处理现在会区分原版生命周期复制和玩家物品移动：原版背包保留会在有作用域的守卫绕过中恢复旧背包；普通 `keepInventory=false` 死亡会清空并保存收藏状态，除非启用死亡保留配置。
45. Server-authoritative persistence now uses the active world directory instead of the game directory, and legacy files under `<game>/data/neo_favorite_items` are migrated once then deleted after successful read.
46. 服务端权威持久化现在使用当前世界目录而不是游戏目录；旧 `<游戏目录>/data/neo_favorite_items` 文件会在首次成功读取后迁移并删除。
47. NeoForge Quark sorting compatibility now augments Quark's sorting-locked slot list with favorite player-inventory slots before Quark clears and rewrites the sorted range.
48. NeoForge Quark 排序兼容现在会在 Quark 清空并重写排序范围前，把已收藏玩家背包槽补入 Quark 的排序锁定槽列表。
49. NeoForge Quark hotbar changer compatibility now handles `ChangeHotbarMessage.swap(Container, int, int)` as an atomic swap under a scoped inventory-guard bypass, then swaps favorite state between the two player inventory indices and sends a full favorite sync.
50. NeoForge Quark 快捷栏切换兼容现在会把 `ChangeHotbarMessage.swap(Container, int, int)` 作为有作用域库存守卫旁路下的原子交换处理，随后在两个玩家背包索引之间交换收藏状态并发送全量收藏同步。
51. NeoForge Sophisticated Backpacks screens now resolve lock-operation slots through a dedicated `StorageScreenBase.findSlot(x, y)` container mouse state machine, with a coordinate fallback for empty player-inventory slots that Sophisticated's lookup can omit.
52. NeoForge Sophisticated Backpacks 界面现在通过专用 `StorageScreenBase.findSlot(x, y)` 容器鼠标状态机解析锁定操作槽位，并对 Sophisticated 自身查找可能遗漏的空玩家槽提供坐标回退。
53. Fabric, Forge, and NeoForge lock-operation `mouseClicked`/`mouseDragged`/`mouseReleased` state machines now consume Alt-left-click before inventory click semantics are selected, converting physical clicks and sampled drag slots into loader slot-toggle handler calls.
54. Fabric、Forge、NeoForge 的锁定操作 `mouseClicked`/`mouseReleased` 状态机会在库存点击语义被选择前消费 Alt+左键，并把物理点击转换为对应加载器槽位切换 handler 调用；拖动槽位切换只由 Mouse Tweaks 兼容层在 MT 存在时转发。
55. NeoForge now hooks SophisticatedCore's `StorageScreenBase.slotClicked` override directly, because that screen does not rely on the vanilla `AbstractContainerScreen.slotClicked` implementation.
56. NeoForge 现在会直接钩住 SophisticatedCore 的 `StorageScreenBase.slotClicked` 覆盖实现，因为该界面不依赖原版 `AbstractContainerScreen.slotClicked` 实现。
57. Vanilla, Fabric creative, and NeoForge Sophisticated `slotClicked` hooks now cancel leaked `PICKUP` and `QUICK_MOVE` inventory actions while Alt is held, but do not toggle state.
58. 原版、Fabric 创造模式与 NeoForge Sophisticated 的 `slotClicked` 钩子现在会在按住 Alt 时取消漏进来的 `PICKUP` 和 `QUICK_MOVE` 库存动作，但不会切换状态。
59. Mouse Tweaks drag compatibility on Fabric, Forge, and NeoForge now refreshes Mouse Tweaks' bookkeeping, borrows its slot-enter detection, and routes newly entered slots to this mod's lock toggle handler while consuming Mouse Tweaks' original Alt click semantics.
60. Fabric、Forge、NeoForge 的 Mouse Tweaks 拖动兼容现在会刷新 Mouse Tweaks 账本，借用其槽位进入检测，并把新进入的槽位路由到本模组的锁定切换 handler，同时消费 Mouse Tweaks 原本的 Alt 点击语义。
61. Overlay rendering now uses a common `OverlayRenderDescriptor` for style, tint, opacity, and foreground placement decisions.
62. Overlay 渲染现在使用 common `OverlayRenderDescriptor` 统一描述样式、染色、透明度和前景位置决策。
63. ModernUI rounded tooltip shadow is generated inside its tooltip shader/render state above normal slot overlays; the removed contrast backing should no longer be present in configs or renderer code.
64. ModernUI 圆角 tooltip 阴影由其 tooltip shader/render state 在普通槽位 Overlay 上方生成；已移除的对比底不应再出现在配置或渲染代码中。
65. Death respawn inventory restoration now has a loader-level `ServerPlayer.restoreFrom` internal operation marker controlled by a common policy that combines `keepEverything`, the world `keepInventory` gamerule, and `[deathBehavior] preserveLockedSlotContents`.
66. 死亡重生库存恢复现在拥有加载器层 `ServerPlayer.restoreFrom` 内部操作标识，并由 common 策略综合 `keepEverything`、世界规则 `keepInventory` 与 `[deathBehavior] preserveLockedSlotContents` 后决定是否启用。
67. `[deathBehavior] preserveLockedSlotContents` can preserve locked slot contents even when `keepInventory=false`: death drops skip non-empty locked player slots and respawn clone/copy restores only those locked slot contents.
68. `[deathBehavior] preserveLockedSlotContents` 可以在 `keepInventory=false` 时保留锁定槽内容：死亡掉落会跳过非空锁定玩家槽，重生 clone/copy 只恢复这些锁定槽内容。
69. Preserved death drops now keep the vanilla `Inventory.dropAll()` path active by temporarily hiding locked stacks before the call and restoring them after return, which avoids leaving NeoForge `LivingDropsEvent` listeners such as Curios on a non-vanilla drop path.
70. 保留型死亡掉落现在通过在调用前临时隐藏锁定栈、返回后恢复来保持原版 `Inventory.dropAll()` 路径继续执行，避免让 Curios 等 NeoForge `LivingDropsEvent` 监听器处在非原版掉落路径上。
71. Configuration is now split into `neo-favorite-items-common.toml` for server/rule-affecting options and `neo-favorite-items-client.toml` for client presentation/feedback options. Legacy `neo-favorite-items.toml` is read only as a migration source and deleted after successful split-file generation.
72. 配置现在拆分为用于服务端/规则项的 `neo-favorite-items-common.toml` 和用于客户端显示/反馈项的 `neo-favorite-items-client.toml`。旧 `neo-favorite-items.toml` 只作为迁移来源读取，并会在拆分文件成功生成后删除。
73. Third-party soft-compatibility mixins are now isolated under `mixin/compat`, registered only from non-required compat configs, and renamed with the `CompatMixin` suffix convention.
74. 第三方软联动 Mixin 现在隔离在 `mixin/compat` 下，只从非 required 兼容配置注册，并统一使用 `CompatMixin` 后缀命名约定。
75. Unregistered SophisticatedCore inventory-helper compatibility mixins were removed; Forge/NeoForge compat plugins now prefer early `LoadingModList` mod-file checks before runtime `ModList` fallback.
76. 未注册的 SophisticatedCore 库存 helper 兼容 Mixin 已删除；Forge/NeoForge 兼容 plugin 现在优先使用早期 `LoadingModList` 模组文件检查，再回退到运行期 `ModList`。
77. ClientSort collect/sort/transfer/stack-fill compatibility now rewrites slot arrays at ClientSort schema-validation and operation boundaries: locked collect slots are filtered, transfer and stack-fill source/target arrays are filtered, and sort mappings keep locked slots fixed while re-pairing remaining sorted sources with remaining targets in ClientSort's target order.
78. ClientSort collect/sort/transfer/stack-fill 兼容现在会在 ClientSort schema 校验与实际操作边界改写槽位数组：锁定 collect 槽会被过滤，transfer 与 stack-fill 的来源/目标数组都会过滤锁槽，sort 映射会固定锁槽，并按 ClientSort 的目标顺序把剩余已排序来源重新配对到剩余目标槽。
79. Better Experience provides the inspected Confluence modpack nearby-container quick-stack action. NeoForge compatibility now only skips Better Experience's per-stack transfer call when the source stack belongs to a locked player-inventory slot; container discovery and target insertion remain owned by Better Experience.
80. 已排查到汇流整合包附近容器一键存储动作由 Better Experience 提供。NeoForge 兼容现在只在来源 stack 属于已锁玩家背包槽时跳过 Better Experience 的单次 stack 转移调用；容器发现和目标放入仍由 Better Experience 自身处理。
81. Wrench Finder direct lookup now treats locked player-inventory and offhand sources as absent during `findDirectMatch`, preventing its non-transactional copy-and-set equip path from duplicating a protected item while allowing later unlocked matches to remain discoverable.
82. Wrench Finder 直接检索现在会在 `findDirectMatch` 中把锁定玩家背包与副手来源视为不存在，避免其非事务性的复制后写入装备流程复制受保护物品，同时保留后续未锁匹配项的检索能力。

## Integration Validation Summary

## 集成验证摘要

- Client-only expectation:
- 单端客户端预期：
  - Fabric: supported via `ClientPlayNetworking.canSend(...)` fallback
  - Fabric：通过 `ClientPlayNetworking.canSend(...)` 回退机制支持
  - Forge: supported via optional `SimpleChannel` presence check
  - Forge：通过可选 `SimpleChannel` 存在性检查支持
  - NeoForge: supported via optional payload registration plus channel presence check
  - NeoForge：通过可选 payload 注册和通道存在性检查支持
  - Persistence: local client storage uses `favoriteitems/<sanitized-server-address>/players/<uuid>.dat`
  - 持久化：客户端本地存储使用 `favoriteitems/<净化后的服务器地址>/players/<uuid>.dat`
- Server-only expectation:
- 仅服务端安装预期：
  - Fabric: player-join full sync now uses the same guarded `sendFullSync` path as toggle resync
  - Fabric：玩家加入时的全量同步现在与切换后的重同步使用同一个受保护的 `sendFullSync` 路径
  - Forge: compatibility display handled via custom `DisplayTest`
  - Forge：通过自定义 `DisplayTest` 处理兼容性显示
  - NeoForge: compatibility display set to ignore side mismatch in `neoforge.mods.toml`
  - NeoForge：在 `neoforge.mods.toml` 中设置忽略双端不对称带来的显示兼容性问题
  - Server-to-client sync packets are skipped when the joining connection does not advertise the corresponding client payload/channel
  - 当加入服务器的连接未声明对应客户端 payload/channel 时，服务端到客户端同步包会被跳过
- Dual-install expectation:
- 双端安装预期：
  - Full/incremental sync and bypass-state sync remain enabled
  - 全量/增量同步和旁路键状态同步保持启用
  - Local fallback is bypassed when the remote channel is present
  - 当远端通道存在时，本地回退逻辑不会生效
  - Persistence uses the active world save under `data/neo_favorite_items/players/<uuid>.dat`
  - 持久化使用当前世界目录下的 `data/neo_favorite_items/players/<uuid>.dat`
- Composite move expectation:
- 复合移动预期：
  - Offhand swaps are canceled when either side is locked and would receive or lose an item, unless bypass is held and enabled.
  - 当任一侧锁定且会接收或失去物品时，副手交换会被取消；启用并按住旁路键时除外。
  - Shift-click equipment moves are canceled before the source stack is removed when the destination armor/offhand slot is locked.
  - 当目标护甲/副手槽已锁定时，Shift 点击装备会在来源物品被移除前取消。
- Generic slot API expectation:
- 通用槽位 API 预期：
  - Custom menus that use vanilla `Slot` mutation APIs will be stopped at source-removal or target-insertion boundaries for locked player inventory slots.
  - 使用原版 `Slot` 变更 API 的自定义菜单，会在锁定玩家背包槽的来源取出或目标放入边界被阻止。
  - The guard does not intercept `Slot.getItem` or `Inventory.getItem`, preserving normal menu sync and third-party inspection behavior.
  - 守卫不拦截 `Slot.getItem` 或 `Inventory.getItem`，以保留正常菜单同步和第三方检查行为。
- Item-handler player inventory expectation:
- Item handler 玩家背包预期：
  - Forge/NeoForge `InvWrapper` and `RangedWrapper` expose real stack and slot-limit reads, while `isItemValid`, `extractItem`, `insertItem`, and `setStackInSlot` still enforce lock rules.
  - Forge/NeoForge 的 `InvWrapper` 与 `RangedWrapper` 会暴露真实物品和槽位上限读取，同时仍通过 `isItemValid`、`extractItem`、`insertItem` 与 `setStackInSlot` 执行锁定规则。
  - JustDireThings screens that add player slots through `InvWrapper(playerInventory)` should display locked slots normally instead of as empty.
  - JustDireThings 中通过 `InvWrapper(playerInventory)` 添加玩家槽位的界面应正常显示锁定槽，而不是显示为空。
  - Overlay rendering, client-side lock toggling, client-side guarded click cancellation, and server-side menu click cancellation should all resolve those item-handler player slots to the same logical player inventory indices.
  - Overlay 渲染、客户端锁定切换、客户端点击预拦截和服务端菜单点击拦截都应把这些 item-handler 玩家槽解析到同一套逻辑玩家背包索引。
- AE2 terminal expectation:
- AE2 终端预期：
  - Space-left-click region moves from locked player inventory slots are canceled at the AE2 shared quick-move entrypoint.
  - 从锁定玩家背包槽出发的空格+左键区域转移会在 AE2 公共快速移动入口被取消。
  - Space-left-click region moves from the AE2 network into locked player inventory slots are canceled before AE2 starts moving repeated stacks.
  - 从 AE2 网络向锁定玩家背包槽批量放入的空格+左键区域转移会在 AE2 开始重复搬运前被取消。
- Quark sorting expectation:
- Quark 排序预期：
  - On NeoForge, Quark main-inventory sorting skips favorite player-inventory slots by treating them as Quark sorting-locked slots.
  - 在 NeoForge 上，Quark 主背包排序会把已收藏玩家背包槽视作 Quark 排序锁定槽并跳过这些槽。
  - Existing Quark-provided locked slots are preserved and merged with favorite slots instead of being replaced.
  - 菜单自身提供的 Quark 锁定槽会被保留，并与收藏槽合并，而不是被覆盖。
- Quark hotbar changer expectation:
- Quark 快捷栏切换预期：
  - On NeoForge, Quark `Z` hotbar row swaps exchange both stacks even when one side is locked, because the swap is treated as an intentional item move rather than a blocked insertion into a locked slot.
  - 在 NeoForge 上，Quark `Z` 键快捷栏行交换即使一侧已锁定也会交换两边物品，因为该交换会被视为一次有意物品移动，而不是一次应被阻止的锁槽放入。
  - Favorite state follows the exchanged stack between hotbar slots `0..8` and main-inventory rows `9..35`, then the updated favorite set is synchronized to the client.
  - 收藏状态会随被交换物品在快捷栏 `0..8` 与主背包行 `9..35` 之间移动，随后更新后的收藏集合会同步给客户端。
- ClientSort expectation:
- ClientSort 预期：
  - ClientSort collect should no longer fail with "invalid payload data" when the selected player-inventory range includes locked slots; those slots are omitted at ClientSort validation and collect-operation entrypoints.
  - 当 ClientSort collect 选择范围包含锁定玩家槽时，不应再因“载荷数据无效”失败；这些槽会在 ClientSort 校验和 collect 操作入口被移除。
  - Locked empty slots are part of this expectation because ClientSort validates empty slots through placement checks before collecting.
  - 锁定空槽同样属于该预期范围，因为 ClientSort 会在 collect 前通过放入校验验证空槽可访问性。
  - When debug logging is enabled, rewritten ClientSort collect/sort arrays should produce `ClientSort collect filtered locked slots` or `ClientSort sort stabilized locked slots` diagnostics.
  - 启用 debug 日志时，ClientSort collect/sort 数组被改写应产生 `ClientSort collect filtered locked slots` 或 `ClientSort sort stabilized locked slots` 诊断。
  - ClientSort sort should keep locked slots unchanged while compacting unlocked sorted sources into unlocked target slots in ClientSort's target order; the rewritten mapping must remain a valid one-to-one source/destination permutation.
  - ClientSort sort 应保持锁定槽不变，同时按 ClientSort 的目标顺序把未锁已排序来源压入未锁目标槽；改写后的映射必须仍是有效的一一 source/destination permutation。
  - ClientSort transfer, match-transfer, and stack-fill must not remove from or insert into locked player inventory slots. This is enforced by filtering both source and target arrays, because ClientSort's implementation can mutate source stacks through destination `safeInsert(srcStack)`.
  - ClientSort transfer、同类移动与 stack-fill 不得从锁定玩家槽取出，也不得向锁定玩家槽放入。该限制通过同时过滤来源和目标数组实现，因为 ClientSort 实现会通过目标槽 `safeInsert(srcStack)` 直接改变来源 stack。
- Sophisticated Backpacks click expectation:
- Sophisticated Backpacks 点击预期：
  - On NeoForge, Alt-left-click lock operations in Sophisticated Backpacks screens are handled by the dedicated container mouse state machine before the screen can emit `PICKUP` or `QUICK_MOVE` inventory actions.
  - 在 NeoForge 上，Sophisticated Backpacks 界面中的 Alt+左键锁定操作会先由专用容器鼠标状态机处理，再轮到界面生成 `PICKUP` 或 `QUICK_MOVE` 库存动作。
  - In normal and Quark/ModernUI-reworked player inventory screens, Alt-left-click slot detection should use the screen's resolved slot before falling back to legacy coordinate scanning, avoiding `reason=no_slot` when the cursor is visibly over a player inventory slot.
  - 在普通玩家背包以及被 Quark/ModernUI 改写的玩家背包界面中，Alt+左键槽位检测应先使用界面解析出的槽位，再回退到旧坐标扫描，避免光标明显位于玩家背包槽上时出现 `reason=no_slot`。
  - If SophisticatedCore handles the click through its own `StorageScreenBase.slotClicked` override, the dedicated mixin cancels that override before `handleInventoryMouseClick`.
  - 如果 SophisticatedCore 通过自己的 `StorageScreenBase.slotClicked` 覆盖方法处理点击，专用 Mixin 会在 `handleInventoryMouseClick` 前取消该覆盖方法。
  - On all three loaders, `PICKUP` and `QUICK_MOVE` callbacks produced after an Alt-left-click are canceled as leaked inventory actions; the physical-click toggle has already happened in the container `mouseClicked` state-machine layer.
  - 在三个加载器上，Alt+左键之后产生的 `PICKUP` 与 `QUICK_MOVE` 回调都会作为漏进来的库存动作被取消；物理点击的收藏切换已经在容器 `mouseClicked` 状态机层完成。
  - Mouse Tweaks drag movement remains supported by suppressing Mouse Tweaks' original Alt inventory click and forwarding newly reached player-inventory slots from the Mouse Tweaks compatibility layer to this mod's toggle path.
  - Mouse Tweaks 拖动仍然受支持：Mouse Tweaks 原本的 Alt 库存点击会被压制，新经过的玩家背包槽由 Mouse Tweaks 兼容层转发到本模组的切换路径。
  - The original quick-move click is canceled after the lock toggle, so the item should not be moved by the backpack screen.
  - 锁定切换后原 quick-move 点击会被取消，因此物品不应被背包界面移动。
- Death lifecycle expectation:
- 死亡生命周期预期：
  - `keepInventory=true` or `restoreFrom keepEverything=true` keeps inventory contents through respawn because vanilla inventory restoration is marked as an internal scoped operation before lock guards or locked-empty-slot rerouting see its writes.
  - `keepInventory=true` 或 `restoreFrom keepEverything=true` 会让背包内容随重生保留，因为原版背包恢复会在锁槽守卫或锁定空槽回退看到写入前被标记为内部作用域操作。
  - With `[deathBehavior] preserveLockedSlotContents=true`, `keepInventory=false` death drops skip non-empty locked player-inventory slots, and clone/copy restores only those locked slots to the respawned player.
  - 当 `[deathBehavior] preserveLockedSlotContents=true` 时，`keepInventory=false` 的死亡掉落会跳过非空锁定玩家背包槽，clone/copy 只把这些锁定槽恢复到重生后的玩家。
  - The skip is implemented by temporarily removing locked stacks around vanilla `Inventory.dropAll()`, not by canceling `dropAll()`, so loader/mod death-drop listeners should continue to receive the normal death-drop event path.
  - 该跳过行为通过围绕原版 `Inventory.dropAll()` 临时移出锁定栈实现，而不是取消 `dropAll()`，因此加载器/模组的死亡掉落监听器应继续收到正常死亡掉落事件路径。
  - With `[deathBehavior] preserveLockedSlotContents=false`, `keepInventory=false` clears favorite state after death drops so empty/changed slots are not left locked unexpectedly.
  - 当 `[deathBehavior] preserveLockedSlotContents=false` 时，`keepInventory=false` 死亡掉落后会清空收藏状态，避免空槽或变化后的槽位继续意外锁定。
- Persistence expectation:
- 持久化预期：
  - Dual-install singleplayer and dedicated-server saves are stored in `<world>/data/neo_favorite_items/players/<uuid>.dat`.
  - 双端单人与专用服务端存储在 `<世界目录>/data/neo_favorite_items/players/<uuid>.dat`。
  - Existing files from the old wrong root `<game>/data/neo_favorite_items/players/<uuid>.dat` are migrated to the world path and then removed.
  - 旧错误根目录 `<游戏目录>/data/neo_favorite_items/players/<uuid>.dat` 中的既有文件会迁移到世界路径并随后删除。

## Manual Validation Completed

## 已完成实机验证

- NeoForge dedicated-server installation modes:
- NeoForge 真实专用服务端安装模式：
  - client mod present, server absent
  - 客户端安装、服务端未安装
  - client absent, server mod present
  - 客户端未安装、服务端安装
  - both sides present
  - 双端都安装
- NeoForge drop behavior:
- NeoForge 丢弃行为：
  - normal selected-hotbar drop
  - 当前快捷栏普通丢弃
  - bypass key held
  - 按住旁路键时的行为
- NeoForge composite movement behavior:
- NeoForge 复合移动行为：
  - GUI and GUI-outside offhand swap (`F`) with locked empty and non-empty offhand slots
  - GUI 内外副手交换（`F`），覆盖锁定空副手槽和锁定非空副手槽
  - Shift-click equippable armor into locked empty armor slots
  - Shift 点击可装备护甲进入锁定空护甲槽
  - AE2 terminal space-left-click `MOVE_REGION` into and out of locked player inventory slots
  - AE2 终端空格+左键 `MOVE_REGION` 对锁定玩家背包槽的放入与取出

## Remaining Manual Checks

## 仍需手工验证

- Fabric and Forge dedicated-server join validation for:
- Fabric、Forge 真实专用服务端联机验证：
  - client mod present, server absent
  - 客户端安装、服务端未安装
  - client absent, server mod present
  - 客户端未安装、服务端安装
  - both sides present
  - 双端都安装
- Fabric and Forge in-game validation of selected-hotbar drop blocking, including:
- Fabric、Forge 仍需验证 GUI 外快捷栏丢弃拦截：
  - normal drop (`Q`)
  - 普通丢弃（`Q`）
  - full-stack drop (`Ctrl+Q`)
  - 整组丢弃（`Ctrl+Q`）
  - bypass key held
  - 按住旁路键时的行为
- Fabric and Forge in-game validation of composite movement blocking:
- Fabric、Forge 仍需验证复合移动拦截：
  - GUI and GUI-outside offhand swap (`F`) with locked empty and non-empty offhand slots
  - GUI 内外副手交换（`F`），覆盖锁定空副手槽和锁定非空副手槽
  - Shift-click equippable armor into locked empty armor slots
  - Shift 点击可装备护甲进入锁定空护甲槽
  - AE2 terminal space-left-click MOVE_REGION into and out of locked player inventory slots
  - AE2 终端空格+左键 MOVE_REGION 对锁定玩家背包槽的放入与取出
- NeoForge in-game validation of Quark sorting with locked player main-inventory slots.
- NeoForge 仍需实机验证 Quark 排序遇到已锁玩家主背包槽时的行为。
- NeoForge in-game validation of Quark hotbar changer (`Z`) swaps with locked hotbar and main-inventory slots.
- NeoForge 仍需实机验证 Quark 快捷栏切换（`Z`）在锁定快捷栏和主背包槽时的交换行为。
- In-game validation of ClientSort collect and sort on the loaders where ClientSort is installed.
- 在安装 ClientSort 的加载器上实机验证 ClientSort collect 与 sort。
- Better Experience fast-storage compatibility still needs in-game validation with a locked source slot next to a matching nearby container stack.
- Better Experience 一键存储兼容仍需实机验证：玩家已锁来源槽旁边存在可合并的附近容器同类 stack 时，锁槽物品不应被拿走。
- Wrench Finder compatibility still needs in-game validation with a locked direct source, mixed locked/unlocked matches, a locked offhand source, an unlocked control, and a non-empty main hand. Locked sources must remain unchanged and no duplicate or main-hand item loss may occur.
- Wrench Finder 兼容仍需实机验证锁定直接来源、锁定/未锁匹配项并存、锁定副手来源、全未锁对照及主手非空场景。锁定来源必须保持不变，且不得复制或丢失原主手物品。
- NeoForge in-game validation of Sophisticated Backpacks Alt-left-click slot locking after the `QUICK_MOVE` click-path fix.
- NeoForge 仍需实机验证 Sophisticated Backpacks 在修复 `QUICK_MOVE` 点击路径后的 Alt+左键槽位锁定行为。
- Manual matrix validation for persistence paths and lifecycle timing:
- 持久化路径与生命周期时机的手工矩阵验证：
  - client-only join to unmodded server
  - 仅客户端安装加入未安装服务端
  - client+server installed on dedicated server
  - 专用服务端双端安装
  - singleplayer integrated server
  - 单人集成服务端

## Notes

## 说明

- This document is a delivery-facing verification summary. It complements `30-report-tests.md`, which focuses on automated evidence and coverage metrics.
- 本文档是面向交付的修复验证摘要，它与 `30-report-tests.md` 互补，后者更侧重自动化证据和覆盖率指标。
