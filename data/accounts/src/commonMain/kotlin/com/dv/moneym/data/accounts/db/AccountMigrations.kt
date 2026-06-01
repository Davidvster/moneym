package com.dv.moneym.data.accounts.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_ACCOUNTS_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE Account ADD COLUMN color_hex TEXT")
    }
}

val MIGRATION_ACCOUNTS_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE Account ADD COLUMN sync_id TEXT")
        connection.execSQL("ALTER TABLE Account ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE Account SET sync_id = lower(hex(randomblob(16))) WHERE sync_id IS NULL")
    }
}
