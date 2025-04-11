package com.zelretch.aniiiiiict.di

import android.content.Context
import androidx.room.Room
import com.zelretch.aniiiiiict.data.api.AnnictApiClient
import com.zelretch.aniiiiiict.data.api.AnnictApiService
import com.zelretch.aniiiiiict.data.api.AnnictConfig
import com.zelretch.aniiiiiict.data.api.ApolloClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.local.AppDatabase
import com.zelretch.aniiiiiict.data.local.dao.WorkImageDao
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.data.repository.AnnictRepositoryImpl
import com.zelretch.aniiiiiict.data.util.ImageDownloader
import com.zelretch.aniiiiiict.util.RetryManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideAnnictAuthManager(
        tokenManager: TokenManager
    ): AnnictAuthManager = AnnictAuthManager(tokenManager)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideAnnictApiService(okHttpClient: OkHttpClient): AnnictApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(AnnictConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        return retrofit.create(AnnictApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnnictApiClient(tokenManager: TokenManager): AnnictApiClient {
        return AnnictApiClient(tokenManager)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aniiiiiict.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideWorkImageDao(database: AppDatabase): WorkImageDao {
        return database.workImageDao()
    }

    @Provides
    @Singleton
    fun provideImageDownloader(@ApplicationContext context: Context): ImageDownloader {
        return ImageDownloader(context)
    }

    @Provides
    @Singleton
    fun provideApolloClient(tokenManager: TokenManager): ApolloClient {
        return ApolloClient(tokenManager)
    }

    @Provides
    @Singleton
    fun provideAnnictRepository(
        apiClient: AnnictApiClient,
        tokenManager: TokenManager,
        authManager: AnnictAuthManager,
        workImageDao: WorkImageDao,
        imageDownloader: ImageDownloader,
        apolloClient: ApolloClient
    ): AnnictRepository {
        return AnnictRepositoryImpl(
            apiClient,
            tokenManager,
            authManager,
            workImageDao,
            imageDownloader,
            apolloClient
        )
    }

    @Provides
    @Singleton
    fun provideRetryManager(): RetryManager = RetryManager()
}
