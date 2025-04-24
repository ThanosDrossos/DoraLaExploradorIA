package com.unam.dora

import com.unam.dora.ChatRepository
import com.unam.dora.GeminiApiService
import com.unam.dora.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideChatRepository(
        messageDao: MessageDao,
        geminiApiService: GeminiApiService
    ): ChatRepository {
        return ChatRepository(messageDao, geminiApiService)
    }
}