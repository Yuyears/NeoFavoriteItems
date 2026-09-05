# Neo Favorite Items Roadmap

# Neo Favorite Items 路线图

Last updated: 2026-09-06

最后更新：2026-09-06

## Version Plan

## 版本计划

- Current baseline: Minecraft `1.21.1`, Java `21`, three loaders.
- 当前基线：Minecraft `1.21.1`、Java `21`、Fabric/Forge/NeoForge。
- Migration branches: `mc/1.20.1`, `mc/1.21.1`, `mc/26.1.2`.
- 移植分支：`mc/1.20.1`、`mc/1.21.1`、`mc/26.1.2`。
- Each branch owns mappings, loader versions, Mixin signatures, and resources.
- 每个分支独立维护映射、加载器版本、Mixin 签名和资源。
- Port pure rules and tests first; port version-sensitive adapters separately.
- 先移植纯规则和测试，再单独移植版本敏感适配层。

## Artifact Naming

## 产物命名

```text
neo_favorite_items-<minecraft>-<loader>-<channel>-<modVersion>-<build>.jar
```

Example: `neo_favorite_items-1.21.1-neoforge-release-0.0.2-build1.jar`.

Build number and channel come from CI or explicit Gradle properties. Gradle must not rewrite `gradle.properties`.

构建号和渠道由 CI 或显式 Gradle 参数提供。Gradle 不得回写 `gradle.properties`。

## P0: Release And Porting

## P0：发布与版本移植

- [ ] Port and compile all three loaders on `mc/1.20.1`.
- [ ] 在 `mc/1.20.1` 编译三个加载器。
- [ ] Port and compile all three loaders on `mc/26.1.2` after availability is confirmed.
- [ ] 在确认依赖可用后，于 `mc/26.1.2` 编译三个加载器。
- [ ] Run Fabric and Forge multiplayer matrix: client-only, server-only, both sides.
- [ ] 实测 Fabric、Forge 多人矩阵：仅客户端、仅服务端、双端安装。
- [ ] Validate Mouse Tweaks drag-click lock toggling on all three loaders.
- [ ] 实测三个加载器的 Mouse Tweaks 拖动点击锁定。
- [ ] Validate creative mode, containers, hotbar swaps, dragging, and external transfers on each branch.
- [ ] 在每个分支验证创造模式、容器、快捷栏交换、拖拽和外部转移。

Completed private runtime validation: Quark sorting/hotbar changer, Inventory Tweaks ReFoxed sorting, and Sophisticated Backpacks lock interaction. These are recorded in verification reports, not active TODOs.

已完成私下实机验证：Quark 整理/快捷栏切换、Inventory Tweaks ReFoxed 整理、Sophisticated Backpacks 锁定交互。结果记录在验证报告，不再作为待办。

## P1: Core Stability

## P1：核心稳定性

- [ ] Split `ServerFavoriteService` by responsibility: toggle, protection, transaction, and death handling.
- [ ] 按职责拆分 `ServerFavoriteService`：切换、保护、事务、死亡处理。
- [ ] Keep `InteractionGuardService` as the single interaction decision entrypoint.
- [ ] 保持 `InteractionGuardService` 为唯一交互判定入口。
- [ ] Add persistence schema version and fixture tests, retaining one-time legacy migration.
- [ ] 增加持久化 schema 版本和 fixture 测试，保留旧格式一次性迁移。
- [ ] Add invalid slot, revision, and player-context network tests.
- [ ] 增加非法槽位、revision 和玩家上下文网络测试。
- [ ] Define bypass-key semantics for complex moves and test `FOLLOW_ITEM`/`STAY_AT_POSITION`.
- [ ] 明确复杂移动旁路键语义并测试 `FOLLOW_ITEM`/`STAY_AT_POSITION`。
- [ ] Add three-loader resource and Mixin configuration inventory checks in CI.
- [ ] 在 CI 增加三加载器资源和 Mixin 配置清单校验。

## P2: Maintenance And UX

## P2：维护与体验

- [ ] Remove unused helpers and repeated platform code only with concrete file-level scope.
- [ ] 仅在明确文件范围后清理未使用 helper 和平台重复代码。
- [ ] Gradually modernize overlay rendering during version ports.
- [ ] 在版本移植过程中渐进式现代化 Overlay 渲染。
- [ ] Evaluate feedback triggers and config UI only after core porting is stable.
- [ ] 核心版本移植稳定后再评估反馈触发和配置界面。

## Deferred

## 暂缓

- MixinExtras: reconsider only after measured cross-loader conflict or dependency convergence.
- MixinExtras：仅在实测跨加载器冲突或依赖收敛后重新评估。
- Custom overlay texture system: wait until renderer and version branches stabilize.
- 自定义 Overlay 纹理系统：待渲染和版本分支稳定后再评估。
- Generic compatibility for untested third-party mods: require a reproducible case.
- 未实测第三方模组的泛化兼容：必须先有可复现问题。

## Known Limitations

## 已知限制

- Favorite state covers player inventory slots only; container-owned slots remain unsupported.
- 收藏状态目前只覆盖玩家物品栏槽位，不支持容器自身槽位。
- Sophisticated empty-slot drag marking remains limited when Mouse Tweaks emits no drag-enter sample.
- 当 Mouse Tweaks 不发出拖动进入采样时，Sophisticated 空槽拖动标记仍有限制。
