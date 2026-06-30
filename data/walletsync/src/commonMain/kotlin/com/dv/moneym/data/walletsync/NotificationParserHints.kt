package com.dv.moneym.data.walletsync

/**
 * Multilingual hint lists for NotificationParser.
 *
 * Languages: en, de, es, it, fr, pt, lt, et, mk, sv, nb, is, lv, pl, nl, da, fi, hr, sk, cs, hu,
 *            ja, vi, tr, sl, ru, ar, hi, zh
 *
 * Entries within each list are ordered:
 *  1. English (baseline)
 *  2. Germanic  (de, nl, sv, nb, da, is)
 *  3. Romance   (es, it, fr, pt)
 *  4. Slavic    (pl, hr, sk, cs, sl, ru, mk)
 *  5. Baltic    (lt, lv, et)
 *  6. Finno-Ugric (fi, hu)
 *  7. Other     (tr, ja, vi, ar, hi, zh)
 */
internal object NotificationParserHints {

    // ─────────────────────────────────────────────────────────────────
    // CREDIT HINTS
    // ─────────────────────────────────────────────────────────────────
    val CREDIT_HINTS: List<String> = listOf(
        // English
        "refund", "refunded", "received", "credited", "credit", "cashback", "cash back",
        "deposit", "incoming", "income", "top up", "top-up",
        // German
        "erstattet", "gutschrift", "eingang", "gutgeschrieben", "rückerstattung",
        "einzahlung", "überweisung erhalten", "geldeingang",
        // Dutch
        "ontvangen", "terugbetaling", "gestort", "bijgeschreven", "tegoed",
        // Swedish
        "återbetalning", "insättning", "mottagen", "mottaget", "inkommande",
        "gottskriven", "krediterad",
        // Norwegian (Bokmål)
        "tilbakebetaling", "innskudd", "mottatt", "kreditert", "innbetaling",
        // Danish
        "tilbagebetaling", "indbetaling", "modtaget", "krediteret",
        // Icelandic
        "endurgreiðsla", "innborgun", "móttekið", "kreditfærsla",
        // Spanish
        "reembolso", "reembolsado", "recibido", "recibida", "abono", "ingreso",
        "depósito",
        // Italian
        "rimborso", "rimborsato", "ricevuto", "ricevuta", "accredito", "accreditato",
        "deposito", "bonifico ricevuto",
        // French
        "remboursement", "remboursé", "reçu", "reçue", "virement reçu", "virement reçu de",
        "dépôt", "versement reçu",
        // Portuguese
        "reembolso", "reembolsado", "recebido", "recebida", "recebeu",
        "depósito", "transferência recebida", "pix recebido",
        // Polish
        "zwrot", "przelew przychodzący", "uznanie", "wpłata", "otrzymano",
        "zaksięgowano", "wpłynęło",
        // Croatian
        "povrat", "uplata", "primljeno", "odobrenje", "kredit",
        // Slovak
        "vrátenie", "dobropis", "prijatá platba", "vklad", "príjem",
        // Czech
        "vrácení", "dobropis", "přijatá platba", "vklad", "příchozí platba",
        // Slovenian
        "vračilo", "polog", "prejeto", "nakazilo prejeto", "prihodek",
        // Russian
        "возврат", "зачисление", "получено", "пополнение", "приход",
        "кредит", "входящий перевод",
        // Macedonian
        "поврат", "уплата", "примено", "кредит", "влог",
        // Lithuanian
        "grąžinimas", "įskaitymas", "gauta", "gautas", "ateinanti suma",
        "papildymas", "kreditavimas",
        // Latvian
        "atmaksa", "saņemts", "saņemta", "ieskaitīts", "ienākošs",
        "papildināšana", "kredīts",
        // Estonian
        "tagasimakse", "laekunud", "krediteeritud", "sissemakse", "saadud",
        // Finnish
        "palautus", "hyvitys", "vastaanotettu", "saapunut", "talletus",
        "tuleva maksu", "kreditoitu",
        // Hungarian
        "visszatérítés", "jóváírás", "befizetés", "beérkezett", "jóváírva",
        // Turkish
        "iade", "alındı", "alınan", "para yükleme", "hesaba geçti",
        "kredi", "para girişi",
        // Japanese
        "入金", "振込入金", "返金", "チャージ", "受取",
        // Vietnamese
        "hoàn tiền", "nhận tiền", "tiền vào", "chuyển khoản đến", "nạp tiền",
        // Arabic
        "استرداد", "مبلغ محول إليك", "إيداع", "رصيد وارد", "تحويل وارد",
        "حُوِّل إليك",
        // Hindi
        "वापसी", "जमा", "प्राप्त", "क्रेडिट", "भुगतान प्राप्त",
        // Chinese
        "退款", "收款", "入账", "存款", "到账", "转入",
    )

