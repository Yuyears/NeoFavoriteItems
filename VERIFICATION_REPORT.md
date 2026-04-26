# Verification Report

# 修复验证报告

Date: 2026-04-26

日期：2026-04-26

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
17. Persistence lifecycle now follows world start full read, player join partial read, player leave incremental save, and world stop full save.
18. 持久化生命周期现在遵循“世界启动完整读取、玩家进入部分读取、玩家退出增量保存、世界关闭完整保存”。
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
- AE2 terminal expectation:
- AE2 终端预期：
  - Space-left-click region moves from locked player inventory slots are canceled at the AE2 shared quick-move entrypoint.
  - 从锁定玩家背包槽出发的空格+左键区域转移会在 AE2 公共快速移动入口被取消。
  - Space-left-click region moves from the AE2 network into locked player inventory slots are canceled before AE2 starts moving repeated stacks.
  - 从 AE2 网络向锁定玩家背包槽批量放入的空格+左键区域转移会在 AE2 开始重复搬运前被取消。

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

- This document is a delivery-facing verification summary. It complements `TEST_REPORT.md`, which focuses on automated evidence and coverage metrics.
- 本文档是面向交付的修复验证摘要，它与 `TEST_REPORT.md` 互补，后者更侧重自动化证据和覆盖率指标。
