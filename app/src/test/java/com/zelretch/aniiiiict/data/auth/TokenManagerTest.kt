package com.zelretch.aniiiiict.data.auth

import android.content.SharedPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import timber.log.Timber

@DisplayName("TokenManager")
class TokenManagerTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var tokenManager: TokenManager

    @BeforeEach
    fun setup() {
        // テスト中はTimberのログを抑制
        if (Timber.forest().isEmpty()) {
            Timber.plant(object : Timber.DebugTree() {})
        }

        prefs = InMemorySharedPreferences()
        tokenManager = TokenManager(prefs)
    }

    @Nested
    @DisplayName("トークン保存前")
    inner class BeforeSaveToken {

        @Test
        @DisplayName("getAccessTokenはnullを返す")
        fun getAccessTokenはnullを返す() {
            // Then
            assertNull(tokenManager.getAccessToken())
        }

        @Test
        @DisplayName("hasValidTokenはfalseを返す")
        fun hasValidTokenはfalseを返す() {
            // Then
            assertFalse(tokenManager.hasValidToken())
        }
    }

    @Nested
    @DisplayName("トークン保存")
    inner class SaveToken {

        @Test
        @DisplayName("有効なトークンを保存して取得できる")
        fun 有効なトークンを保存して取得できる() {
            // When
            tokenManager.saveAccessToken("abc1234567890")

            // Then
            assertEquals("abc1234567890", tokenManager.getAccessToken())
            assertTrue(tokenManager.hasValidToken())
        }

        @Test
        @DisplayName("空文字や空白のみのトークンは保存されない")
        fun 空文字や空白のみのトークンは保存されない() {
            // Given: 先に有効なトークンを保存
            tokenManager.saveAccessToken("valid_token")

            // When: 空文字・空白のみを保存しようとする
            tokenManager.saveAccessToken("")
            tokenManager.saveAccessToken("   ")

            // Then: 直前の有効な値が保持される
            assertEquals("valid_token", tokenManager.getAccessToken())
            assertTrue(tokenManager.hasValidToken())
        }
    }
}

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