    // ─────────────────────────────────────────────────────────────────
    // DEBIT HINTS
    // ─────────────────────────────────────────────────────────────────
    val DEBIT_HINTS: List<String> = listOf(
        // English
        "paid", "payment", "purchase", "purchase of", "spent", "charge", "charged",
        "debit", "debited", "withdrawal", "transaction",
        // German
        "gezahlt", "bezahlt", "zahlung", "kauf", "abbuchung", "belastung",
        "ausgabe", "abhebung", "transaktion",
        // Dutch
        "betaald", "betaling", "aankoop", "afgeschreven", "kosten", "afboeking",
        "uitgave",
        // Swedish
        "betalat", "betalning", "köp", "debiterat", "uttag", "kostnad",
        "transaktion",
        // Norwegian (Bokmål)
        "betalt", "betaling", "kjøp", "debitert", "uttak", "kostnad",
        "transaksjon",
        // Danish
        "betalt", "betaling", "køb", "debiteret", "hævning", "udgift",
        "transaktion",
        // Icelandic
        "greitt", "greiðsla", "kaup", "debitfærsla", "úttekt",
        // Spanish
        "pagado", "pago", "compra", "cobro", "cargo", "débito", "gasto",
        // Italian
        "pagato", "pagata", "pagamento", "acquisto", "addebito", "prelievo",
        "spesa",
        // French
        "payé", "payée", "paiement", "achat", "débit", "débité", "prélèvement",
        "retrait",
        // Portuguese
        "pago", "paga", "pagamento", "compra", "débito", "debitado",
        "compra aprovada", "aprovada", "aprovado",
        // Polish
        "zapłacono", "płatność", "zakup", "obciążenie", "transakcja",
        "wypłata",
        // Croatian
        "plaćeno", "plaćanje", "kupnja", "kupovina", "terećenje", "transakcija",
        // Slovak
        "zaplatené", "platba", "nákup", "odpis", "transakcia",
        // Czech
        "zaplaceno", "platba", "nákup", "odpis", "transakce",
        // Slovenian
        "plačano", "plačilo", "nakup", "obremenitev", "transakcija",
        // Russian
        "оплачено", "оплата", "покупка", "списание", "расход",
        "транзакция", "снятие",
        // Macedonian
        "платено", "плаќање", "купување", "задолжување", "трансакција",
        // Lithuanian
        "apmokėta", "mokėjimas", "pirkimas", "nurašymas", "išlaidos",
        "operacija",
        // Latvian
        "samaksāts", "maksājums", "pirkums", "norakstīts", "izdevumi",
        "transakcija",
        // Estonian
        "makstud", "makse", "ost", "debiteeritud", "kulu", "tehing",
        // Finnish
        "maksettu", "maksu", "ostos", "veloitettu", "nosto", "kulu",
        "tapahtuma",
        // Hungarian
        "fizetve", "fizetés", "vásárlás", "terhelés", "tranzakció", "kiadás",
        // Turkish
        "ödendi", "ödeme", "alışveriş", "borçlandırıldı", "işlem",
        "para çıkışı",
        // Japanese
        "支払い", "決済", "引き落とし", "ご利用", "出金", "購入",
        // Vietnamese
        "thanh toán", "mua hàng", "trừ tiền", "giao dịch", "tiền ra",
        "chi tiêu",
        // Arabic
        "تم الدفع", "دفع", "شراء", "خصم", "معاملة", "سحب",
        "عملية شراء",
        // Hindi
        "भुगतान", "खरीदारी", "डेबिट", "लेनदेन", "निकासी",
        // Chinese
        "付款", "消费", "扣款", "支出", "购买", "转出",
    )

