
# Neo Favorite Items - Minecraft Mod Project

## 项目概述

这是一个为 Minecraft 1.21.1 版本设计的多平台模组，支持 Fabric、Forge 和 NeoForge 三个加载器。该模组提供了强大的物品栏收藏和锁定功能，允许玩家标记重要物品槽位并防止意外操作。

**当前版本**: 0.0.1-alpha  
**包名**: `mycraft.yuyears.neofavoriteitems`  
**Mod ID**: `neo_favorite_items`

## 包名结构

- **主包名**: `mycraft.yuyears.neofavoriteitems`
- **Mod ID**: `neo_favorite_items`

## 核心功能

### 1. 配置系统 (TOML格式)

配置文件位置: `config/neo-favorite-items.toml`

**主要配置项**:

- **general**: 通用设置
  - `lockEmptySlots`: 是否允许锁定空槽位 (默认: false)
  - `autoUnlockEmptySlots`: 空槽位是否自动解锁 (默认: true)
  - `allowItemsIntoLockedEmptySlots`: 是否允许物品进入锁定的空槽位 (默认: false)

- **lockBehavior**: 锁定行为控制
  - `preventClick`: 阻止点击已锁定槽位 (默认: true)
  - `preventDrop`: 阻止从已锁定槽位丢弃物品 (默认: true)
  - `preventQuickMove`: 阻止从已锁定槽位快速移动物品 (默认: true)
  - `preventShiftClick`: 阻止 Shift 点击已锁定槽位 (默认: true)
  - `preventDrag`: 阻止拖拽物品经过已锁定槽位 (默认: true)
  - `preventSwap`: 阻止与已锁定槽位交换物品 (默认: true)
  - `allowBypassWithKey`: 允许使用特殊键绕过锁定 (默认: true)

- **slotBehavior**: 槽位行为
  - `moveBehavior`: 物品移动时收藏状态的行为
    - `FOLLOW_ITEM`: 收藏状态跟随物品移动
    - `STAY_AT_POSITION`: 收藏状态固定在槽位位置

- **overlay**: 覆盖层样式
  - `lockedStyle`: 已锁定槽位的覆盖层样式 (默认: LOCK)
  - `holdingKeyLockedStyle`: 按住旁路键时已锁定槽位样式 (默认: LOCK)
  - `highlightStyle`: 按住锁定操作键时的提示样式 (默认: HIGHLIGHT)
  - 可用样式: BORDER, CLASSIC, FRAMEWORK, HIGHLIGHT, BRACKETS, LOCK, MARK, TAG, STAR, COLOR_OVERLAY
  - `lockedOverlayColor`: 已锁定槽位颜色，支持 `0xAARRGGBB` 或 `"luv(L, u, v, alpha)"`
  - `lockableHighlightColor`: 可收藏提示颜色，支持 `0xAARRGGBB` 或 `"luv(L, u, v, alpha)"`
  - `unlockableHighlightColor`: 可取消收藏提示颜色，支持 `0xAARRGGBB` 或 `"luv(L, u, v, alpha)"`
  - `lockedOverlayOpacity` / `lockableHighlightOpacity` / `unlockableHighlightOpacity`: 独立透明度
  - `bypassOverlayOpacityMultiplier`: 按住旁路键时已锁 overlay 的透明度乘数
  - `renderLockedOverlayInFront`: 已锁 overlay 是否显示在物品图标前方
  - `renderLockableHighlightInFront`: 可收藏提示 overlay 是否显示在物品图标前方
  - `renderUnlockableHighlightInFront`: 可解锁提示 overlay 是否显示在物品图标前方
  - PNG 材质会自动按实际尺寸采样并缩放到 16x16 槽位，不需要配置 texture size

- **feedback**: 反馈设置
  - `showVisualFeedback`: 显示视觉反馈 (默认: true)
  - `playSoundFeedback`: 播放声音反馈 (默认: true)
  - `feedbackSound`: 反馈音效 (默认: minecraft:block.note_block.hat)
  - `feedbackVolume`: 音量 (默认: 0.5)
  - `feedbackPitch`: 音高 (默认: 1.0)

- **debug**: 调试选项
  - `enabled`: 启用额外诊断日志 (默认: false)

按键绑定注册在 Minecraft 控制设置中：
- `key.neo_favorite_items.lock_operation`: 锁定操作键 (默认: Left Alt)
- `key.neo_favorite_items.bypass_lock`: 旁路键 (默认: Left Control)

### 2. 核心类结构

**公共模块 (common)**:

*核心管理类*
- `NeoFavoriteItemsMod.java` - 主模组类和公共初始化入口
- `ConfigManager.java` - TOML 配置管理器，负责加载、解析和保存配置
- `FavoritesManager.java` - 收藏槽位管理器，管理玩家的收藏状态
- `DebugLogger.java` - 调试日志工具

