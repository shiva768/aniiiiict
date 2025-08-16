package com.zelretch.aniiiiiict

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Aniiiiictアプリケーションクラス
 * 
 * アプリケーション全体の初期化処理を行います。
 * HiltとTimberの設定を含みます。
 */
@HiltAndroidApp
class AniiiiictApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Timberログライブラリの初期化
        // デバッグビルド時のみログ出力を有効化し、リリース時は自動的に無効化
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
