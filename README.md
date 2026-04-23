# Neo Favorite Items

`Neo Favorite Items` 是一个面向 Minecraft 1.21.1 的多平台物品栏收藏/锁定模组。玩家可以把自己的物品栏槽位标记为收藏槽位，并通过交互拦截、Overlay 提示和数据持久化减少误操作。

- Mod ID: `neo_favorite_items`
- 包名: `mycraft.yuyears.neofavoriteitems`
- 版本: `0.0.1-alpha-build1`
- Java: 21
- 构建系统: Gradle Kotlin DSL + Architectury Loom
- 支持平台: Fabric、Forge、NeoForge

## 功能概览

- 按住锁定操作键后点击玩家物品栏槽位，可切换收藏状态。
- 默认按键:
  - 锁定操作键: `Left Alt`
  - 旁路键: `Left Control`
- 收藏状态使用统一逻辑槽位索引:
  - `0..8`: 快捷栏
  - `9..35`: 主背包
  - `36..39`: 护甲栏
  - `40`: 副手
- 已收藏槽位会在 GUI 和快捷栏 HUD 中显示 Overlay。
- 支持 `border`、`classic`、`framework`、`highlight`、`brackets`、`lock`、`mark`、`tag`、`star` 九种 PNG 材质。
- Overlay 颜色支持 `0xAARRGGBB` 和 `luv(L, u, v, alpha)` 两种写法。
- 支持阻止点击、丢弃、快速移动、Shift 点击、拖拽、交换等行为。
- 服务端安装时会走服务端权威状态与同步；客户端仍保留本地响应和本地持久化能力。
- 配置文件和界面文本提供中英文资源。

## 项目结构

```text
.
├─ common/       平台无关代码、配置、状态、持久化、渲染抽象和资源
├─ fabric/       Fabric 入口、网络、Mixin、按键和渲染适配
├─ forge/        Forge 入口、网络、Mixin、事件、按键和渲染适配
├─ neoforge/     NeoForge 入口、网络、Mixin、事件、按键和渲染适配
├─ gradle/       Gradle Wrapper
├─ build.gradle.kts
├─ settings.gradle.kts
└─ gradle.properties
```

公共源码位于 `common/src/main/java/mycraft/yuyears/neofavoriteitems`，主要分为:

- `domain`: `LogicalSlotIndex`、`InteractionType`、`InteractionDecision`
- `application`: `InteractionGuardService`、`ServerFavoriteService`、`ClientFavoriteSyncService`
- `integration`: `SlotMappingService`
- `persistence`: `DataPersistenceManager`
- `render`: `OverlayRenderer`
- 根包: `ConfigManager`、`FavoritesManager`、`NeoFavoriteItemsConfig`、`DebugLogger`

公共资源位于 `common/src/main/resources/assets/neo_favorite_items`，平台模块通过 `processResources` 合并这些资源。

## 构建

```powershell
# 构建所有平台
.\gradle.bat build

# 单独构建
.\gradle.bat :fabric:build
.\gradle.bat :forge:build
.\gradle.bat :neoforge:build

# 只编译 Java
.\gradle.bat :common:compileJava :fabric:compileJava :forge:compileJava :neoforge:compileJava
```

构建产物:

- `fabric/build/libs/neo_favorite_items-fabric-0.0.1-alpha-build1.jar`
- `forge/build/libs/neo_favorite_items-forge-0.0.1-alpha-build1.jar`
- `neoforge/build/libs/neo_favorite_items-neoforge-0.0.1-alpha-build1.jar`

## 开发运行

```powershell
.\gradle.bat :fabric:runClient
.\gradle.bat :forge:runClient
.\gradle.bat :neoforge:runClient
```

## 配置

首次运行后会生成:

```text
config/neo-favorite-items.toml
```

主要配置分组:

- `general`: 空槽锁定、空槽自动解锁、是否允许物品进入锁定空槽
- `lockBehavior`: 不同交互类型的拦截策略和旁路键行为
- `slotBehavior`: 收藏状态随物品移动或固定在槽位
- `overlay`: Overlay 样式、颜色、透明度和前景渲染开关
- `feedback`: 文本、音效等反馈设置
- `debug`: 诊断日志开关

按键绑定通过 Minecraft 控制设置管理，不写入模组配置文件。

## 文档

- `ARCHITECTURE_DESIGN.md`: 当前架构、分层职责和关键运行流程
- `TODO.md`: 仍需完成或验证的事项
- `README.md`: 面向使用和开发入口的简要说明

## 许可证

MIT License
