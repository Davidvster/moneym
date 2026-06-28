# Phase 4 Plan: Copy, Icons, AI Visibility

## Goal

Polish user-facing copy and affordances for notification import, sync, wallet suggestion notifications, and AI analysis.

## Implementation

- Change user-facing wallet sync copy toward notification import / payment notifications while keeping internal module names.
- Update changed string keys in English plus all 27 locale folders.
- Make the transaction header sync affordance clearer and keep wallet notification suggestions visually distinct with bell iconography.
- Make the overview AI action visible as a compact "Analyze with AI" button with sparkles icon.
- Avoid hardcoded user-visible literals.

## Tests

- Run resource generation or compile for affected modules.
- Run available screenshot/preview tests for overview, transactions, and wallet sync when practical.
- Commit the phase after verification.
