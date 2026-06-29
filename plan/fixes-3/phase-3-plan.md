# Phase 3: Gemma Tools In AI Analysis

## Goal

Make local Gemma analysis execute finance tools instead of falling back to snapshots when tools mode is selected.

## Changes

- Add a prompt-level local tool protocol to `PromptBuilder`.
- Set local LLM support for tools only with execution support in place.
- In `LocalLlmAiEngine`, handle `TOOL_CALL: toolName {"param":"value"}` loops internally:
  - Run at most 3 iterations.
  - Execute matching `AiTool`.
  - Feed tool results back into the next prompt.
  - Do not emit raw tool calls to the UI.
  - Feed concise errors back for unknown, malformed, or failing calls.
- Preserve snapshot behavior for non-tool grounding and non-tool engines.

## Verification

- `./gradlew :core:ai:testDebugUnitTest :feature:aianalysis:testDebugUnitTest`

## Commit

- `Enable executable tools for local Gemma analysis`
