package com.zelretch.aniiiiict.ui.base

import com.zelretch.aniiiiict.domain.error.DomainError
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * エラーをユーザー向けメッセージに変換するマッパー
 *
 * Domain層のエラー（DomainError）やその他の例外を、
 * ユーザーに表示する分かりやすいメッセージに変換する。
 *
 * UI層に配置することで、メッセージ表現の詳細をUI層に閉じ込める。
 */
@Singleton
class ErrorMapper @Inject constructor() {

    /**
     * エラーをユーザー向けメッセージに変換する
     *
     * @param error 変換対象のエラー
     * @param context 追加のコンテキスト情報（オプション）
     * @return ユーザーに表示するメッセージ
     */
    fun toUserMessage(error: Throwable, context: String? = null): String {
        // ログ出力
        Timber.e(error, "Error occurred${context?.let { " in $it" } ?: ""}")

        return when (error) {
            // ネットワークエラー
            is DomainError.NetworkError.Timeout ->
                "接続がタイムアウトしました。ネットワーク接続を確認してください"

            is DomainError.NetworkError.NoConnection ->
                "ネットワーク接続を確認してください"

            is DomainError.NetworkError ->
                "ネットワーク接続を確認してください"

            // APIエラー
            is DomainError.ApiError.ServerError ->
                "サーバーで問題が発生しています。しばらく時間をおいてからお試しください"

            is DomainError.ApiError.ClientError -> when (error.statusCode) {
                400 -> "リクエストが無効です"
                401 -> "認証に失敗しました。再度ログインしてください"
                403 -> "アクセスが拒否されました"
                404 -> "データが見つかりませんでした"
                429 -> "リクエストが多すぎます。しばらく時間をおいてからお試しください"
                else -> "APIエラーが発生しました (HTTP ${error.statusCode})"
            }

            is DomainError.ApiError.GraphQLError ->
                "データの取得に失敗しました"

            is DomainError.ApiError ->
                "データの取得に失敗しました"

            // 認証エラー
            is DomainError.AuthError.AuthenticationRequired ->
                "認証が必要です。ログインしてください"

            is DomainError.AuthError.InvalidToken ->
                "認証トークンが無効です。再度ログインしてください"

            is DomainError.AuthError.TokenSaveFailed ->
                "認証情報の保存に失敗しました。アプリを再起動してください"

            is DomainError.AuthError.CallbackFailed ->
                "認証処理に失敗しました。もう一度お試しください"

            is DomainError.AuthError ->
                "認証に失敗しました。再度ログインしてください"

            // ビジネスロジックエラー
            is DomainError.BusinessError.RecordCreationFailed ->
                "エピソードの記録に失敗しました。しばらく時間をおいてからお試しください"

            is DomainError.BusinessError.RecordDeletionFailed ->
                "記録の削除に失敗しました。しばらく時間をおいてからお試しください"

            is DomainError.BusinessError.StatusUpdateFailed ->
                "ステータスの更新に失敗しました。しばらく時間をおいてからお試しください"

            is DomainError.BusinessError.AnimeDetailNotFound ->
                "アニメ詳細情報の取得に失敗しました"

            is DomainError.BusinessError.ProgramsLoadFailed ->
                "番組データの取得に失敗しました。しばらく時間をおいてからお試しください"

            is DomainError.BusinessError.RecordsLoadFailed ->
                "記録データの取得に失敗しました。しばらく時間をおいてからお試しください"

            is DomainError.BusinessError ->
                "処理に失敗しました。しばらく時間をおいてからお試しください"

            // バリデーションエラー
            is DomainError.ValidationError.MissingRequiredParameter ->
                "必要な情報が不足しています"

            is DomainError.ValidationError.InvalidInput ->
                "入力内容に問題があります"

            is DomainError.ValidationError ->
                "入力内容を確認してください"

            // 不明なエラー
            is DomainError.Unknown ->
                error.message ?: "処理中にエラーが発生しました"

            // その他の例外（Domain層で変換されていないエラー）
            else -> {
                Timber.w("Unmapped error type: ${error::class.simpleName}")
                "処理中にエラーが発生しました"
            }
        }
    }

    /**
     * 複数のエラーを統合してユーザー向けメッセージに変換する
     *
     * @param errors エラーのリスト
     * @return ユーザーに表示するメッセージ
     */
    fun toUserMessage(errors: List<Throwable>): String = when {
        errors.isEmpty() -> "エラーが発生しました"
        errors.size == 1 -> toUserMessage(errors.first())
        else -> {
            val primaryError = errors.first()
            val message = toUserMessage(primaryError)
            "複数のエラーが発生しました: $message 他${errors.size - 1}件"
        }
    }
}