*领域层 (domain)*
- `LogicalSlotIndex.java` - 逻辑槽位索引值对象
- `InteractionType.java` - 交互类型枚举（点击、拖拽、丢弃等）
- `InteractionDecision.java` - 交互决策结果

*应用服务层 (application)*
- `InteractionGuardService.java` - 交互守卫服务，根据配置和收藏状态决定是否允许交互

*基础设施层 (infrastructure)*
- `DataPersistenceManager.java` - 数据持久化管理器，支持存档级和全局级数据存储
- `OverlayRenderer.java` - 抽象的覆盖层渲染器基类
- `SlotMappingService.java` - 槽位映射服务，处理玩家物品栏索引到逻辑槽位的转换

**平台特定实现**:

*Fabric 平台*
- `NeoFavoriteItemsFabric.java` / `NeoFavoriteItemsFabricClient.java` - Fabric 平台入口和客户端入口
- `FabricSlotResolver.java` / `FabricSlotInteractionHandler.java` - Fabric 槽位解析与点击处理
- `FabricOverlayRenderer.java` - Fabric 平台的 Overlay 渲染实现
- Mixin 注入器目录: `mixin/`

*Forge 平台*
- `NeoFavoriteItemsForge.java` - Forge 主类，使用 `@Mod` 注解和事件总线
- `ForgeOverlayRenderer.java` - Forge 平台的 Overlay 渲染实现
- 音效注册: 使用 DeferredRegister 注册反馈音效

*NeoForge 平台*
- `NeoFavoriteItemsNeoForge.java` - NeoForge 主类
- `NeoForgeOverlayRenderer.java` - NeoForge 平台的 Overlay 渲染实现

### 3. 当前实现状态

✅ **已完成的核心功能**:
- ✅ 项目基础架构和多平台 Gradle 配置（Kotlin DSL）
- ✅ 完整的 TOML 配置系统，包含详细的中文注释
- ✅ 平台无关的核心业务逻辑（领域层、应用服务层）
- ✅ Fabric、Forge、NeoForge 三个平台的完整集成
- ✅ 按键绑定系统（锁定操作键 + 绕过锁定键）
- ✅ 槽位收藏/取消收藏功能
- ✅ 数据持久化系统（支持存档级和全局级存储）
- ✅ 玩家登录/登出时的数据自动加载/保存
- ✅ 交互守卫服务（基于配置的拦截逻辑）
- ✅ Overlay 渲染系统抽象层和 Fabric 明度 PNG 着色实现
- ✅ Fabric 普通容器与创造模式玩家物品栏槽位拦截
- ✅ Fabric 创造模式 `SlotWrapper` 解包，统一映射到真实玩家物品栏索引
- ✅ Fabric GUI/HUD overlay 前景渲染，避免被物品图标遮挡
- ✅ Overlay 颜色支持 ARGB 与 CIE L*u*v* 双格式
- ✅ 语言文件支持（英文和中文）
- ✅ 自定义音效注册（Forge/NeoForge）
- ✅ 调试日志系统

🔄 **部分完成**:
- ⚠️ Forge/NeoForge 已保留基础 renderer 和编译通过状态，后续需要同步 Fabric 的创造模式与前景渲染细节
- ⚠️ 物品移动时的收藏状态处理入口已存在（FOLLOW_ITEM / STAY_AT_POSITION），旁路移动追踪细节仍需完善

📋 **待实现功能**:
- ❌ 视觉反馈效果（粒子、动画等）
- ❌ 音效反馈的实际播放逻辑
- ❌ 服务端权威验证机制
- ❌ 配置热重载功能
- ❌ GUI 界面用于批量管理收藏槽位

## 技术架构亮点

### 分层架构设计
- **领域层 (domain)**: 纯业务逻辑，无外部依赖
- **应用服务层 (application)**: 协调领域对象完成用例
- **基础设施层 (infrastructure)**: 持久化、渲染、配置等技术服务
- **接口适配层 (platform)**: 各平台特定的集成代码

### 设计模式应用
- **单例模式**: 所有 Manager 类采用单例模式
- **策略模式**: Overlay 样式可配置切换
- **观察者模式**: 事件驱动的玩家登录/登出处理
- **值对象模式**: LogicalSlotIndex 不可变值对象
- **服务定位器**: SlotMappingService 提供槽位映射服务

### 数据流
```
用户输入 → 按键检测 → 收藏状态变更 → 持久化存储
         ↓
    物品栏操作 → InteractionGuardService 验证 → 允许/拒绝
         ↓
    渲染循环 → OverlayRenderer → 绘制收藏标记
```