    // ─────────────────────────────────────────────────────────────────
    // AMOUNT HINTS
    // ─────────────────────────────────────────────────────────────────
    val AMOUNT_HINTS: List<String> = listOf(
        // English
        "paid", "payment", "purchase", "spent", "refund", "received", "credited",
        "amount", "total", "charged",
        // German
        "gezahlt", "bezahlt", "zahlung", "kauf", "betrag", "summe", "gutschrift",
        // Dutch
        "betaald", "betaling", "aankoop", "bedrag", "totaal",
        // Swedish
        "betalat", "betalning", "köp", "belopp", "summa",
        // Norwegian
        "betalt", "betaling", "kjøp", "beløp", "sum",
        // Danish
        "betalt", "betaling", "køb", "beløb",
        // Icelandic
        "greitt", "greiðsla", "upphæð",
        // Spanish
        "pagado", "pago", "compra", "importe", "total", "monto",
        // Italian
        "pagato", "pagamento", "acquisto", "importo", "totale",
        // French
        "payé", "paiement", "achat", "montant", "total",
        // Portuguese
        "pago", "pagamento", "compra", "valor", "total", "recebeu",
        // Polish
        "zapłacono", "płatność", "zakup", "kwota", "suma",
        // Croatian
        "plaćeno", "plaćanje", "kupnja", "iznos", "ukupno",
        // Slovak
        "zaplatené", "platba", "nákup", "suma", "čiastka",
        // Czech
        "zaplaceno", "platba", "nákup", "částka", "suma",
        // Slovenian
        "plačano", "plačilo", "nakup", "znesek", "vsota",
        // Russian
        "оплачено", "оплата", "покупка", "сумма",
        // Macedonian
        "платено", "плаќање", "сума", "износ",
        // Lithuanian
        "apmokėta", "mokėjimas", "suma", "kaina",
        // Latvian
        "samaksāts", "maksājums", "summa",
        // Estonian
        "makstud", "makse", "summa",
        // Finnish
        "maksettu", "maksu", "summa", "määrä",
        // Hungarian
        "fizetve", "fizetés", "összeg",
        // Turkish
        "ödendi", "ödeme", "tutar", "miktar",
        // Japanese
        "支払い", "金額", "合計",
        // Vietnamese
        "thanh toán", "số tiền", "tổng",
        // Arabic
        "دفع", "مبلغ", "إجمالي",
        // Hindi
        "भुगतान", "राशि", "कुल",
        // Chinese
        "付款", "金额", "合计",
    )

