# Phase 2 Plan: AI Analysis Tool Support

## Goal
Make AI Analysis "Tools" mode work even for engines that report `supportsTools = false`, by adding an app-managed tool-call loop that can execute the existing database-backed finance tools and feed results back into the final answer.

## Scope
- Modules likely involved:
  - `feature/aianalysis`
  - `core/ai`
  - any nearby test fakes in `core/testing` or feature tests
- Keep `BuildFinanceToolsetUseCase` as the source of database-backed tools. Do not replace its repository-backed query behavior.
- Do not alter unrelated engine/provider setup.

## Implementation Steps
1. Inspect the current AI Analysis flow:
   - Find `BuildFinanceToolsetUseCase`, `PromptBuilder`, `AnalyzeViewModel`, `AiEngine`, tool models, and existing unsupported-tool fallback behavior.
   - Identify the smallest app-layer service/use case boundary for executing tool iterations.
2. Add an app-managed tool loop for tools mode:
   - When tools mode is selected and the engine does not natively support tools, prompt the engine with available tool names, schemas, and a strict textual call format.
   - Parse streamed assistant output for a tool request.
   - Invoke the matching `AiTool`.
   - Append tool-result context/messages and ask the engine for the final response.
   - Cap iterations to prevent loops.
   - Unknown tools, invalid JSON, invalid params, and tool failures must produce a graceful final answer/error note rather than crashing.
3. Update prompt grounding:
   - Tools mode must include callable definitions and instructions, not only a vague "tools are available" note.
   - Keep normal/context modes unchanged except where shared abstractions require harmless signature changes.
4. Remove or narrow unsupported-tools UI/fallback:
   - Do not show "tools unsupported" for app-managed tool mode.
   - Only keep unsupported notices for paths where the app-layer loop is intentionally unavailable.
5. Tests:
   - Add parser tests for valid tool call, invalid JSON/params, unknown tool, and max-iteration handling.
   - Add `AnalyzeViewModelTest` coverage proving tools mode uses DB-backed tool results when a fake engine reports `supportsTools = false`.
   - Prefer existing fake repositories/engines and repository contracts.
6. Verification:
   - Run `./gradlew :feature:aianalysis:testDebugUnitTest :core:ai:testDebugUnitTest --console=plain`.
   - If task names differ, discover the correct module names and record the command used.

## Constraints
- Follow ViewModel conventions: one public `onIntent(...)`, state exposed as a single `StateFlow`.
- Keep user-visible strings in resources for every supported locale if any are added or changed.
- Repository/data contracts must keep fake parity.
- Do not commit; the main agent will review, update status, verify, and commit.
