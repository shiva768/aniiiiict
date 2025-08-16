package com.zelretch.aniiiiiict

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AniiiiictApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // デバッグビルド時のみTimberを初期化
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
