package com.dv.moneym.data.aichat

import com.dv.moneym.data.aichat.db.AiChatRoomDatabase
import com.dv.moneym.data.aichat.internal.AiChatRepositoryImpl

fun createAiChatRepository(
    db: AiChatRoomDatabase,
): AiChatRepository = AiChatRepositoryImpl(db)
