# Work Items

# 阶段任务

This folder is for temporary implementation plans that are larger than one conversation but smaller than a permanent project document.

本目录用于保存跨对话但尚不适合成为永久项目文档的临时实施计划。

Create a new file when:

需要新建文件的情况：

- The goal is concrete but the implementation spans multiple sessions.
- 目标明确，但实施需要多个会话。
- The task has several validation passes, especially across Fabric, Forge, and NeoForge.
- 任务需要多轮验证，尤其是跨 Fabric、Forge、NeoForge。
- The task would make `roadmap.md` too noisy if every sub-step were added there.
- 若把每个子步骤都写进 `roadmap.md`，会让路线图变得嘈杂。

Use this name format:

使用以下命名格式：

```text
YYYY-MM-DD-short-topic.md
```

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
