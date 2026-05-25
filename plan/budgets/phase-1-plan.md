# Phase 1 — Data layer

**Goal:** Persist budgets. No UI yet. Verified by unit tests on the repository and the active-in-month rule.

## Files to create

### `:data:budgets` module

- `data/budgets/build.gradle.kts` — clone of `data/categories/build.gradle.kts`; namespace `com.dv.moneym.data.budgets`; framework `baseName = "DataBudgets"`.
- `data/budgets/src/commonMain/kotlin/com/dv/moneym/data/budgets/db/BudgetEntity.kt`
  ```kotlin
  @Entity(tableName = "Budget")
  data class BudgetEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val name: String,
      @ColumnInfo(name = "amount_minor") val amountMinor: Long,
      val currency: String,
      @ColumnInfo(name = "category_id") val categoryId: Long?,
      @ColumnInfo(name = "period_type") val periodType: String,        // "MONTHLY"
      @ColumnInfo(name = "start_year_month") val startYearMonth: String, // "2026-05"
      @ColumnInfo(name = "recurring_months") val recurringMonths: Int?,
      @ColumnInfo(name = "created_at") val createdAt: Long,
      @ColumnInfo(name = "updated_at") val updatedAt: Long,
  )
  ```
- `db/BudgetDao.kt`
  - `@Query("SELECT * FROM Budget") fun selectAll(): Flow<List<BudgetEntity>>`
  - `@Query("SELECT * FROM Budget WHERE id = :id") suspend fun selectById(id: Long): BudgetEntity?`
  - `@Insert suspend fun insert(entity: BudgetEntity): Long`
  - `@Update suspend fun update(entity: BudgetEntity)`
  - `@Query("DELETE FROM Budget WHERE id = :id") suspend fun deleteById(id: Long)`
  - No SQL aggregation; combination with transactions happens in use cases.
- `db/BudgetsRoomDatabase.kt`
  ```kotlin
  @Suppress("NO_ACTUAL_FOR_EXPECT")
  expect object BudgetsRoomDatabaseConstructor : RoomDatabaseConstructor<BudgetsRoomDatabase>

  @Database(entities = [BudgetEntity::class], version = 1)
  @ConstructedBy(BudgetsRoomDatabaseConstructor::class)
  abstract class BudgetsRoomDatabase : RoomDatabase() {
      abstract fun budgetDao(): BudgetDao
  }
  ```
- `BudgetRepository.kt` (interface, public)
  ```kotlin
  interface BudgetRepository {
      fun observeAll(): Flow<List<Budget>>
      suspend fun getById(id: BudgetId): Budget?
      suspend fun insert(budget: Budget): BudgetId
      suspend fun update(budget: Budget)
      suspend fun delete(id: BudgetId)
  }
  ```
- `internal/BudgetLocalDataSource.kt` + `internal/RoomBudgetDataSource.kt` + `internal/BudgetMappers.kt` + `internal/BudgetRepositoryImpl.kt` — mirror `data/categories/internal/` exactly.
- `BudgetRepositoryFactory.kt`
  ```kotlin
  fun createBudgetRepository(db: BudgetsRoomDatabase): BudgetRepository =
      BudgetRepositoryImpl(RoomBudgetDataSource(db))
  ```
- `androidMain/.../BudgetsDatabaseFactory.kt` — `Room.databaseBuilder<BudgetsRoomDatabase>(context, "moneym_budgets.db")...`
- `iosMain/.../BudgetsDatabaseFactory.kt` — NSFileManager → NSApplicationSupportDirectory, same as `CategoriesDatabaseFactory.ios.kt`.
- `data/budgets/schemas/com.dv.moneym.data.budgets.db.BudgetsRoomDatabase/1.json` — generated on first KSP run; commit it.

### Core model

- `core/model/src/commonMain/kotlin/com/dv/moneym/core/model/Budget.kt`
  ```kotlin
  @Serializable
  data class Budget(
      val id: BudgetId,
      val name: String,
      val amount: Money,
      val categoryId: CategoryId?,
      val periodType: BudgetPeriodType,
      val startYearMonth: YearMonth,
      val recurringMonths: Int?,
      @Serializable(with = InstantSerializer::class) val createdAt: Instant,
      @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
  ) {
      fun isActiveIn(ym: YearMonth): Boolean { /* see README rule */ }
  }

  enum class BudgetPeriodType { MONTHLY }
  ```
- `core/model/.../Ids.kt` — add `@Serializable data class BudgetId(val value: Long)`.

### Testing fake

- `core/testing/src/commonMain/kotlin/com/dv/moneym/core/testing/FakeBudgetRepository.kt` — MutableStateFlow-backed, auto-increment id, mirrors `FakeCategoryRepository`.

## Files to modify

- `settings.gradle.kts` — add `include(":data:budgets")`.
- `composeApp/src/commonMain/kotlin/com/dv/moneym/di/DataModules.kt`
  ```kotlin
  val dataBudgetsModule = module {
      single<BudgetRepository> { createBudgetRepository(get<BudgetsRoomDatabase>()) }
  }
  ```
- `composeApp/androidMain/.../PlatformDataModule.kt` + `iosMain/.../PlatformDataModule.kt` — add `single { createBudgetsDatabase(get()) }` next to existing DB factories.
- Wherever modules are listed for Koin startup (search `dataCategoriesModule` references) — append `dataBudgetsModule`.

## Tests

- `data/budgets/src/commonTest/kotlin/.../BudgetRepositoryTest.kt` — insert/observe/delete round-trip via Room in-memory DB.
- `core/model/src/commonTest/kotlin/.../BudgetActiveInMonthTest.kt` — table-driven: single matches only start month, unlimited matches all `ym >= start`, N=3 matches months [start, start+3), `ym < start` is never active.

## Verify

```bash
./gradlew :data:budgets:assembleDebug :data:budgets:testDebugUnitTest
./gradlew :core:model:testDebugUnitTest
./gradlew :composeApp:assembleDebug
```

## Done when

- All commands above green.
- `BudgetEntity` schema v1 json committed.
- `FakeBudgetRepository` matches the interface (parity rule — otherwise downstream `compileDebugUnitTestKotlinAndroid` tasks start failing).
