# Work-Item Process

# 阶段任务流程

This document defines how to create temporary implementation plans that are larger than one conversation but smaller than a permanent project document.

本文档定义如何创建跨对话但尚不适合成为永久项目文档的临时实施计划。

Create a new file when:

需要新建文件的情况：

- The goal is concrete but the implementation spans multiple sessions.
- 目标明确，但实施需要多个会话。
- The task has several validation passes, especially across Fabric, Forge, and NeoForge.
- 任务需要多轮验证，尤其是跨 Fabric、Forge、NeoForge。
- The task would make `20-plan-roadmap.md` too noisy if every sub-step were added there.
- 若把每个子步骤都写进 `20-plan-roadmap.md`，会让路线图变得嘈杂。

Use this name format:

使用以下命名格式：

```text
docs/work-items/YYYY-MM-DD-short-topic.md
```

Work-item files are intentionally kept in their own folder because they are temporary, can multiply during long stages, and should not obscure the stable documentation set in `docs/`.

阶段任务文档刻意放在独立目录中，因为它们是临时文件，长阶段中可能变多，不应干扰 `docs/` 下稳定文档集合的阅读。

Suggested template:

建议模板：

```markdown
# Short Topic

Last updated: YYYY-MM-DD

## Goal

## Scope

## Checklist

- [ ] ...

## Decisions

## Validation

## Handoff Notes
```
