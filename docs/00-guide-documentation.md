# Neo Favorite Items Documentation Guide

# Neo Favorite Items 文档目录与规范

Last updated: 2026-09-06

最后更新：2026-09-06

## Document Catalog

## 文档目录

- [00-guide-documentation.md](00-guide-documentation.md): documentation catalog, naming rules, and maintenance rules.
- [00-guide-documentation.md](00-guide-documentation.md)：文档目录、命名规范和维护规则。
- [10-design-architecture.md](10-design-architecture.md): stable architecture, module boundaries, layer responsibilities, and runtime flows.
- [10-design-architecture.md](10-design-architecture.md)：稳定架构、模块边界、分层职责和核心运行流程。
- [20-plan-roadmap.md](20-plan-roadmap.md): prioritized backlog, known limitations, and long-term maintenance notes.
- [20-plan-roadmap.md](20-plan-roadmap.md)：优先级待办、已知限制和长期维护事项。
- [30-report-tests.md](30-report-tests.md): automated test scope, coverage snapshot, and build verification for the current delivery.
- [30-report-tests.md](30-report-tests.md)：当前交付的自动化测试范围、覆盖率快照和构建验证。
- [31-report-verification.md](31-report-verification.md): fix validation notes and manual integration validation history.
- [31-report-verification.md](31-report-verification.md)：修复验证记录和手工集成验证历史。
- [32-report-implementation-summary.md](32-report-implementation-summary.md): completed implementation scope and historical project status.
- [32-report-implementation-summary.md](32-report-implementation-summary.md)：已完成实现范围和历史项目状态。
- [40-process-work-items.md](40-process-work-items.md): rules and template for temporary stage plans.
- [40-process-work-items.md](40-process-work-items.md)：临时阶段计划的规则和模板。

## Naming Rules

## 命名规范

- Use numbered lowercase kebab-case file names:
- 文档文件名使用带序号的小写短横线格式：

```text
NN-category-topic.md
```

- `NN`: two-digit order number. It controls reading order and keeps related documents grouped.
- `NN`：两位数字序号，用于控制阅读顺序，并让同类文档自然聚在一起。
- `category`: document type, such as `guide`, `design`, `plan`, `report`, `process`, or `workitem`.
- `category`：文档类型，例如 `guide`、`design`、`plan`、`report`、`process`、`workitem`。
- `topic`: short concrete subject.
- `topic`：简短明确的主题。
- Keep root-level documents limited to `README.md` and `LICENSE`.
- 根目录只保留 `README.md` 和 `LICENSE`。
- Keep stable project documents flat under `docs/`. Temporary cross-conversation work items are the exception and go under `docs/work-items/`.
- 稳定项目文档平铺在 `docs/` 下。跨对话临时阶段任务是例外，统一放入 `docs/work-items/`。
- Prefer sequence ranges:
- 推荐序号范围：
  - `00-09`: documentation guides and indexes
  - `00-09`：文档指南和目录
  - `10-19`: design and architecture
  - `10-19`：设计和架构
  - `20-29`: roadmap and planning
  - `20-29`：路线图和计划
  - `30-39`: reports and verification snapshots
  - `30-39`：报告和验证快照
  - `40-49`: process and workflow rules
  - `40-49`：流程和工作规则
  - `50-79`: reserved for temporary stage work items if they must be promoted into the main `docs/` set
  - `50-79`：保留给必须提升到主 `docs/` 集合中的临时阶段任务
  - `80-99`: archive or migration notes when they must stay in the repo
  - `80-99`：必须保留在仓库内的归档或迁移记录

## Responsibility Boundaries

## 职责边界

- `10-design-architecture.md`: stable structure, boundaries, runtime flows, and architectural constraints. Do not use it as a changelog or test report.
- `10-design-architecture.md`：稳定结构、边界、核心流程和架构约束。不要把它当作变更日志或测试报告。
- `20-plan-roadmap.md`: active backlog, priorities, known limitations, and intake buffer. Do not store completed implementation history here.
- `20-plan-roadmap.md`：当前待办、优先级、已知限制和需求缓冲区。不要在这里存放已完成实现历史。
- `30-report-tests.md`: automated tests, coverage, compile/build evidence, and automated validation gaps.
- `30-report-tests.md`：自动化测试、覆盖率、编译/构建证据，以及自动化验证缺口。
- `31-report-verification.md`: manual runtime validation, integration expectations, remaining manual checks, and delivery-facing validation notes.
- `31-report-verification.md`：手工运行验证、集成预期、仍需手工检查项，以及面向交付的验证说明。
- `32-report-implementation-summary.md`: completed implementation scope and historical project status.
- `32-report-implementation-summary.md`：已完成实现范围和历史项目状态。
- `40-process-work-items.md`: how to create and close temporary cross-conversation work items.
- `40-process-work-items.md`：如何创建和关闭跨对话临时阶段任务。

## Work-Item Rule

## 阶段任务规则

Create a work-item document when a task has a concrete goal but cannot reasonably be finished in one conversation, or when it needs validation across multiple loaders/mod combinations.

当某个任务目标明确但单次对话难以完整完成，或需要跨多个加载器和模组组合验证时，创建阶段任务文档。

Recommended location and name:

推荐位置与命名：

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

When the work is done, merge lasting decisions into `10-design-architecture.md`, move remaining backlog into `20-plan-roadmap.md`, summarize validation in `30-report-tests.md` or `31-report-verification.md`, then either move the work item into an archive document if it has historical value or delete it if it does not.

任务完成后，把长期有效决策并入 `10-design-architecture.md`，把剩余待办并入 `20-plan-roadmap.md`，把验证摘要写入 `30-report-tests.md` 或 `31-report-verification.md`，随后若有追溯价值则把阶段任务内容并入归档文档，若没有追溯价值则删除。
