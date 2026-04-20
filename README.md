
# Neo Favorite Items

## 项目概述

这是一个为 Minecraft 1.21.1 设计的多平台物品栏收藏/锁定模组，Mod ID 为 `neo_favorite_items`，当前包名为 `mycraft.yuyears.neofavoriteitems`。项目支持 Fabric、Forge 和 NeoForge，当前 Fabric 主功能路径已基本完成。

## 核心功能

### 1. 物品栏收藏系统
- 按住游戏内按键绑定的锁定操作键（默认 `Left Alt`）时，左键点击玩家物品栏槽位切换收藏状态。
- 普通容器、创造模式标签页快捷栏、创造模式物品栏页均按真实玩家物品栏索引处理。
- 已收藏槽位在 GUI 与退出 GUI 后的快捷栏 HUD 中显示 overlay。
- 支持 `border`、`classic`、`framework`、`highlight`、`brackets`、`lock`、`mark`、`tag`、`star` 九种明度/形状 PNG 材质。
- Overlay PNG 会按实际图片尺寸采样并缩放到 16x16 槽位，16x16、32x32、64x64 等单图材质不需要额外配置尺寸。

### 2. 锁定行为控制
- 可配置阻止的操作：点击、丢弃、快速移动、Shift点击、拖拽、交换
- 支持按住游戏内按键绑定的旁路键（默认 `Left Control`）临时绕过锁定限制
- 可配置空槽位的锁定行为

### 3. 数据持久化
- **单人游戏**：数据保存在存档文件夹中
- **多人游戏**：数据保存在服务器端，确保跨服务器的一致性
- **仅客户端**：数据保存在游戏根目录的 `itemfavorites` 文件夹中

### 4. 高度可配置
- **配置文件**：`config/neo-favorite-items.toml`
- **支持的配置项**：
  - 锁定行为设置
  - Overlay 样式、透明度和颜色
  - 已锁、可收藏提示、可解锁提示是否显示在物品图标前方
  - 音效和视觉反馈
  - Debug 日志开关

Overlay 颜色支持旧版 `0xAARRGGBB`，也支持 CIE L*u*v* 写法，例如：

```toml
lockedOverlayColor = "luv(86, 10, 80, 1.0)"
```

按键在 Minecraft 控制设置中注册，不再写入模组配置文件。

## 安装方法

1. **Fabric**：将模组文件放入 `mods` 文件夹
2. **Forge**：将模组文件放入 `mods` 文件夹
3. **NeoForge**：将模组文件放入 `mods` 文件夹

## 构建项目

使用 Gradle 构建系统：

```bash
# 构建所有平台
.\gradle.bat

# 单独构建 Fabric
.\gradle.bat :fabric:build

# 单独构建 Forge
.\gradle.bat :forge:build

# 单独构建 NeoForge
.\gradle.bat :neoforge:build
```

## 配置文件

首次运行游戏时，会自动生成配置文件 `config/neo-favorite-items.toml`，包含中英双语注释。

## 按键绑定

- **锁定操作键**：`Left Alt`（在 Minecraft 控制设置中修改）
- **旁路键**：`Left Control`（在 Minecraft 控制设置中修改）

## 开发说明

### 项目结构
- **common**：平台无关的核心代码
- **fabric**：Fabric 平台实现
- **forge**：Forge 平台实现
- **neoforge**：NeoForge 平台实现

### 主要类
- `NeoFavoriteItemsConfig`：配置数据模型
- `ConfigManager`：配置管理器
- `FavoritesManager`：收藏状态管理器
- `DataPersistenceManager`：数据持久化管理器
- `OverlayRenderer`：Overlay 渲染系统

## 许可证

MIT 许可证
