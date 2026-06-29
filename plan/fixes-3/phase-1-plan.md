# Phase 1: Shared Language Enum

## Goal

Create one shared source of truth for supported app languages and use it from settings and AI analysis.

## Changes

- Add `SupportedLanguage` in `core/common` with `code`, `nativeName`, `englishName`, and helpers:
  - `fromCode(code: String)`
  - `fromLanguageTag(tag: String)`
  - `responseLanguageNameForTag(tag: String): String?`
- Replace the settings `supportedLanguages` list with `SupportedLanguage.entries`.
- Replace `AnalyzeViewModel`'s private language-name mapper with the shared helper.
- Keep stored language settings as string tags for compatibility.

## Verification

- `./gradlew :core:common:testDebugUnitTest :feature:settings:testDebugUnitTest :feature:aianalysis:testDebugUnitTest`

## Commit

- `Introduce shared supported language enum`
