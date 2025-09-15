package com.zelretch.aniiiiict

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Hiltのテストで、テスト用のApplicationクラス（HiltTestApplication）を使用するためのカスタムテストランナー。
 */
@Suppress("unused")
class ヒルトテストランナー : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
