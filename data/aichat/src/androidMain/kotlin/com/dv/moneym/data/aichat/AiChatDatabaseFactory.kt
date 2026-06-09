package com.dv.moneym.data.aichat

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.aichat.db.AiChatRoomDatabase
import kotlinx.coroutines.Dispatchers

fun createAiChatDatabase(context: Context): AiChatRoomDatabase =
    Room.databaseBuilder<AiChatRoomDatabase>(context = context, name = "moneym_aichat.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
