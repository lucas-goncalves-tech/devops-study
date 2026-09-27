---
name: teach-anything-consolidate
description: Use when user finishes learning a topic and wants to consolidate knowledge into a file. Triggers: "consolida", "entendi tudo, salva", "consolida num arquivo", or similar requests to save/consolidate what was taught.
---

# Consolidate Learning

## Overview

When user understood everything and wants to consolidate, create a structured reference file that emphasizes their point of greatest difficulty.

## Flow

```
User: "Entendi, consolida"
↓
Suggest difficult point based on their questions
↓
User confirms or corrects
↓
Ask which folder (organized by topic, ex: docs/docker/)
↓
Create .md file
```

## File Structure

- **Name by content:** `docker-fundamentals.md`, `vpc-setup.md`
- **Details first:** Detailed sections, then summary if needed
- **Inline notes:** `> **Nota:** ...` at points where user struggled
- **Code blocks:** Format all commands and configs

## Suggesting the Difficult Point

Look for where they:
- Asked the most questions
- Needed repetition
- Initially got it wrong

Example: "Pel suas perguntas, o ponto mais difícil foi o **layer caching**. Vou detalhar isso no arquivo."

If user disagrees, use their answer.

## Do NOT

- Create file without asking where to save
- Skip asking which point was hardest
- Put file in random location
- Summary before detailed sections
