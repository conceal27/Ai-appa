package com.ai.companion.di

import android.content.Context
import androidx.room.Room
import com.ai.companion.data.local.AppDatabase
import com.ai.companion.data.local.AppPreferences
import com.ai.companion.data.local.dao.ChatMessageDao
import com.ai.companion.data.local.dao.MemoryDao
import com.ai.companion.data.remote.api.DeepSeekApi
import com.ai.companion.data.repository.ChatRepositoryImpl
import com.ai.companion.domain.repository.ChatRepository
import com.ai.companion.domain.usecase.HumanizeService
import com.ai.companion.domain.usecase.MemoryService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideDeepSeekApi(client: OkHttpClient): DeepSeekApi {
        return DeepSeekApi(client)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    @Singleton
    fun provideMemoryDao(database: AppDatabase): MemoryDao {
        return database.memoryDao()
    }

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideMemoryService(memoryDao: MemoryDao): MemoryService {
        return MemoryService(memoryDao)
    }

    @Provides
    @Singleton
    fun provideHumanizeService(): HumanizeService {
        return HumanizeService()
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        chatMessageDao: ChatMessageDao,
        appPreferences: AppPreferences,
        memoryService: MemoryService,
        humanizeService: HumanizeService,
        deepSeekApi: DeepSeekApi
    ): ChatRepository {
        return ChatRepositoryImpl(
            chatMessageDao,
            appPreferences,
            memoryService,
            humanizeService,
            deepSeekApi
        )
    }
}
