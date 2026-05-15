# Neo Favorite Items Documentation

# Neo Favorite Items 文档索引

Last updated: 2026-05-15

最后更新：2026-05-15

## Document Map

## 文档地图

- [Architecture](architecture.md): stable architecture, module boundaries, layer responsibilities, and runtime flows.
- [Architecture](architecture.md)：稳定架构、模块边界、分层职责和核心运行流程。
- [Roadmap](roadmap.md): prioritized backlog, known limitations, and long-term maintenance notes.
- [Roadmap](roadmap.md)：优先级待办、已知限制和长期维护事项。
- [Test Report](reports/test-report.md): automated test scope, coverage snapshot, and build verification for the current delivery.
- [Test Report](reports/test-report.md)：当前交付的自动化测试范围、覆盖率快照和构建验证。
- [Verification Report](reports/verification-report.md): fix validation notes and manual integration validation history.
- [Verification Report](reports/verification-report.md)：修复验证记录和手工集成验证历史。
- [Work Items](work-items/README.md): temporary stage plans for work that is too large for one conversation.
- [Work Items](work-items/README.md)：跨对话阶段任务和临时实施计划。
- [Archive](archive/): logs or historical notes kept for reference, not active project guidance.
- [Archive](archive/)：仅供追溯的日志或历史记录，不作为当前项目指导。

## Naming Rules

## 命名规范

- Use lowercase kebab-case file names for project documents, for example `architecture.md` and `test-report.md`.
- 项目文档文件名使用小写短横线，例如 `architecture.md`、`test-report.md`。
- Keep root-level documents limited to `README.md` and `LICENSE`.
- 根目录只保留 `README.md` 和 `LICENSE`。
- Put stable design and process documents directly under `docs/`.
- 稳定设计和流程文档放在 `docs/` 下。
- Put verification and test snapshots under `docs/reports/`.
- 验证和测试快照放在 `docs/reports/` 下。
- Put temporary or stage-specific plans under `docs/work-items/`.
- 临时或阶段性计划放在 `docs/work-items/` 下。
- Put inactive logs, raw notes, and obsolete material under `docs/archive/`.
- 不再活跃的日志、原始记录和过期材料放在 `docs/archive/` 下。

## Work-Item Rule

## 阶段任务规则

Create a work-item document when a task has a concrete goal but cannot reasonably be finished in one conversation, or when it needs validation across multiple loaders/mod combinations.

当某个任务目标明确但单次对话难以完整完成，或需要跨多个加载器和模组组合验证时，创建阶段任务文档。

Recommended name:

推荐命名：

```text
docs/work-items/YYYY-MM-DD-short-topic.md
```

Each work-item should contain:

每个阶段任务文档应包含：

- Goal / 目标
- Scope / 范围
- Checklist / 检查清单
- Decisions / 已定决策
- Validation / 验证记录
- Handoff Notes / 交接说明

When the work is done, merge lasting decisions into `architecture.md`, move remaining backlog into `roadmap.md`, summarize validation in `reports/`, then move the work-item to `archive/` or delete it if it has no historical value.

任务完成后，把长期有效决策并入 `architecture.md`，把剩余待办并入 `roadmap.md`，把验证摘要写入 `reports/`，随后将阶段任务文档移入 `archive/`，若没有追溯价值则删除。
