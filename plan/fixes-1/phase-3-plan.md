# Phase 3: Localized Default Categories

## Context
`DefaultCategories.kt` has 13 hardcoded English category names. When a user's device is in German/Spanish/Italian, the seeded categories still appear in English. Need to localize them at seed time.

Pattern from `SeedAccountsUseCase`: it accepts `defaultName: String` passed from `DataModules.kt`. Same approach here but for 13 names. Since `SeedCategoriesUseCase.invoke()` is a suspend function, we can use a `suspend () -> List<String>` provider that calls Compose Resources' `getString()`.

## Critical Files
- `data/categories/src/commonMain/kotlin/com/dv/moneym/data/categories/DefaultCategories.kt`
- `data/categories/src/commonMain/kotlin/com/dv/moneym/data/categories/SeedCategoriesUseCase.kt`
- `composeApp/src/commonMain/kotlin/com/dv/moneym/di/DataModules.kt`
- `composeApp/src/commonMain/composeResources/values/strings.xml`
- `composeApp/src/commonMain/composeResources/values-de/strings.xml`
- `composeApp/src/commonMain/composeResources/values-es/strings.xml`
- `composeApp/src/commonMain/composeResources/values-it/strings.xml`

## Category Order (must match DefaultCategories.kt order)
1. Groceries (expense)
2. Eating out (expense)
3. Rent (expense)
4. Transport (expense)
5. Utilities (expense)
6. Health (expense)
7. Entertainment (expense)
8. Shopping (expense)
9. Other (expense)
10. Salary (income)
11. Payment (income)
12. Gift (income)
13. Other (income)

## Changes

### Change 1: String resource keys (composeApp strings.xml)
Add 13 new string keys:
```xml
<string name="category_seed_groceries">Groceries</string>
<string name="category_seed_eating_out">Eating out</string>
<string name="category_seed_rent">Rent</string>
<string name="category_seed_transport">Transport</string>
<string name="category_seed_utilities">Utilities</string>
<string name="category_seed_health">Health</string>
<string name="category_seed_entertainment">Entertainment</string>
<string name="category_seed_shopping">Shopping</string>
<string name="category_seed_other_expense">Other</string>
<string name="category_seed_salary">Salary</string>
<string name="category_seed_payment">Payment</string>
<string name="category_seed_gift">Gift</string>
<string name="category_seed_other_income">Other</string>
```

Add translations for de, es, it locales (use standard known translations):
- de: Lebensmittel, Essen gehen, Miete, Transport, Nebenkosten, Gesundheit, Unterhaltung, Einkaufen, Sonstiges, Gehalt, Zahlung, Geschenk, Sonstiges
- es: Comestibles, Comer fuera, Alquiler, Transporte, Servicios, Salud, Entretenimiento, Compras, Otros, Salario, Pago, Regalo, Otros
- it: Alimentari, Mangiare fuori, Affitto, Trasporti, Utenze, Salute, Intrattenimento, Shopping, Altro, Stipendio, Pagamento, Regalo, Altro

### Change 2: SeedCategoriesUseCase
```kotlin
// Current:
class SeedCategoriesUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke() {
        if (repository.count() == 0L) {
            defaultCategories.forEach { repository.insert(it) }
        }
    }
}

// New:
class SeedCategoriesUseCase(
    private val repository: CategoryRepository,
    private val nameProvider: suspend () -> List<String>,
) {
    suspend operator fun invoke() {
        if (repository.count() == 0L) {
            val names = nameProvider()
            defaultCategorySpecs.zip(names) { spec, name ->
                spec.toCategory(name)
            }.forEach { repository.insert(it) }
        }
    }
}
```

### Change 3: DefaultCategories.kt
Split into specs (icon/color/type) and names separately:
```kotlin
data class DefaultCategorySpec(val icon: Icon, val color: String, val type: TransactionType)

val defaultCategorySpecs: List<DefaultCategorySpec> = listOf(
    DefaultCategorySpec(Icon.Basket,   "#7E9C8C", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Utensils, "#C97B57", TransactionType.EXPENSE),
    // ... all 13
)

fun DefaultCategorySpec.toCategory(name: String): Category = Category(
    CategoryId(0), name, icon.key, color, isUserCreated = false, archived = false, epoch, epoch, type
)
```
Keep `defaultCategories` list for backward compatibility if used in tests, OR update `SeedCategoriesUseCaseTest`.

### Change 4: DataModules.kt
```kotlin
single {
    SeedCategoriesUseCase(get()) {
        listOf(
            getString(Res.string.category_seed_groceries),
            getString(Res.string.category_seed_eating_out),
            getString(Res.string.category_seed_rent),
            getString(Res.string.category_seed_transport),
            getString(Res.string.category_seed_utilities),
            getString(Res.string.category_seed_health),
            getString(Res.string.category_seed_entertainment),
            getString(Res.string.category_seed_shopping),
            getString(Res.string.category_seed_other_expense),
            getString(Res.string.category_seed_salary),
            getString(Res.string.category_seed_payment),
            getString(Res.string.category_seed_gift),
            getString(Res.string.category_seed_other_income),
        )
    }
}
```
Import: `org.jetbrains.compose.resources.getString` and the `Res` class from composeApp resources.

Note: `composeApp` module already has `composeResources` so `Res` is available.

### Change 5: Update FakeCategoryRepository if needed
Check `core/testing` fake — `SeedCategoriesUseCase` constructor changed so any test that instantiates it needs updating.

## Verification
- Fresh install in German locale → categories show German names
- Fresh install in English → categories show English names
- Existing install (categories already seeded) → no change
