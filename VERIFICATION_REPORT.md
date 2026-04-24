# Verification Report

# 修复验证报告

Date: 2026-04-24

日期：2026-04-24

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
  - Forge: compatibility display handled via custom `DisplayTest`
  - Forge：通过自定义 `DisplayTest` 处理兼容性显示
  - NeoForge: compatibility display set to ignore side mismatch in `neoforge.mods.toml`
  - NeoForge：在 `neoforge.mods.toml` 中设置忽略双端不对称带来的显示兼容性问题
- Dual-install expectation:
- 双端安装预期：
  - Full/incremental sync and bypass-state sync remain enabled
  - 全量/增量同步和旁路键状态同步保持启用
  - Local fallback is bypassed when the remote channel is present
  - 当远端通道存在时，本地回退逻辑不会生效
  - Persistence uses the active world save under `data/neo_favorite_items/players/<uuid>.dat`
  - 持久化使用当前世界目录下的 `data/neo_favorite_items/players/<uuid>.dat`

## Remaining Manual Checks

## 仍需手工验证

- Real dedicated-server join validation for:
- 真实专用服务端联机验证：
  - client mod present, server absent
  - 客户端安装、服务端未安装
  - client absent, server mod present
  - 客户端未安装、服务端安装
  - both sides present
  - 双端都安装
- In-game validation of selected-hotbar drop blocking on all three loaders, including:
- 三个平台都需要验证 GUI 外快捷栏丢弃拦截：
  - normal drop (`Q`)
  - 普通丢弃（`Q`）
  - full-stack drop (`Ctrl+Q`)
  - 整组丢弃（`Ctrl+Q`）
  - bypass key held
  - 按住旁路键时的行为
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
