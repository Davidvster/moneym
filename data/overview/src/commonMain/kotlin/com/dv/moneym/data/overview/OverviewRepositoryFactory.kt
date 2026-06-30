package com.dv.moneym.data.overview

import com.dv.moneym.data.overview.db.OverviewRoomDatabase
import com.dv.moneym.data.overview.internal.OverviewRepositoryImpl
import com.dv.moneym.data.overview.internal.RoomOverviewDataSource

fun createOverviewRepository(
    db: OverviewRoomDatabase,
): OverviewRepository = OverviewRepositoryImpl(
    RoomOverviewDataSource(db)
)