    // ─────────────────────────────────────────────────────────────────
    // CARD HINTS
    // ─────────────────────────────────────────────────────────────────
    val CARD_HINTS: List<String> = listOf(
        // English
        "card", "visa", "mastercard", "debit", "credit", "ending", "last", "digits",
        // German
        "karte", "kreditkarte", "debitkarte", "endet", "letzte",
        // Dutch
        "kaart", "eindigt", "laatste",
        // Swedish
        "kort", "slutar", "sista",
        // Norwegian
        "kort", "slutter", "siste",
        // Danish
        "kort", "slutter", "sidste",
        // Icelandic
        "kort", "lykill",
        // Spanish
        "tarjeta", "tarjeta de débito", "tarjeta de crédito", "terminada",
        "terminado", "últimos",
        // Italian
        "carta", "carta di credito", "carta di debito", "termina", "ultime",
        // French
        "carte", "carte bancaire", "se terminant", "derniers",
        // Portuguese
        "cartão", "cartao", "final", "com final", "últimos",
        // Polish
        "karta", "kartą", "kończy się", "ostatnie",
        // Croatian
        "kartica", "završava", "posljednje",
        // Slovak
        "karta", "kartou", "končí", "posledné",
        // Czech
        "karta", "kartou", "končí", "poslední",
        // Slovenian
        "kartica", "konča", "zadnje",
        // Russian
        "карта", "карточка", "заканчивается", "последние",
        // Macedonian
        "картичка", "завршува", "последни",
        // Lithuanian
        "kortelė", "kortelės", "baigiasi",
        // Latvian
        "karte", "karte beidzas", "pēdējie",
        // Estonian
        "kaart", "lõppeb", "viimased",
        // Finnish
        "kortti", "päättyy", "viimeiset",
        // Hungarian
        "kártya", "végződő", "utolsó",
        // Turkish
        "kart", "biten", "son",
        // Japanese
        "カード", "末尾",
        // Vietnamese
        "thẻ", "số cuối",
        // Arabic
        "بطاقة", "تنتهي بـ", "آخر أرقام",
        // Hindi
        "कार्ड", "अंतिम अंक",
        // Chinese
        "卡", "尾号",
    )

    // ─────────────────────────────────────────────────────────────────
    // CARD TAIL MARKERS
    // ─────────────────────────────────────────────────────────────────
    val CARD_TAIL_MARKERS: List<String> = listOf(
        // Universal masked-card patterns
        "**", "****", "xxxx", "••••",
        // English
        " card ", " with the card", " with card",
        " visa ", " mastercard ", " debit ", " credit ",
        " ending ", " last ", " digits ",
        // German
        " karte ", " endet auf ", " kreditkarte ", " debitkarte ",
        // Dutch
        " kaart ", " eindigt op ",
        // Swedish
        " kort ", " slutar på ",
        // Norwegian
        " kort ", " slutter på ",
        // Danish
        " kort ", " slutter på ",
        // Icelandic
        " kort ",
        // Spanish
        " tarjeta ", " terminada en ", " con tarjeta ",
        // Italian
        " carta ", " che termina con ", " con carta ",
        // French
        " carte ", " se terminant par ",
        // Portuguese
        " cartão ", " cartao ", " com final ", " para o cartão", " para o cartao",
        " cartão com", " cartao com",
        // Polish
        " kartą ", " karta ", " kończy się ",
        // Croatian
        " kartica ", " završava ",
        // Slovak
        " kartou ", " karta ", " končí ",
        // Czech
        " kartou ", " karta ", " končí ",
        // Slovenian
        " kartica ", " konča se ",
        // Russian
        " карта ", " карточка ", " заканчивается на ",
        // Macedonian
        " картичка ", " завршува на ",
        // Lithuanian
        " kortele ", " kortelė ", " baigiasi ",
        // Latvian
        " karte ", " beidzas ar ",
        // Estonian
        " kaart ", " lõppeb ",
        // Finnish
        " kortilla ", " kortti ", " päättyy ",
        // Hungarian
        " kártyán ", " kártya ", " végződő ",
        // Turkish
        " kart ", " ile biten ",
        // Japanese
        "カード", "末尾",
        // Vietnamese
        " thẻ ", " số cuối ",
        // Arabic
        " بطاقة ", " تنتهي بـ",
        // Hindi
        " कार्ड ",
        // Chinese
        " 卡 ", "尾号",
    )

