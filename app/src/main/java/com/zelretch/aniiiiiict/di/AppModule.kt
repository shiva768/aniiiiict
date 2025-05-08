package com.zelretch.aniiiiiict.di

import android.content.Context
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.api.ApolloClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.data.repository.AnnictRepositoryImpl
import com.zelretch.aniiiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiiict.ui.base.DefaultCustomTabsIntentFactory
import com.zelretch.aniiiiiict.util.AndroidLogger
import com.zelretch.aniiiiiict.util.Logger
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
    fun provideTokenManager(@ApplicationContext context: Context, logger: Logger): TokenManager {
        return TokenManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideAnnictAuthManager(
        tokenManager: TokenManager,
        okHttpClient: OkHttpClient,
        retryManager: RetryManager,
        logger: Logger
    ): AnnictAuthManager = AnnictAuthManager(tokenManager, okHttpClient, retryManager, logger)

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
    fun provideApolloClient(
        tokenManager: TokenManager,
        okHttpClient: OkHttpClient,
        logger: Logger
    ): AniiiiiictApolloClient {
        return AniiiiiictApolloClient(tokenManager, okHttpClient, logger)
    }

    @Provides
    @Singleton
    fun provideAnnictRepository(
        tokenManager: TokenManager,
        authManager: AnnictAuthManager,
        apolloClient: ApolloClient,
        logger: Logger
    ): AnnictRepository {
        return AnnictRepositoryImpl(
            tokenManager = tokenManager,
            authManager = authManager,
            apolloClient = apolloClient,
            logger = logger
        )
    }

    @Provides
    @Singleton
    fun provideRetryManager(logger: Logger): RetryManager = RetryManager(logger)

    @Provides
    @Singleton
    fun provideProgramFilter(): ProgramFilter = ProgramFilter()

    @Provides
    @Singleton
    fun provideLogger(): Logger = AndroidLogger()

    @Provides
    @Singleton
    fun customTabIntentFactory(): CustomTabsIntentFactory = DefaultCustomTabsIntentFactory()

}
