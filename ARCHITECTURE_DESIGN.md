# Neo Favorite Items 架构说明

最后更新: 2026-04-23

本文档记录当前项目结构和实现边界。它描述的是当前仓库状态，而不是早期迁移草案。

## 设计目标

- 让收藏/锁定规则尽量位于平台无关代码中。
- 使用统一逻辑槽位模型，避免把 GUI 槽位 id 当作持久化身份。
- 平台层只负责注册、按键、网络、Mixin/事件和渲染入口。
- 客户端保持响应速度，服务端安装时负责权威状态和同步。
- Fabric、Forge、NeoForge 尽量共享 common 层服务。

## 模块结构

```text
common/
  src/main/java/mycraft/yuyears/neofavoriteitems/
    domain/        纯领域模型
    application/   用例服务和同步服务
    integration/   Minecraft 槽位到逻辑槽位的适配
    persistence/   收藏数据读写
    render/        Overlay 抽象和颜色工具
    *.java         配置、状态、公共初始化和日志
  src/main/resources/assets/neo_favorite_items/
    lang/          en_us.json, zh_cn.json
    textures/      Overlay PNG 材质

fabric/
  Fabric 入口、客户端入口、网络 payload、槽位解析、Mixin、Fabric 渲染器

forge/
  Forge 入口、网络 payload、Mixin、Forge 事件/按键/渲染器

neoforge/
  NeoForge 入口、网络 payload、Mixin、NeoForge 事件/按键/渲染器
```

## 分层职责

### Domain

位置: `common/.../domain`

- `LogicalSlotIndex`: 统一玩家物品栏槽位索引，范围 `0..40`
- `InteractionType`: 点击、丢弃、快速移动、拖拽、交换等交互类型
- `InteractionDecision`: 允许、旁路允许、拒绝及原因

该层不依赖加载器 API。

### Application

位置: `common/.../application`

- `InteractionGuardService`: 根据收藏状态、配置、旁路键和槽位内容做拦截决策
- `ServerFavoriteService`: 服务端收藏切换、校验、修订号、旁路状态和服务端交互保护
- `ClientFavoriteSyncService`: 客户端全量/增量同步、过期修订过滤和同步缺口检测

### Integration

位置: `common/.../integration`

- `SlotMappingService`: 在 Minecraft 玩家背包索引和 `LogicalSlotIndex` 之间转换。
- 当前收藏/锁定只面向玩家物品栏槽位，不面向箱子、熔炉等容器自身槽位。

### Persistence

位置: `common/.../persistence`

- `DataPersistenceManager`: 按玩家 UUID 保存和加载收藏数据。
- 支持客户端本地目录、单人世界目录和服务端玩家数据场景。

### Render

位置: `common/.../render`

- `OverlayRenderer`: 平台渲染器的公共基类和颜色/资源工具。
- 平台模块负责把公共材质解析为各自加载器可用的渲染调用。

## 平台职责

### Fabric

- `NeoFavoriteItemsFabric`: 公共初始化、服务端生命周期、玩家加入/离开、服务端全量同步。
- `NeoFavoriteItemsFabricClient`: 客户端配置、按键、Overlay、客户端同步接收、世界切换加载/保存。
- `FabricFavoriteNetworking`: Toggle、Full Sync、Delta Sync、Bypass Key State payload。
- Mixin 覆盖普通容器、创造模式槽位、物品栏交互和玩家背包变更保护。

### Forge

- `NeoFavoriteItemsForge`: Mod 入口、事件总线、按键、GUI Layer、玩家登录/登出、服务端同步。
- `ForgeFavoriteNetworking`: Forge SimpleChannel 网络包注册与收发。
- Mixin 和 Forge 适配类复用 common 决策服务。

### NeoForge

- `NeoFavoriteItemsNeoForge`: Mod 入口、NeoForge 事件、按键、GUI Layer、payload handler、玩家登录/登出。
- `NeoForgeFavoriteNetworking`: NeoForge payload 注册与收发。
- Mixin 和 NeoForge 适配类复用 common 决策服务。

## 核心流程

### 切换收藏

1. 客户端检测锁定操作键和目标玩家物品栏槽位。
2. 平台槽位解析把目标转换为 `LogicalSlotIndex`。
3. 有服务端支持时发送 Toggle 请求。
4. `ServerFavoriteService` 校验空槽配置、更新收藏状态、保存数据并生成修订号。
5. 服务端发送全量或增量同步。
6. 客户端通过 `ClientFavoriteSyncService` 应用同步结果并刷新本地展示。

### 交互拦截

1. 平台 Mixin/事件捕获点击、快速移动、拖拽、丢弃、交换或外部移动。
2. 只处理玩家自己的物品栏槽位。
3. `InteractionGuardService` 根据配置和收藏状态返回决策。
4. 平台层按决策取消交互或允许继续。
5. 旁路键状态由客户端按键轮询同步到服务端。

### Overlay 渲染

1. 平台渲染器枚举 GUI 槽位或快捷栏槽位。
2. 槽位映射到 `LogicalSlotIndex`。
3. 根据收藏状态、锁定操作键、旁路键和配置选择 Overlay 样式。
4. PNG 材质按实际尺寸采样并缩放到 16x16 槽位。
5. 根据配置决定绘制在物品图标前方或后方。

## 当前约束

- 收藏身份只覆盖玩家物品栏 `0..40`。
- 容器自身槽位收藏尚未实现。
- 物品移动策略已有配置入口，旁路状态下的复杂移动追踪仍需继续完善。
- 视觉反馈和音效实际播放仍需补齐。
- 尚未建立自动化测试套件。

## 维护约定

- 新逻辑优先放入 `common`，平台层保持薄适配。
- 持久化和网络只传递逻辑槽位或玩家背包索引，不传递 GUI slot id。
- 新增 Overlay 样式时，同时更新:
  - `NeoFavoriteItemsConfig.OverlayStyle`
  - `OverlayRenderer` 资源路径
  - 三个平台渲染器
  - `common/src/main/resources/assets/neo_favorite_items/textures`
- 新增配置项时，同时更新:
  - `NeoFavoriteItemsConfig`
  - `ConfigManager`
  - 语言文件或配置注释
  - `README.md` 的配置摘要
