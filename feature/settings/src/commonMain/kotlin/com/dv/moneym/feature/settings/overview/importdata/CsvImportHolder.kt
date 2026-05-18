package com.dv.moneym.feature.settings.overview.importdata

enum class CsvSourceFormat { MONEYM, EASY_HOME_FINANCE }

class CsvImportHolder {
    var content: String = ""
    var format: CsvSourceFormat = CsvSourceFormat.MONEYM
}
