package com.dv.moneym.data.llmmodels

import kotlin.time.TimeSource

/**
 * Smooths download speed over a sampling window so the displayed rate doesn't flicker on every
 * 64 KB chunk. Returns the most recent bytes/second estimate, or null until the first window
 * elapses.
 */
class DownloadSpeedMeter(private val windowMs: Long = 1000L) {

    private var anchorMark: TimeSource.Monotonic.ValueTimeMark? = null
    private var anchorBytes = 0L
    private var lastSpeed: Long? = null

    fun update(bytesRead: Long): Long? {
        val now = TimeSource.Monotonic.markNow()
        val start = anchorMark
        if (start == null) {
            anchorMark = now
            anchorBytes = bytesRead
            return lastSpeed
        }
        val elapsedMs = (now - start).inWholeMilliseconds
        if (elapsedMs >= windowMs) {
            val delta = bytesRead - anchorBytes
            if (delta >= 0 && elapsedMs > 0) lastSpeed = delta * 1000 / elapsedMs
            anchorMark = now
            anchorBytes = bytesRead
        }
        return lastSpeed
    }
}