    // ─────────────────────────────────────────────────────────────────
    // TRAILING MERCHANT NOISE
    // ─────────────────────────────────────────────────────────────────
    val TRAILING_MERCHANT_NOISE: List<String> = listOf(
        // English
        "received", "refunded", "refund", "credited",
        "with the card", "with card", "with the", "with",
        // German
        "gezahlt", "bezahlt", "erhalten", "erstattet",
        // Dutch
        "betaald", "ontvangen", "terugbetaald",
        // Swedish
        "betalt", "mottagen", "mottaget",
        // Norwegian
        "betalt", "mottatt",
        // Danish
        "betalt", "modtaget",
        // Icelandic
        "greitt", "móttekið",
        // Spanish
        "pagado", "pagada", "recibido", "recibida",
        // Italian
        "pagato", "pagata", "ricevuto", "ricevuta",
        // French
        "payé", "payée", "reçu", "reçue",
        // Portuguese
        "aprovada", "aprovado", "recebida", "recebido",
        // Polish
        "zapłacono", "otrzymano",
        // Croatian
        "plaćeno", "primljeno",
        // Slovak
        "zaplatené", "prijaté",
        // Czech
        "zaplaceno", "přijato",
        // Slovenian
        "plačano", "prejeto",
        // Russian
        "оплачено", "получено",
        // Macedonian
        "платено", "примено",
        // Lithuanian
        "apmokėta", "gauta",
        // Latvian
        "samaksāts", "saņemts",
        // Estonian
        "makstud", "laekunud",
        // Finnish
        "maksettu", "vastaanotettu",
        // Hungarian
        "fizetve", "beérkezett",
        // Turkish
        "ödendi", "alındı",
        // Vietnamese
        "thành công",
        // Arabic
        "بنجاح",
        // Hindi
        "सफल",
        // Chinese
        "成功",
    )

    // ─────────────────────────────────────────────────────────────────
    // MERCHANT STOP MARKERS
    // Phrases that usually start account/source metadata after the merchant.
    // ─────────────────────────────────────────────────────────────────
    val MERCHANT_STOP_MARKERS: List<String> = listOf(
        " from ",
        " paid automatically",
        " with auto accept",
        " automatically",
        " via ",
        " using ",
        " met ",
        " van ",
        " automatisch",
    )

    // ─────────────────────────────────────────────────────────────────
    // NON-TRANSACTION FILTER HINTS
    // Used in combinations so real payment confirmations are not filtered out.
    // ─────────────────────────────────────────────────────────────────
    val MARKET_HINTS: List<String> = listOf(
        "stock", "stocks", "share", "shares", "market", "markets", "ticker",
        "nasdaq", "nyse", "dow", "s&p", "ftse", "dax", "cac", "ibex",
        "is down", "is up", "down", "up", "fell", "rose", "gain", "loss",
        "price alert", "departure", "anthropic", "analyst", "earnings",
        "crypto", "bitcoin", "ethereum",
    )

    val NEWS_HINTS: List<String> = listOf(
        "news", "scientist", "departure", "leaves", "joins", "announces",
        "report", "reports", "because", "after", "amid",
    )

    val CHALLENGE_REWARD_HINTS: List<String> = listOf(
        "challenge", "new challenge", "reward", "rewards", "points", "point",
        "cash prize", "bonus", "badge", "streak", "mission", "quest",
    )

    val DEADLINE_TERMS_HINTS: List<String> = listOf(
        "t&c", "t&cs", "terms", "conditions", "apply", "by ", "before ",
        "until ", "deadline", "expires", "expire", "enrol", "enroll",
        "join by", "tap to", "offer ends",
    )

    val PROMOTIONAL_HINTS: List<String> = listOf(
        "trial", "free trial", "offer", "benefit", "benefits", "deal", "deals",
        "discount", "cashback offer", "promotion", "promo", "upgrade",
        "metal", "premium", "switch to", "licensed platform", "on us",
    )

    val WORTH_BENEFITS_POINTS_HINTS: List<String> = listOf(
        "worth", "benefit", "benefits", "points", "point", "reward", "rewards",
        "value", "save up to",
    )

