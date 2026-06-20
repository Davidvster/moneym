package com.dv.moneym.data.walletsync.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_WALLET_SYNC_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE WalletSuggestion ADD COLUMN suggested_account_id INTEGER")
        connection.execSQL("ALTER TABLE WalletSuggestion ADD COLUMN suggested_category_id INTEGER")
    }
}