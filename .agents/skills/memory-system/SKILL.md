---
name: memory-system
description: Use when starting a session, when closing an issue with learnings to record, or when the user says "remember this", "save this", "don't forget", or asks what you remember.
---

# Memory System

Persistent, searchable memory that survives sessions: `.agents/memory/`. It is what lets a fresh
session know what the user already learned even when the tracker is gone.

**Memory** = what you KNOW · **Plan** = what you'll DO · **Task** = what you're DOING NOW.

## Layout

```
.agents/memory/
├── MEMORY.md     # index — one-line pointers only, max 200 lines
├── progress.md   # per-issue learnings: what worked, what blocked, what the next issue reuses
└── [topic].md    # user-preferences, project-conventions, tech-decisions, feedback-history, ...
```

## Index format

One pointer per entry, ~150 chars: `- [type] summary → topic.md`.
Types: `[user]` `[feedback]` `[project]` `[reference]`.
Topic files start with frontmatter: `type`, `created`, `updated`.

## Taxonomy

| Type | Store | Example |
|---|---|---|
| `user` | role, preferences, tools, communication style | "Senior DevOps engineer" |
| `feedback` | what the user liked or disliked in output | "wants tables, not prose" |
| `project` | conventions, decisions, per-issue learnings | "use `terraform plan`, not apply" |
| `reference` | non-sensitive infra facts, URLs, ports | "caddy on :443, VPS at …" |

## Never save

| Don't save | Why |
|---|---|
| Secrets, tokens, passwords, keys | memory is persistent and shared |
| Anything derivable from the repo | `AGENTS.md` is the source of truth — point at it, don't copy it |
| Temporary debug context, code snippets | goes stale immediately |
| Conversation transcripts, file paths that may move | distil; describe instead of quoting |

## Recall — session start

1. Read `.agents/memory/MEMORY.md`. Apply relevant entries **silently**; recite only if asked.
2. Read the topic file a relevant entry points to.

## Save

1. Classify the type. 2. Append to the matching topic file, or create it with frontmatter.
3. Add the one-line pointer to `MEMORY.md`. 4. Confirm: `Saved to memory: [summary] → [file]`.

## Issue closure — house rule

An issue is not done until its memory is written. In `progress.md`, one section per closed issue:

- **Reuses** — commands, flags and paths that worked; the next issue starts from here.
- **Blockers** — what stalled, and how it was actually resolved (not the symptom).
- **Decisions** — what was chosen and why, so it isn't relitigated.
- **Pointers** — paths, commits, docs. Reference, never paste.

Then add the index line. Distilled insight only.

## Prune

Index over 200 lines → warn and suggest merging or archiving. Never delete without asking.
