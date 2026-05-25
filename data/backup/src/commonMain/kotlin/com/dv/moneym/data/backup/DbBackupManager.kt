package com.dv.moneym.data.backup

import com.dv.moneym.platform.DbPlatform

private val DB_NAMES = listOf("moneym_categories.db", "moneym_accounts.db", "moneym_transactions.db", "moneym_budgets.db")
private val DB_SUFFIXES = listOf("", "-wal", "-shm")

class DbBackupManager(
    private val dbPlatform: DbPlatform,
    private val closeAllDatabases: () -> Unit = {},
) {

    suspend fun export(): ByteArray {
        val files = mutableListOf<Pair<String, ByteArray>>()
        DB_NAMES.forEach { name ->
            DB_SUFFIXES.forEach { suffix ->
                val fullName = "$name$suffix"
                val path = "${dbPlatform.dbDirectory}/$fullName"
                val bytes = dbPlatform.readBytes(path) ?: return@forEach
                if (bytes.isNotEmpty()) {
                    files.add(fullName to bytes)
                }
            }
        }
        return createZip(files)
    }

    suspend fun restore(zip: ByteArray) {
        val files = extractZip(zip)
        check(files.isNotEmpty()) { "Invalid or empty backup archive" }

        // Close all Room connections so no writes interfere during file replacement
        closeAllDatabases()

        // Delete existing WAL/SHM files — stale pages from Room's session would
        // corrupt the restored .db if left behind
        DB_NAMES.forEach { name ->
            listOf("-wal", "-shm").forEach { suffix ->
                dbPlatform.deleteFile("${dbPlatform.dbDirectory}/$name$suffix")
            }
        }

        files.forEach { (name, bytes) ->
            val path = "${dbPlatform.dbDirectory}/$name"
            val ok = dbPlatform.writeBytes(path, bytes)
            if (!ok) throw Exception("Failed to write $name")
        }
        dbPlatform.terminateApp()
    }
}
