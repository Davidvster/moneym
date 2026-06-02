package com.dv.moneym.data.remotebackup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LatestBackupRefTest {

    private fun ref(name: String, modifiedAtMs: Long) =
        RemoteFileRef(id = name, name = name, modifiedAtMs = modifiedAtMs, sizeBytes = 0L)

    @Test
    fun skipsSidecarFilesEvenWhenNewer() {
        val files = listOf(
            ref("moneym-sync-state.json", modifiedAtMs = 300),
            ref("moneym-devices.json", modifiedAtMs = 200),
            ref("moneym-backup-100.bin", modifiedAtMs = 100),
        )
        assertEquals("moneym-backup-100.bin", latestBackupRef(files)?.name)
    }

    @Test
    fun picksNewestBackupAmongBackups() {
        val files = listOf(
            ref("moneym-backup-1.bin", modifiedAtMs = 100),
            ref("moneym-backup-2.bin", modifiedAtMs = 300),
            ref("moneym-backup-3.zip", modifiedAtMs = 200),
        )
        assertEquals("moneym-backup-2.bin", latestBackupRef(files)?.name)
    }

    @Test
    fun returnsNullWhenOnlySidecarsPresent() {
        val files = listOf(
            ref("moneym-sync-state.json", modifiedAtMs = 300),
            ref("moneym-devices.json", modifiedAtMs = 200),
        )
        assertNull(latestBackupRef(files))
    }

    @Test
    fun returnsNullWhenEmpty() {
        assertNull(latestBackupRef(emptyList()))
    }
}
