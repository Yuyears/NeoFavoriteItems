
# New Item Favorites Mod

## 项目概述

这是一个为 Minecraft 1.21.1 版本设计的多平台模组，支持 Fabric、Forge 和 NeoForge 三个加载器。该模组提供了强大的物品栏收藏和锁定功能，帮助玩家更好地管理物品栏。

## 核心功能

### 1. 物品栏收藏系统
- 按 `F` 键切换物品栏槽位的收藏状态
- 收藏的槽位会显示视觉标记（可配置样式）
- 支持多种 overlay 样式：锁定图标、边框发光、颜色覆盖、对勾、星星

### 2. 锁定行为控制
- 可配置阻止的操作：点击、丢弃、快速移动、Shift点击、拖拽、交换
- 支持按住 `Left Control` 键绕过锁定限制
- 可配置空槽位的锁定行为

### 3. 数据持久化
- **单人游戏**：数据保存在存档文件夹中
- **多人游戏**：数据保存在服务器端，确保跨服务器的一致性
- **仅客户端**：数据保存在游戏根目录的 `itemfavorites` 文件夹中

### 4. 高度可配置
- **配置文件**：`config/new-item-favorites.toml`
- **支持的配置项**：
  - 锁定行为设置
  - Overlay 样式和颜色
  - 音效和视觉反馈
  - 按键绑定

## 安装方法

1. **Fabric**：将模组文件放入 `mods` 文件夹
2. **Forge**：将模组文件放入 `mods` 文件夹
3. **NeoForge**：将模组文件放入 `mods` 文件夹

## 构建项目

使用 Gradle 构建系统：

```bash
# 构建所有平台
./gradlew build

# 单独构建 Fabric
./gradlew fabric:build

# 单独构建 Forge
./gradlew forge:build

# 单独构建 NeoForge
./gradlew neoforge:build
```

## 配置文件

首次运行游戏时，会自动生成配置文件 `config/new-item-favorites.toml`，包含详细的中文注释。

## 按键绑定

- **切换收藏**：`F`（可在配置文件中修改）
- **绕过锁定**：`Left Control`（可在配置文件中修改）

## 开发说明

### 项目结构
- **common**：平台无关的核心代码
- **fabric**：Fabric 平台实现
- **forge**：Forge 平台实现
- **neoforge**：NeoForge 平台实现

### 主要类
- `NewItemFavoritesConfig`：配置数据模型
- `ConfigManager`：配置管理器
- `FavoritesManager`：收藏状态管理器
- `DataPersistenceManager`：数据持久化管理器
- `OverlayRenderer`：Overlay 渲染系统

## 许可证

MIT 许可证
