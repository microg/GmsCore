---
name: create-prompt
description: 'Create a concise, repo-aware task prompt for code work in this repository.'
argument-hint: What code task should the prompt describe?
disable-model-invocation: true
---

Use this skill when you want to generate a focused implementation prompt for the current repository.

Steps:
1. Identify the desired outcome: feature, bug fix, refactor, or test update.
2. Specify relevant files, packages, or modules in the repository.
3. Preserve existing style, conventions, and build/test expectations.
4. Request a final summary of touched files and the result.

Quality checks:
- Prompt is specific and actionable.
- Scope is limited to known repository areas.
- The request mentions tests if behavior changes.
- The output is concise and implementation-focused.

Example prompt:
"Please inspect the current repository and implement/fix the following task:
- [Describe the feature, bug, or refactor clearly]
- Include any relevant file paths or modules
- Preserve existing style and conventions
- Add or update tests if needed
- Summarize the final changes and touched files"
