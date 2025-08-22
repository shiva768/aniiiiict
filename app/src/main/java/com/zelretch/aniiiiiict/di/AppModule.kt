package com.zelretch.aniiiiiict.di

import android.content.Context
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.api.AniListApolloClient
import com.zelretch.aniiiiiict.data.api.AnnictApolloClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiiict.data.repository.AniListRepositoryImpl
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.data.repository.AnnictRepositoryImpl
import com.zelretch.aniiiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiiict.ui.base.DefaultCustomTabsIntentFactory
import com.zelretch.aniiiiiict.util.RetryManager
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

    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager = TokenManager(context)

    @Provides
    @Singleton
    fun provideAnnictAuthManager(
        tokenManager: TokenManager,
        okHttpClient: OkHttpClient,
        retryManager: RetryManager
    ): AnnictAuthManager = AnnictAuthManager(tokenManager, okHttpClient, retryManager)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().addInterceptor(
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    ).connectTimeout(
        TIMEOUT_SECONDS,
        TimeUnit.SECONDS
    ).readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).build()

    @Provides
    @Singleton
    fun provideAnnictApolloClient(tokenManager: TokenManager, okHttpClient: OkHttpClient): AnnictApolloClient =
        AnnictApolloClient(tokenManager, okHttpClient)

    @Provides
    @Singleton
    fun provideAnnictRepository(
        tokenManager: TokenManager,
        authManager: AnnictAuthManager,
        apolloClient: AnnictApolloClient
    ): AnnictRepository = AnnictRepositoryImpl(
        tokenManager = tokenManager,
        authManager = authManager,
        annictApolloClient = apolloClient
    )

    @Provides
    @Singleton
    fun provideAniListApolloClient(okHttpClient: OkHttpClient): AniListApolloClient = AniListApolloClient(okHttpClient)

    @Provides
    @Singleton
    fun provideAniListRepository(apolloClient: AniListApolloClient): AniListRepository = AniListRepositoryImpl(
        apolloClient = apolloClient
    )

    @Provides
    @Singleton
    fun provideRetryManager(): RetryManager = RetryManager()

    @Provides
    @Singleton
    fun provideProgramFilter(): ProgramFilter = ProgramFilter()

    @Provides
    @Singleton
    fun customTabIntentFactory(): CustomTabsIntentFactory = DefaultCustomTabsIntentFactory()
}
