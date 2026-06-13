# Phase 4 — Suggestions: single-accept confirm + filter sheet fixes (tasks 5 + 6)

## Goals
1. **Single Accept** of a suggested transaction must show a confirmation dialog (it's not easily reversible). Loading overlay during processing already exists (`isProcessing`) — keep. Single Reject keeps its undo snackbar (no dialog).
2. **Filter sheet**: the "note" field should read **"Transaction description"** (it searches description/counterparty). And the Clear/Apply buttons must NOT shrink in height when the keyboard appears.

## Kotlin files
- `feature/banksync/.../suggestions/BankSuggestionsUiState.kt`
- `feature/banksync/.../suggestions/BankSuggestionsViewModel.kt`
- `feature/banksync/.../suggestions/BankSuggestionsScreen.kt`

### UiState + Intent
- Add field to `BankSuggestionsUiState`: `val acceptConfirmId: Long? = null,`
- Add intents (replace the single-accept path):
  - `data class RequestAccept(val id: Long) : BankSuggestionsIntent`
  - `data object ConfirmAccept : BankSuggestionsIntent`
  - **Remove** the now-unused `data class Accept(val id: Long)` variant.

### ViewModel
- Remove the `is BankSuggestionsIntent.Accept -> acceptAll(listOf(intent.id))` branch.
- Add:
  - `is BankSuggestionsIntent.RequestAccept -> _state.update { it.copy(acceptConfirmId = intent.id) }`
  - `BankSuggestionsIntent.ConfirmAccept -> { val id = _state.value.acceptConfirmId; _state.update { it.copy(acceptConfirmId = null) }; if (id != null) acceptAll(listOf(id)) }`
- Update `DismissConfirm` to also clear `acceptConfirmId`:
  `_state.update { it.copy(showAcceptConfirm = false, showRejectConfirm = false, acceptConfirmId = null) }`
- `acceptAll` already sets `isProcessing` true/false — unchanged.

### Screen — `BankSuggestionsScreen.kt`
- Single Accept button (`SuggestionCard`, line ~499-505): change `onClick` to `onIntent(BankSuggestionsIntent.RequestAccept(row.id))`.
- Add a confirm dialog near the other dialogs (after the reject-confirm block ~line 321):
```kotlin
state.acceptConfirmId?.let {
    MmDialog(
        title = stringResource(Res.string.suggestions_accept_one_confirm_title),
        confirmText = stringResource(Res.string.suggestions_accept),
        onConfirm = { onIntent(BankSuggestionsIntent.ConfirmAccept) },
        onDismiss = { onIntent(BankSuggestionsIntent.DismissConfirm) },
        dismissText = stringResource(Res.string.bank_sync_cancel),
    ) {
        Text(
            text = stringResource(Res.string.suggestions_accept_one_confirm_body),
            style = MM.type.body,
            color = MM.colors.text2,
        )
    }
}
```
- Add imports for the two new generated string keys (add strings first — see below).

### FilterSheet — fix keyboard button shrink
In `FilterSheet` (`BankSuggestionsScreen.kt` ~line 341), make the content `Column` scroll and respect the IME so fixed-height buttons aren't compressed:
- Add to the Column modifier: `.verticalScroll(rememberScrollState())` and `.imePadding()`.
- Imports: `androidx.compose.foundation.rememberScrollState`, `androidx.compose.foundation.verticalScroll`, `androidx.compose.foundation.layout.imePadding`.
- The note `MmField` label keeps `Res.string.suggestions_filter_note` (only its value text changes in XML).

## String files (banksync module, all 28: values + 27 locales)

### (a) CHANGE existing `suggestions_filter_note` value → "Transaction description"
| loc | value | loc | value |
|---|---|---|---|
| values | Transaction description | nb | Transaksjonsbeskrivelse |
| ar | وصف المعاملة | nl | Transactieomschrijving |
| cs | Popis transakce | pl | Opis transakcji |
| da | Transaktionsbeskrivelse | pt | Descrição da transação |
| de | Transaktionsbeschreibung | ru | Описание транзакции |
| es | Descripción de la transacción | sk | Popis transakcie |
| et | Tehingu kirjeldus | sl | Opis transakcije |
| fi | Tapahtuman kuvaus | sv | Transaktionsbeskrivning |
| fr | Description de la transaction | tr | İşlem açıklaması |
| hi | लेन-देन विवरण | vi | Mô tả giao dịch |
| hr | Opis transakcije | zh | 交易描述 |
| hu | Tranzakció leírása | is | Lýsing færslu |
| it | Descrizione della transazione | ja | 取引の説明 |
| lt | Operacijos aprašymas | lv | Darījuma apraksts |
| mk | Опис на трансакцијата | | |

### (b) ADD `suggestions_accept_one_confirm_title` = "Accept transaction?"
| loc | value | loc | value |
|---|---|---|---|
| values | Accept transaction? | nb | Godta transaksjon? |
| ar | قبول المعاملة؟ | nl | Transactie accepteren? |
| cs | Přijmout transakci? | pl | Zaakceptować transakcję? |
| da | Acceptér transaktion? | pt | Aceitar transação? |
| de | Transaktion übernehmen? | ru | Принять транзакцию? |
| es | ¿Aceptar transacción? | sk | Prijať transakciu? |
| et | Kas kinnitada tehing? | sl | Sprejmem transakcijo? |
| fi | Hyväksytäänkö tapahtuma? | sv | Acceptera transaktion? |
| fr | Accepter la transaction ? | tr | İşlem kabul edilsin mi? |
| hi | लेन-देन स्वीकार करें? | vi | Chấp nhận giao dịch? |
| hr | Prihvatiti transakciju? | zh | 接受交易？ |
| hu | Elfogadja a tranzakciót? | is | Samþykkja færslu? |
| it | Accettare la transazione? | ja | 取引を承認しますか？ |
| lt | Priimti operaciją? | lv | Pieņemt darījumu? |
| mk | Да се прифати трансакцијата? | | |

### (c) ADD `suggestions_accept_one_confirm_body` = "This creates a transaction and can't be undone."
| loc | value |
|---|---|
| values | This creates a transaction and can't be undone. |
| ar | ينشئ هذا معاملة ولا يمكن التراجع عنه. |
| cs | Tím se vytvoří transakce a nelze ji vrátit zpět. |
| da | Dette opretter en transaktion og kan ikke fortrydes. |
| de | Dies erstellt eine Transaktion und kann nicht rückgängig gemacht werden. |
| es | Esto crea una transacción y no se puede deshacer. |
| et | See loob tehingu ja seda ei saa tagasi võtta. |
| fi | Tämä luo tapahtuman, eikä sitä voi kumota. |
| fr | Cela crée une transaction et est irréversible. |
| hi | इससे एक लेन-देन बनता है और इसे पूर्ववत नहीं किया जा सकता। |
| hr | Time se stvara transakcija i ne može se poništiti. |
| hu | Ez létrehoz egy tranzakciót, és nem vonható vissza. |
| is | Þetta býr til færslu og er ekki hægt að afturkalla. |
| it | Questo crea una transazione e non può essere annullato. |
| ja | これにより取引が作成され、元に戻せません。 |
| lt | Taip sukuriama operacija ir jos negalima atšaukti. |
| lv | Tādējādi tiek izveidots darījums, un to nevar atsaukt. |
| mk | Ова создава трансакција и не може да се врати. |
| nb | Dette oppretter en transaksjon og kan ikke angres. |
| nl | Hiermee maak je een transactie aan; dit kan niet ongedaan worden gemaakt. |
| pl | Spowoduje to utworzenie transakcji i nie można tego cofnąć. |
| pt | Isto cria uma transação e não pode ser desfeito. |
| ru | Это создаст транзакцию, и отменить её будет нельзя. |
| sk | Tým sa vytvorí transakcia a nedá sa vrátiť späť. |
| sl | S tem se ustvari transakcija in je ni mogoče razveljaviti. |
| sv | Detta skapar en transaktion och kan inte ångras. |
| tr | Bu, bir işlem oluşturur ve geri alınamaz. |
| vi | Thao tác này tạo một giao dịch và không thể hoàn tác. |
| zh | 这将创建一笔交易，且无法撤销。 |

Insert (b)/(c) near the existing `suggestions_accept_all_confirm_*` lines (~60-61) in each file. Escape XML appropriately (the `'` in `can't` does not need escaping in Android XML strings, but match how existing strings handle apostrophes in that file — the EN `suggestions_accept_all_confirm_body` uses a literal `'`).

## Verify
- `grep -rL suggestions_accept_one_confirm_title feature/banksync/src/commonMain/composeResources/*/strings.xml` → nothing
- `grep -rL suggestions_accept_one_confirm_body feature/banksync/src/commonMain/composeResources/*/strings.xml` → nothing
- `./gradlew :feature:banksync:compileDebugKotlinAndroid`
