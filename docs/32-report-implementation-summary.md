# Implementation Summary

# 实现摘要

Last updated: 2026-05-10

最后更新：2026-05-10

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
