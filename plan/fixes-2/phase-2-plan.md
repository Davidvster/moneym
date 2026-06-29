# Phase 2 Plan: Bulk Transaction Mutation Support

## Goal

Add repository-level selected-transaction bulk mutation support so the UI phase can call one stable interface.

## Implementation

- Extend `TransactionRepository`, `TransactionLocalDataSource`, Room DAO, repository implementation, and `FakeTransactionRepository` with selected-id bulk methods:
  - `delete(ids: Set<TransactionId>)`
  - `updateCategory(ids: Set<TransactionId>, categoryId: CategoryId, type: TransactionType)`
  - `updateAccount(ids: Set<TransactionId>, accountId: AccountId, currency: CurrencyCode, rate: Double?)`
  - `updatePaymentMode(ids: Set<TransactionId>, paymentModeId: PaymentModeId?)`
- Category updates must update both `category_id` and transaction `type`.
- Wallet updates must update account for selected rows; when a rate is supplied, update selected amounts and currency only.
- Payment mode updates must support assigning a mode or clearing it.
- Preserve sync semantics by updating `updated_at` and using soft delete for deletion.

## Tests

- Repository implementation tests for bulk delete, category/type update, account/currency conversion, and payment mode update.
- Fake repository parity for all new methods.
- Run `./gradlew --no-configuration-cache :data:transactions:testDebugUnitTest :feature:transactions:testDebugUnitTest`.