    val PAYMENT_CONFIRMATION_HINTS: List<String> = listOf(
        "paid", "payment", "purchase", "spent", "charged", "debited",
        "received", "credited", "refund", "transfer", "direct debit",
        "compra", "pagamento", "recebeu", "transferência",
    )

    // ─────────────────────────────────────────────────────────────────
    // DATE HINTS
    // ─────────────────────────────────────────────────────────────────
    val DATE_HINTS: List<String> = listOf(
        // English
        "january", "february", "march", "april", "may", "june", "july",
        "august", "september", "october", "november", "december",
        "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
        // German
        "januar", "februar", "märz", "mai", "juni", "juli",
        "august", "september", "oktober", "november", "dezember",
        "jan", "feb", "mär", "jun", "jul", "aug", "sep", "okt",
        // Dutch
        "januari", "februari", "maart", "april", "mei", "juni", "juli",
        "augustus", "september", "oktober", "november", "december",
        // Swedish
        "januari", "februari", "mars", "april", "maj", "juni", "juli",
        "augusti", "september", "oktober", "november", "december",
        // Norwegian (Bokmål)
        "januar", "februar", "mars", "april", "mai",
        "august", "desember",
        // Danish
        "januar", "februar", "marts",
        // Icelandic
        "janúar", "febrúar", "mars", "apríl", "maí", "júní", "júlí",
        "ágúst", "september", "október", "nóvember", "desember",
        // Spanish
        "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
        "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        // Italian
        "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno", "luglio",
        "agosto", "settembre", "ottobre", "novembre", "dicembre",
        // French
        "janvier", "février", "mars", "avril", "mai", "juin", "juillet",
        "août", "septembre", "octobre", "novembre", "décembre",
        // Portuguese
        "janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho",
        "agosto", "setembro", "outubro", "novembro", "dezembro",
        // Polish
        "styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec", "lipiec",
        "sierpień", "wrzesień", "październik", "listopad", "grudzień",
        "sty", "lut", "mar", "kwi", "cze", "lip", "sie", "wrz", "paź", "lis", "gru",
        // Croatian
        "siječanj", "veljača", "ožujak", "travanj", "svibanj", "lipanj", "srpanj",
        "kolovoz", "rujan", "listopad", "studeni", "prosinac",
        // Slovak
        "január", "február", "marec", "apríl", "máj", "jún", "júl",
        "august", "september", "október", "november", "december",
        // Czech
        "leden", "únor", "březen", "duben", "květen", "červen", "červenec",
        "srpen", "září", "říjen", "listopad", "prosinec",
        // Slovenian
        "januar", "februar", "marec", "april", "maj", "junij", "julij",
        "avgust", "september", "oktober", "november", "december",
        // Russian
        "январь", "февраль", "март", "апрель", "май", "июнь", "июль",
        "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
        "янв", "фев", "мар", "апр", "июн", "июл", "авг", "сен", "окт", "ноя", "дек",
        // Macedonian
        "јануари", "февруари", "март", "април", "мај", "јуни", "јули",
        "август", "септември", "октомври", "ноември", "декември",
        // Lithuanian
        "sausis", "vasaris", "kovas", "balandis", "gegužė", "birželis", "liepa",
        "rugpjūtis", "rugsėjis", "spalis", "lapkritis", "gruodis",
        "sau", "vas", "kov", "bal", "geg", "bir", "lie", "rug", "rgp", "spa", "lap", "gru",
        // Latvian
        "janvāris", "februāris", "marts", "aprīlis", "maijs", "jūnijs", "jūlijs",
        "augusts", "septembris", "oktobris", "novembris", "decembris",
        // Estonian
        "jaanuar", "veebruar", "märts", "aprill", "mai", "juuni", "juuli",
        "august", "september", "oktoober", "november", "detsember",
        // Finnish
        "tammikuu", "helmikuu", "maaliskuu", "huhtikuu", "toukokuu", "kesäkuu",
        "heinäkuu", "elokuu", "syyskuu", "lokakuu", "marraskuu", "joulukuu",
        "tammi", "helmi", "maalis", "huhti", "touko", "kesä",
        "heinä", "elo", "syys", "loka", "marras", "joulu",
        // Hungarian
        "január", "február", "március", "április", "május", "június", "július",
        "augusztus", "szeptember", "október", "november", "december",
        // Turkish
        "ocak", "şubat", "mart", "nisan", "mayıs", "haziran", "temmuz",
        "ağustos", "eylül", "ekim", "kasım", "aralık",
        "oca", "şub", "haz", "tem", "ağu", "eyl", "kas", "ara",
        // Japanese
        "月", "年",
        // Vietnamese
        "tháng", "năm",
        // Arabic
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو",
        "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
        // Hindi
        "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई",
        "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर",
        // Chinese
        "月", "年",
    )

