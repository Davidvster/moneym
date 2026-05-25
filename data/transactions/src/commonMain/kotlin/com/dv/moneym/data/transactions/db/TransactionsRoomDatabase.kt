package com.dv.moneym.data.transactions.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TransactionsRoomDatabaseConstructor : RoomDatabaseConstructor<TransactionsRoomDatabase>

@Database(
    entities = [TransactionEntity::class, PaymentModeEntity::class, RecurringTransactionEntity::class],
    version = 2,
)
@ConstructedBy(TransactionsRoomDatabaseConstructor::class)
abstract class TransactionsRoomDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentModeDao(): PaymentModeDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE TransactionEntry ADD COLUMN recurring_id INTEGER")
                connection.execSQL("CREATE INDEX IF NOT EXISTS idx_te_recurring ON TransactionEntry(recurring_id)")
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS RecurringTransactionEntry (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        amount_minor INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        note TEXT,
                        category_id INTEGER NOT NULL,
                        account_id INTEGER NOT NULL,
                        payment_mode_id INTEGER,
                        start_date TEXT NOT NULL,
                        freq_unit TEXT NOT NULL,
                        freq_interval INTEGER NOT NULL,
                        day_of_week INTEGER,
                        day_of_month INTEGER,
                        use_last_day INTEGER NOT NULL DEFAULT 0,
                        end_kind TEXT NOT NULL,
                        end_count INTEGER,
                        end_date TEXT,
                        last_materialized TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS idx_rte_category ON RecurringTransactionEntry(category_id)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS idx_rte_account ON RecurringTransactionEntry(account_id)")
            }
        }
    }
}
