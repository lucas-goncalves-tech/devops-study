---
name: remember
description: Save something to this repo's persistent memory so future sessions recall it.
argument-hint: "what to remember"
disable-model-invocation: true
---

# /remember

Persist `$ARGUMENTS` in `.agents/memory/`. Read `.agents/skills/memory-system/SKILL.md` first and
follow its taxonomy, format and "never save" list.

1. Classify: `user` | `feedback` | `project` | `reference`.
2. Append to the matching topic file, or create it with `type` / `created` / `updated` frontmatter.
3. Add a one-line pointer (~150 chars) to `.agents/memory/MEMORY.md`.
4. Confirm: `Saved to memory: [summary] → .agents/memory/[file].md`.

Distil, don't copy. Nothing already written in the repo. No secrets. Index stays under 200 lines;
never delete an entry without asking.
