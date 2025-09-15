package com.zelretch.aniiiiict.data.auth

import android.content.SharedPreferences
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import timber.log.Timber

class トークンマネージャーテスト : BehaviorSpec({
    lateinit var prefs: SharedPreferences
    lateinit var tokenManager: TokenManager

    // テスト中はTimberのログを抑制
    beforeSpec {
        if (Timber.forest().isEmpty()) {
            Timber.plant(object : Timber.DebugTree() {})
        }
    }

    beforeTest {
        prefs = InMemorySharedPreferences()
        tokenManager = TokenManager(prefs)
    }

    given("TokenManagerの基本動作") {
        `when`("まだトークンが保存されていない") {
            then("getAccessTokenはnullを返し、hasValidTokenはfalseになる") {
                tokenManager.getAccessToken().shouldBeNull()
                tokenManager.hasValidToken().shouldBeFalse()
            }
        }

        `when`("空白ではないトークンを保存する") {
            then("保存したトークンを取得でき、hasValidTokenはtrueになる") {
                tokenManager.saveAccessToken("abc1234567890")
                tokenManager.getAccessToken() shouldBe "abc1234567890"
                tokenManager.hasValidToken().shouldBeTrue()
            }
        }

        `when`("空文字や空白のみのトークンを保存しようとする") {
            then("保存は無視され、直前の有効な値が保持される") {
                // 先に有効なトークンを保存
                tokenManager.saveAccessToken("valid_token")
                // 空文字・空白のみを保存しようとする
                tokenManager.saveAccessToken("")
                tokenManager.saveAccessToken("   ")

                tokenManager.getAccessToken() shouldBe "valid_token"
                tokenManager.hasValidToken().shouldBeTrue()
            }
        }
    }
})

// シンプルなインメモリ実装（テスト用）
private class InMemorySharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (map[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor(map)
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    private class Editor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clearAll = false
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            if (key != null) temp[key] = value
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
            if (key != null) temp[key] = values
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            if (key != null) temp[key] = value
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            if (key != null) temp[key] = value
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            if (key != null) temp[key] = value
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            if (key != null) temp[key] = value
        }
        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) temp[key] = null
        }
        override fun clear(): SharedPreferences.Editor = apply {
            clearAll = true
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clearAll) {
                map.clear()
            }
            for ((k, v) in temp) {
                if (v == null) map.remove(k) else map[k] = v
            }
            temp.clear()
            clearAll = false
        }
    }
}
