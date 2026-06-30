# Phase 4 Plan: AI Tool Result Loop Fix

## Goal

Fix tool-grounded AI analysis so model-emitted tool calls are executed and the final user-visible answer contains actual results, not raw markup such as:

```text
<searchTransactions>{"q":"groceries","type":"expense"}</searchTransactions>
```

This phase covers user item 1 only. Commit after verification.

## Expected Files/Modules

- `core/ai/src/commonMain/kotlin/com/dv/moneym/core/ai/AppManagedToolLoop.kt`
- `core/ai/src/commonTest/kotlin/com/dv/moneym/core/ai/...` if a focused parser/loop test file is useful.
- `feature/aianalysis/src/commonMain/kotlin/com/dv/moneym/feature/aianalysis/AnalyzeViewModel.kt` only if the loop fix requires call-site changes.
- `feature/aianalysis/src/commonTest/kotlin/com/dv/moneym/feature/aianalysis/AnalyzeViewModelTest.kt`
- `plan/overview-ai/status.md`

## Implementation

- Extend the app-managed tool-call parser to support both formats:
  - Existing canonical format: `<moneym_tool_call>{"name":"searchTransactions","params":{...}}</moneym_tool_call>`.
  - Legacy/function-tag format emitted by some local models: `<searchTransactions>{"q":"groceries","type":"expense"}</searchTransactions>`.
- Treat function-tag content as the params object for the tag name.
- Only accept function-tag names that exactly match one of the available tool names in the current loop. Avoid treating arbitrary XML-like text as a tool call.
- Normalize string params defensively:
  - trim values
  - tolerate whitespace in known enum-like values such as `"ex pense"` by compacting before tool invocation if that can be done narrowly without changing free-text query values.
- Ensure the first assistant message that is just a tool call is never emitted directly to the user. The loop should invoke the tool, feed the tool result back to the model, and emit the next non-tool assistant answer.
- If the engine repeatedly returns only tool calls until the iteration limit, return a useful fallback that includes a concise tool result summary instead of the raw tool tag.
- Keep the existing canonical parser behavior compatible with existing tests.

## Tests

- Add parser/loop tests for:
  - canonical `<moneym_tool_call>` format still parses.
  - `<searchTransactions>{"q":"groceries","type":"expense"}</searchTransactions>` invokes the `searchTransactions` tool and emits the model's follow-up answer.
  - Unknown function tags are not executed and do not expose raw markup as a successful answer.
  - Repeated tool-only replies end with a fallback based on the latest tool result, not raw tags.
- Add/update an AI analysis ViewModel regression test with a fake engine:
  - user sends a tools-mode prompt
  - fake engine first emits `<searchTransactions>{"q":"team","type":"ex pense"}</searchTransactions>`
  - the DB-backed tool result is passed into the second model call
  - final chat message is the second model answer, not the raw tool tag.

## Verification

Run:

```bash
./gradlew --no-configuration-cache :core:ai:testDebugUnitTest :feature:aianalysis:testDebugUnitTest
```

Then run:

```bash
git diff --check
```

## Status Update Required

After implementation and verification, update `plan/overview-ai/status.md` with:

- Phase 4 status and verification command results.
- The commit hash after committing.

Commit only Phase 4 changes.
