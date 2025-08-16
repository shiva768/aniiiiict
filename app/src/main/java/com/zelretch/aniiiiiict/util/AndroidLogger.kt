package com.zelretch.aniiiiiict.util

import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Timberライブラリを使用したAndroidログ実装
 * 
 * Timber（木材）ライブラリを使用してログ出力を行います。
 * デバッグビルド時のみログが出力され、リリースビルドでは自動的に抑制されます。
 * タイムスタンプとコンテキスト情報を含むメッセージフォーマットを提供します。
 */
@Singleton
class AndroidLogger @Inject constructor() : Logger {
    // 日付フォーマット設定（年-月-日 時:分:秒.ミリ秒）
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * ログメッセージをタイムスタンプとコンテキスト付きでフォーマット
     * @param message ログメッセージ
     * @param context コンテキスト情報（メソッド名など）
     * @return フォーマット済みメッセージ
     */
    private fun formatMessage(message: String, context: String): String {
        val timestamp = dateFormat.format(Date())
        return "[$timestamp][$context] $message"
    }

    /**
     * エラーログ出力（例外情報付き）
     * @param tag ログタグ（クラス名など）
     * @param error 例外オブジェクト
     * @param context コンテキスト情報
     */
    override fun error(tag: String, error: Throwable, context: String) {
        Timber.e(error, formatMessage(error.message ?: "Unknown error", context))
    }

    /**
     * エラーログ出力（メッセージのみ）
     * @param tag ログタグ（クラス名など）
     * @param errorMessage エラーメッセージ
     * @param context コンテキスト情報
     */
    override fun error(tag: String, errorMessage: String, context: String) {
        Timber.e(formatMessage(errorMessage, context))
    }

    /**
     * 警告ログ出力
     * @param tag ログタグ（クラス名など）
     * @param message ログメッセージ
     * @param context コンテキスト情報
     */
    override fun warning(tag: String, message: String, context: String) {
        Timber.w(formatMessage(message, context))
    }

    /**
     * 情報ログ出力
     * @param tag ログタグ（クラス名など）
     * @param message ログメッセージ
     * @param context コンテキスト情報
     */
    override fun info(tag: String, message: String, context: String) {
        Timber.i(formatMessage(message, context))
    }

    /**
     * デバッグログ出力
     * @param tag ログタグ（クラス名など）
     * @param message ログメッセージ
     * @param context コンテキスト情報
     */
    override fun debug(tag: String, message: String, context: String) {
        Timber.d(formatMessage(message, context))
    }
}