    // ─────────────────────────────────────────────────────────────────
    // LEADING CONNECTORS
    // Prepositions stripped from the START of an extracted merchant string
    // ─────────────────────────────────────────────────────────────────
    val LEADING_CONNECTORS: List<String> = listOf(
        // English
        "to", "at", "from",
        // German
        "an", "bei", "von",
        // Dutch
        "aan", "bij", "van",
        // French
        "chez", "de",
        // Portuguese
        "em", "para",
        // Spanish
        "en", "a", "desde",
        // Italian
        "a", "da", "presso",
        // Swedish
        "till", "hos", "från",
        // Norwegian (Bokmål)
        "til", "hos", "fra",
        // Danish
        "til", "hos", "fra",
        // Icelandic
        "til", "hjá", "frá",
        // Polish
        "do", "w", "od", "u",
        // Croatian
        "do", "u", "od", "kod",
        // Slovak
        "do", "v", "od", "u",
        // Czech
        "do", "v", "od", "u",
        // Slovenian
        "do", "v", "od", "pri",
        // Russian
        "в", "у", "от", "для",
        // Macedonian
        "во", "кај", "од",
        // Lithuanian
        "į", "pas", "iš",
        // Latvian
        "uz", "pie", "no",
        // Estonian
        "poodi", "juurde", "käest",
        // Finnish
        "kaupassa", "myymälässä",
        // Hungarian
        "nál", "nél", "ban", "ben", "tól", "től",
        // Turkish
        "için", "mağazasında", "kurumunda",
    )

    // ─────────────────────────────────────────────────────────────────
    // MERCHANT PATTERNS
    // Regex patterns used to extract the merchant name from notification text.
    // Each pattern captures everything after a leading preposition/connector.
    // Order matters: more specific / higher-priority patterns come first.
    // ─────────────────────────────────────────────────────────────────
    val MERCHANT_PATTERNS: List<Regex> = listOf(
        // English
        Regex("\\b(?:to|at|from)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // German
        Regex("\\b(?:an|bei|von)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Dutch
        Regex("\\b(?:aan|bij|van)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Portuguese — before chez|de to avoid shadowing
        Regex("\\b(?:em|para)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // French / Spanish / Italian / Portuguese (de / chez)
        Regex("\\b(?:chez|de)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Spanish (en / a)
        Regex("\\b(?:en|\\ba)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Italian (presso / da)
        Regex("\\b(?:presso|da)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Swedish / Norwegian / Danish / Icelandic (hos / till / til)
        Regex("\\b(?:hos|till|til)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Russian
        Regex("\\b(?:в|у)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Polish / Czech / Slovak / Slovenian / Croatian
        Regex("\\b(?:do|w|u|v|kod|pri)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Lithuanian
        Regex("\\b(?:pas|į)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Latvian
        Regex("\\b(?:pie|uz)\\s+(.+)$", RegexOption.IGNORE_CASE),
        // Turkish
        Regex("\\b(?:için|kurumunda|mağazasında)\\s+(.+)$", RegexOption.IGNORE_CASE),
    )
}
