# NeoFavoriteItems TODO

最后更新：2026-04-20

## 阶段 0：本地构建入口

状态：完成

- 已添加当前目录 `gradle.bat`，固定使用 `D:\Tools\Gradle\gradle-8.14.4`。
- 已固定 Gradle 下载/缓存目录为 `D:\Tools\Gradle\GRADLE_Local_Repository`。
- 直接运行 `.\gradle.bat` 时，默认提交 `common/fabric/forge/neoforge` 四端 `compileJava`，由 Gradle 自动调度执行。
- 传入参数时保持透传，例如 `.\gradle.bat :fabric:compileJava`。

## 阶段 1：架构设计与 common 核心

状态：完成

- 已新增 `ARCHITECTURE_DESIGN.md`，记录新模组分层、平台边界、槽位映射和服务端权威策略。
- 已引入 `LogicalSlotIndex`、`SlotMappingService`、`InteractionGuardService` 等 common 层基础模型。
- 已把 `FavoritesManager` 从裸 `int` 存储迁移到逻辑槽位模型，同时保留兼容入口。
- 验证：`:common:compileJava` 已通过。

## 阶段 2：Fabric 1.21.1 基础集成

状态：完成

- 已完成 Fabric 入口和 Overlay 渲染的 1.21.1 Mojmap/API 迁移。
- Fabric 平台细节通过 helper/反射隔离，避免 common 层依赖 Minecraft client 类型。
- 验证：`:fabric:compileJava` 已通过。

## 阶段 3：Forge/NeoForge 1.21.1 基础集成

状态：完成

- 已为 Forge/NeoForge 子项目添加 `loom.platform`，让 Architectury Loom 正确注册 loader 配置。
- 已完成 Forge/NeoForge 入口、客户端 tick、Overlay 渲染和 1.21.1 Mojmap/API 迁移。
- 平台私有字段访问集中在平台层 helper 中，common 层不承担映射兼容成本。
- 验证：`:forge:compileJava` 与 `:neoforge:compileJava` 已通过。

## 阶段 4：交互拦截与服务端权威

状态：Fabric 主路径完成，跨端验证进行中

- 设计目标：平台层只负责捕获点击、拖拽、快捷移动、丢弃、热键等交互，决策统一交给 common 的 `InteractionGuardService`。
- Fabric 已接入 `AbstractContainerScreen#slotClicked`、`CreativeModeInventoryScreen#slotClicked` 与 GUI 鼠标前置拦截，普通容器和创造模式玩家背包槽位均走 common 决策。
- Fabric 创造模式已通过 `CreativeModeInventoryScreen$SlotWrapper.target` 解包，统一使用 Mojmap 原生 `Slot#getContainerSlot()` 映射到真实玩家背包槽位。
- 下一步：复用同一决策模型到 Forge/NeoForge，继续补齐服务端权威验证点。
- 需要覆盖的风险点：旁路键移动追踪、多人服务端验证、Forge/NeoForge 与 Fabric 的行为一致性。

## 阶段 5：资源、体验与测试

状态：进行中

- Overlay 已改为“明度/形状 PNG + 配置色程序化着色”方案；颜色配置支持旧版 `0xAARRGGBB` 与 `"luv(L, u, v, alpha)"` 双格式。
- Common 已集中提供颜色通道/透明度/ARGB helper，Fabric 渲染层已复用；旧 `overlayColor/overlayOpacity` 只作为读取旧配置时的兼容键，不再写入新配置。
- 已接入 `border/classic/framework/highlight/brackets/lock/mark/tag/star` 九种明度材质，配置文件可选择对应 `OverlayStyle`。
- Overlay PNG 已改为按资源实际宽高采样并缩放到 16x16 槽位，兼容更高分辨率的单图材质；已锁、可收藏提示、可解锁提示可分别配置是否渲染在物品图标前方。
- 锁定操作键和旁路键已注册为游戏内按键绑定，默认分别为 Left Alt 和 Left Ctrl，不再写入模组配置文件。
- Fabric 已改为通过当前按键绑定值 + GLFW 状态轮询锁定操作键/旁路键，避免 GUI 内重绑按键失效和打开 GUI 前已按住按键丢失的问题。
- 按住锁定操作键时，GUI 中带有物品的玩家背包槽显示 `highlight`；已收藏槽位显示可解锁 highlight，未收藏槽位显示可收藏 highlight，二者颜色和透明度可分别配置。
- 已为退出 GUI 后的快捷栏 HUD 添加已锁槽位 overlay 渲染，和 GUI 使用同一套 locked overlay；按住旁路键时 GUI 内已锁 overlay 会按配置淡化。
- Fabric GUI 与 HUD overlay 已提高渲染 z 层，显示在物品图标前方，避免被物品遮挡。
- 配置项已补充中英双语注释；`autoUnlockEmptySlots=true` 时，`allowItemsIntoLockedEmptySlots` 不参与决策。
- 增加最小单元测试，优先覆盖槽位映射、序列化兼容和交互决策。
- 最终验证目标：三平台 `compileJava` 通过，并尽量执行可用的资源处理/打包任务。
