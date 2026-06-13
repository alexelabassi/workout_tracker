# CLAUDE_CODE_WORKFLOW.md

# Fast Claude Code Workflow

## Core Rule

Claude Code is not the architect.

Claude Code implements exact tasks from the docs.

You are the tech lead.

---

## Every Session Starts With

```text
Read CLAUDE.md, AGENTS.md, docs/PROJECT_SPEC.md, docs/ARCHITECTURE.md, docs/DATABASE_SCHEMA.md, and docs/TASKS.md.

Do not edit yet. Summarize the project and the next task.
```

---

## For Big Changes

Use plan mode first.

Prompt:

```text
Plan only. Do not edit files.

Task: [task]

Give:
1. files you will change
2. exact steps
3. risks
4. tests/build commands
```

---

## For Implementation

Prompt:

```text
Implement only [feature].

Rules:
- Follow CLAUDE.md and docs.
- Do not modify unrelated files.
- Do not change schema unless explicitly required.
- Add tests.
- Run tests/build.
- Report changed files and commands run.
```

---

## After Every Working Slice

Run:

```bash
git status
git add .
git commit -m "Implement [feature]"
```

---

## Never Ask Claude

Bad:

```text
Build the whole app.
Make it better.
Add cool features.
Refactor everything.
```

Good:

```text
Implement workout session creation from template_day, including snapshot copying into session_exercises, session_exercise_muscle_groups, and session_routines.
```

---

## Debugging Prompt

```text
The build/test failed. Read the error carefully. Fix only the root cause. Do not refactor unrelated code.

Error:
[paste error]
```

---

## Review Prompt

```text
Review the implementation for:
- ownership/security bugs
- missing validation
- wrong cascade/snapshot behavior
- broken transaction boundaries
- missing tests

Do not edit yet. Report issues first.
```
