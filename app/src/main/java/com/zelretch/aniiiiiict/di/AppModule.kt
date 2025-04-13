package com.zelretch.aniiiiiict.di

import android.content.Context
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.api.ApolloClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.datastore.WorkImagePreferences
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.data.repository.AnnictRepositoryImpl
import com.zelretch.aniiiiiict.data.util.ImageDownloader
import com.zelretch.aniiiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiiict.util.RetryManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.zelretch.aniiiiiict.data.api.ApolloClient as AniiiiiictApolloClient

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
        tokenManager: TokenManager,
        okHttpClient: OkHttpClient,
        retryManager: RetryManager
    ): AnnictAuthManager = AnnictAuthManager(tokenManager, okHttpClient, retryManager)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            } else {
                okhttp3.logging.HttpLoggingInterceptor.Level.NONE
            }
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideWorkImagePreferences(@ApplicationContext context: Context): WorkImagePreferences {
        return WorkImagePreferences(context)
    }

    @Provides
    @Singleton
    fun provideImageDownloader(@ApplicationContext context: Context): ImageDownloader {
        return ImageDownloader(context)
    }

    @Provides
    @Singleton
    fun provideApolloClient(
        tokenManager: TokenManager,
        okHttpClient: OkHttpClient
    ): AniiiiiictApolloClient {
        return AniiiiiictApolloClient(tokenManager, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideAnnictRepository(
        tokenManager: TokenManager,
        authManager: AnnictAuthManager,
        workImagePreferences: WorkImagePreferences,
        imageDownloader: ImageDownloader,
        apolloClient: ApolloClient
    ): AnnictRepository {
        return AnnictRepositoryImpl(
            tokenManager = tokenManager,
            authManager = authManager,
            workImagePreferences = workImagePreferences,
            imageDownloader = imageDownloader,
            apolloClient = apolloClient
        )
    }

    @Provides
    @Singleton
    fun provideRetryManager(): RetryManager = RetryManager()

    @Provides
    @Singleton
    fun provideProgramFilter(): ProgramFilter = ProgramFilter()
}