## 项目构建与运行

项目使用 Gradle Kotlin DSL 构建系统，支持分别为三个平台构建:

```bash
# 构建所有平台
.\gradle.bat

# 单独构建 Fabric
.\gradle.bat :fabric:build

# 单独构建 Forge
.\gradle.bat :forge:build

# 单独构建 NeoForge
.\gradle.bat :neoforge:build

# 运行开发环境
.\gradle.bat :fabric:runClient      # Fabric 客户端
.\gradle.bat :forge:runClient       # Forge 客户端
.\gradle.bat :neoforge:runClient    # NeoForge 客户端
```

**构建产物位置**:
- Fabric: `fabric/build/libs/`
- Forge: `forge/build/libs/`
- NeoForge: `neoforge/build/libs/`

## 开发扩展指南

### 添加新的 Overlay 样式

1. 在 `NeoFavoriteItemsConfig.java` 的 `OverlayStyle` 枚举中添加新样式
2. 在各平台的渲染器中实现绘制逻辑：
   - Fabric: `FabricOverlayRenderer.java`
   - Forge: `ForgeOverlayRenderer.java`
   - NeoForge: `NeoForgeOverlayRenderer.java`
3. 在 `OverlayRenderer.java` 中添加对应的纹理路径常量

### 添加新的配置项

1. 在 `NeoFavoriteItemsConfig.java` 的数据类中添加字段
2. 在 `ConfigManager.java` 的 `CONFIG_COMMENTS` 中添加配置说明
3. 在对应的 `setXxxValue()` 方法中添加解析逻辑
4. 确保配置文件的向后兼容性（使用 `appendMissingConfigEntries()` 方法）

### 实现物品栏操作拦截

**Fabric 平台**（使用 Mixin）:
1. 在 `fabric/src/main/java/mycraft/yuyears/neofavoriteitems/fabric/mixin/` 创建 Mixin 类
2. 注入到以下目标方法：
   - `ScreenHandler.slotClick()` - 物品点击
   - `PlayerEntity.dropItem()` - 物品丢弃
   - `ScreenHandler.quickMove()` - Shift 点击
   - `ScreenHandler.onMouseClick()` - 鼠标拖拽
3. 在 Mixin 中调用 `InteractionGuardService.evaluate()` 进行验证

**Forge/NeoForge 平台**（使用事件）:
1. 订阅 `ContainerEvent.Click` 等相关事件
2. 在事件处理器中调用 `InteractionGuardService.evaluate()`
3. 根据决策结果取消或修改事件

### 添加新的交互类型

1. 在 `InteractionType.java` 中添加新类型
2. 在 `InteractionGuardService.shouldBlockByConfig()` 中添加对应的判断逻辑
3. 如需新的配置项，按上述"添加新的配置项"步骤操作

### 数据持久化扩展

当前数据存储在：
- **单人游戏**: `<世界文件夹>/data/neo_favorite_items/players/<UUID>.dat`
- **多人游戏**: `<游戏目录>/itemfavorites/<服务器标识>/players/<UUID>.dat`

如需更改存储格式：
1. 修改 `FavoritesManager.serialize()` 和 `deserialize()` 方法
2. 考虑使用 NBT 或 JSON 格式替代当前的二进制格式
3. 实现数据迁移逻辑以兼容旧格式

## 注意事项与最佳实践

### 兼容性
- ✅ 模组设计为**客户端必需**，服务端可选
- ✅ 服务端安装时可提供额外的权威验证
- ✅ 配置文件带有详细的中文注释，便于用户理解
- ✅ 支持中英文双语界面

### 性能优化
- 使用单例模式避免重复创建管理器实例
- 收藏数据仅在玩家登录/登出时进行 I/O 操作
- Overlay 渲染使用延迟计算，仅在需要时绘制
- 调试日志默认关闭，避免影响性能

### 数据安全
- 玩家数据按 UUID 隔离，避免混淆
- 使用 DataOutputStream 进行二进制序列化，减小文件大小
- 存档级数据优先于全局数据，支持多世界独立配置

### 已知限制
- 当前仅支持玩家快捷栏和背包槽位的收藏
- 容器（箱子、熔炉等）槽位收藏功能待实现
- 多人游戏时每个服务器需要单独配置（通过服务器标识区分）

### 未来规划
- [ ] 支持容器槽位收藏
- [ ] 添加收藏夹分组功能
- [ ] 实现配置热重载（/reload 命令支持）
- [ ] 添加统计信息（收藏物品数量、使用频率等）
- [ ] 支持模组物品的特殊规则配置
- [ ] 添加 JEI/REI 集成，显示物品收藏状态
