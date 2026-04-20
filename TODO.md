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

状态：进行中

- 设计目标：平台层只负责捕获点击、拖拽、快捷移动、丢弃、热键等交互，决策统一交给 common 的 `InteractionGuardService`。
- Fabric 已接入 `AbstractContainerScreen#slotClicked` 薄 mixin，并通过 `:fabric:compileJava`。
- 下一步：复用同一决策模型到 Forge/NeoForge，继续补齐服务端权威验证点。
- 需要覆盖的风险点：创造模式、旁路键、非玩家容器槽位、多人服务端验证和客户端提示一致性。

## 阶段 5：资源、体验与测试

状态：待开始

- 补齐 `lock.png`、`star.png`、`checkmark.png` 或改为更稳定的程序化 Overlay。
- 增加最小单元测试，优先覆盖槽位映射、序列化兼容和交互决策。
- 最终验证目标：三平台 `compileJava` 通过，并尽量执行可用的资源处理/打包任务。
