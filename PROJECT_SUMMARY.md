
# New Item Favorites - Minecraft Mod Project

## 项目概述

这是一个为 Minecraft 1.21.1 版本设计的多平台模组，支持 Fabric、Forge 和 NeoForge 三个加载器。该模组提供了强大的物品栏收藏和锁定功能。

## 包名结构

- **主包名**: `mycraft.yuyears.newitemfavorites`
- **Mod ID**: `new_item_favorites`

## 核心功能

### 1. 配置系统 (TOML格式)

配置文件位置: `config/new-item-favorites.toml`

**主要配置项**:

- **general**: 通用设置
  - `lockEmptySlots`: 是否允许锁定空槽位
  - `autoUnlockEmptySlots`: 空槽位是否自动解锁
  - `allowItemsIntoLockedEmptySlots`: 是否允许物品进入锁定的空槽位

- **lockBehavior**: 锁定行为
  - `preventClick`: 阻止点击
  - `preventDrop`: 阻止丢弃
  - `preventQuickMove`: 阻止快速移动
  - `preventShiftClick`: 阻止Shift点击
  - `preventDrag`: 阻止拖拽
  - `preventSwap`: 阻止交换
  - `allowBypassWithKey`: 允许使用特殊键绕过

- **slotBehavior**: 槽位行为
  - `moveBehavior`: 物品移动时收藏状态的行为 (FOLLOW_ITEM / STAY_AT_POSITION)

- **overlay**: 覆盖层样式
  - 多种样式选项: LOCK_ICON, BORDER_GLOW, COLOR_OVERLAY, CHECKMARK, STAR
  - 可配置颜色和透明度

- **feedback**: 反馈设置
  - 视觉反馈和音效反馈

- **keybindings**: 按键绑定
  - `toggleFavoriteKey`: 切换收藏的按键 (默认: F)
  - `bypassLockKey`: 绕过锁定的按键 (默认: Left Control)

### 2. 核心类结构

**公共模块 (common)**:
- [NewItemFavoritesConfig.java](file:///workspace/common/src/main/java/mycraft/yuyears/newitemfavorites/NewItemFavoritesConfig.java) - 配置数据模型
- [ConfigManager.java](file:///workspace/common/src/main/java/mycraft/yuyears/newitemfavorites/ConfigManager.java) - 配置管理器
- [FavoritesManager.java](file:///workspace/common/src/main/java/mycraft/yuyears/newitemfavorites/FavoritesManager.java) - 收藏管理器
- [NewItemFavoritesMod.java](file:///workspace/common/src/main/java/mycraft/yuyears/newitemfavorites/NewItemFavoritesMod.java) - 主模组类

**平台实现**:
- Fabric: [NewItemFavoritesFabric.java](file:///workspace/fabric/src/main/java/mycraft/yuyears/newitemfavorites/fabric/NewItemFavoritesFabric.java)
- Forge: [NewItemFavoritesForge.java](file:///workspace/forge/src/main/java/mycraft/yuyears/newitemfavorites/forge/NewItemFavoritesForge.java)
- NeoForge: [NewItemFavoritesNeoForge.java](file:///workspace/neoforge/src/main/java/mycraft/yuyears/newitemfavorites/neoforge/NewItemFavoritesNeoForge.java)

### 3. 当前实现状态

✅ **已完成**:
- 项目基础架构和多平台Gradle配置
- 核心数据模型和TOML配置系统
- 平台无关的核心逻辑
- Fabric、Forge、NeoForge三个平台的集成
- 基础按键绑定和槽位收藏切换功能
- 语言文件 (英文和中文)

🔄 **待实现** (需要进一步开发):
- 客户端UI和Overlay渲染系统
- 槽位锁定行为的拦截和验证
- 服务端权威验证
- 物品移动时的收藏状态处理
- 音效和视觉反馈系统
- Mixin注入来拦截物品栏操作

## 项目构建

项目使用 Gradle 构建系统，支持分别为三个平台构建:

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

## 开发扩展

### 添加新的Overlay样式

在 [NewItemFavoritesConfig.java](file:///workspace/common/src/main/java/mycraft/yuyears/newitemfavorites/NewItemFavoritesConfig.java) 的 `OverlayStyle` 枚举中添加新样式，然后在各平台的渲染代码中实现相应的绘制逻辑。

### 添加新的配置项

1. 在 [NewItemFavoritesConfig.java](file:///workspace/common/src/main/java/mycraft/yuyears/newitemfavorites/NewItemFavoritesConfig.java) 中添加配置字段
2. 在 [ConfigManager.java](file:///workspace/common/src/main/java/mycraft/yuyears/newitemfavorites/ConfigManager.java) 中添加解析和保存逻辑
3. 在 `CONFIG_COMMENTS` 中添加配置说明

### 实现物品栏操作拦截

需要使用 Mixin 来注入到以下位置:
- 物品点击事件
- 物品丢弃事件
- Shift点击事件
- 物品拖拽事件
- 槽位交换事件

## 注意事项

- 模组设计为客户端必需，服务端可选
- 服务端安装时可以提供权威验证
- 配置文件带有详细的中文注释
- 支持中英文双语
