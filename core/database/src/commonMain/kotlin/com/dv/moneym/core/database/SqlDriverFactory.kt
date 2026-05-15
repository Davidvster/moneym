package com.dv.moneym.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

interface SqlDriverFactory {
    fun create(schema: SqlSchema<QueryResult.Value<Unit>>, name: String): SqlDriver
}
