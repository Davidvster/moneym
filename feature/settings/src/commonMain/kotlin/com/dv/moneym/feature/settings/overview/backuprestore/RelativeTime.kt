package com.dv.moneym.feature.settings.overview.backuprestore

internal object RelativeTime {
    fun format(deltaMs: Long): RelativeTimeBucket = when {
        deltaMs < 0L -> RelativeTimeBucket.JustNow
        deltaMs < 60_000L -> RelativeTimeBucket.JustNow
        deltaMs < 3_600_000L -> RelativeTimeBucket.Minutes((deltaMs / 60_000L).toInt())
        deltaMs < 86_400_000L -> RelativeTimeBucket.Hours((deltaMs / 3_600_000L).toInt())
        deltaMs < 30L * 86_400_000L -> RelativeTimeBucket.Days((deltaMs / 86_400_000L).toInt())
        else -> RelativeTimeBucket.LongerAgo
    }
}

internal sealed interface RelativeTimeBucket {
    data object JustNow : RelativeTimeBucket
    data class Minutes(val n: Int) : RelativeTimeBucket
    data class Hours(val n: Int) : RelativeTimeBucket
    data class Days(val n: Int) : RelativeTimeBucket
    data object LongerAgo : RelativeTimeBucket
}
