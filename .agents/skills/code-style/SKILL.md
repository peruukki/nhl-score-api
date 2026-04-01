---
name: code-style
description: Enforces `nhl-score-api` code and documentation style (concise code, comment guidance, documentation quote style, trailing whitespace, and alphabetical ordering). Use when the user asks how to write code that matches project conventions.
---

# Code Style

## Concise code

Prefer concise code; avoid introducing variables that are referenced only once.

## Comments

Avoid unnecessary comments. Add comments only for non-obvious behavior or useful context.

## Documentation quote style

When editing documentation in markdown files (e.g. `README.md`), use fancy/curly quotes instead of straight quotes. Use plain quotes for command examples.

## Whitespace

Trim trailing whitespace.

## Alphabetical sorting

Always sort items alphabetically when possible. Apply this to:

- Dependencies in `project.clj`
- Fields in Malli schema definitions
- Imports in `:require` clauses
- Protocol methods in protocol definitions and implementations
- Record methods in record definitions
- Private variable definitions
- Lists, maps, or collections where order doesn't affect functionality
