# Phase 8 — Verify Build & Fix Any Remaining Issues

## Goal
Run the Android debug build and the iOS Kotlin compilation check. Fix any compilation errors introduced by previous phases.

## Commands to Run

```bash
# Build Android
./gradlew :composeApp:assembleDebug

# Check iOS compilation (Kotlin only, no Xcode needed)
./gradlew :composeApp:compileKotlinIosArm64
```

If either fails, diagnose and fix the errors in the relevant files.

## Common Issues to Watch For

1. **Missing imports** — if a `stringResource()` call was added but the import for `Res` or the specific string key is missing.
2. **Duplicate string keys** — if a key was added to strings.xml that already existed.
3. **Broken aliases** — if the `MoneyMSpacing` or `MoneyMRadius` legacy aliases don't compile due to circular references.
4. **`MM.space` in non-composable context** — if any replacement was placed outside a `@Composable` function.
5. **Unresolved references** — if a new sub-composable is called but not defined (e.g., from Phase 5 or 6 extraction).

## Fix Strategy

For each error:
1. Read the file reported in the error.
2. Make the minimal targeted fix.
3. Re-run the build command.
4. Repeat until clean.

## Acceptance Criteria
1. `./gradlew :composeApp:assembleDebug` exits with BUILD SUCCESSFUL.
2. `./gradlew :composeApp:compileKotlinIosArm64` exits with BUILD SUCCESSFUL (or at minimum no Kotlin compile errors).
3. Report what was fixed.
