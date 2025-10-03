package com.zelretch.aniiiiict.domain.error

/**
 * ドメイン層のエラーを表すsealed class
 *
 * アプリケーション全体で発生する可能性のあるエラーを型安全に表現する。
 * Repository層やUseCase層で発生したエラーは、このDomainErrorに変換される。
 */
sealed class DomainError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * ネットワーク関連エラー
     */
    sealed class NetworkError(message: String? = null, cause: Throwable? = null) : DomainError(message, cause) {
        /** 接続タイムアウト */
        class Timeout(cause: Throwable? = null) : NetworkError("接続がタイムアウトしました", cause)

        /** ネットワーク接続なし */
        class NoConnection(cause: Throwable? = null) : NetworkError("ネットワーク接続がありません", cause)

        /** その他のネットワークエラー */
        class Unknown(cause: Throwable? = null) : NetworkError("ネットワークエラーが発生しました", cause)
    }

    /**
     * API関連エラー
     */
    sealed class ApiError(message: String? = null, cause: Throwable? = null) : DomainError(message, cause) {
        /** サーバーエラー（5xx） */
        class ServerError(cause: Throwable? = null) : ApiError("サーバーエラーが発生しました", cause)

        /** クライアントエラー（4xx） */
        class ClientError(val statusCode: Int, cause: Throwable? = null) :
            ApiError("リクエストエラーが発生しました (HTTP $statusCode)", cause)

        /** GraphQLエラー */
        class GraphQLError(message: String, cause: Throwable? = null) : ApiError(message, cause)

        /** その他のAPIエラー */
        class Unknown(cause: Throwable? = null) : ApiError("APIエラーが発生しました", cause)
    }

    /**
     * 認証関連エラー
     */
    sealed class AuthError(message: String? = null, cause: Throwable? = null) : DomainError(message, cause) {
        /** 認証が必要 */
        class AuthenticationRequired(cause: Throwable? = null) : AuthError("認証が必要です", cause)

        /** トークン無効 */
        class InvalidToken(cause: Throwable? = null) : AuthError("認証トークンが無効です", cause)

        /** トークン保存失敗 */
        class TokenSaveFailed(cause: Throwable? = null) : AuthError("認証情報の保存に失敗しました", cause)

        /** 認証コールバック失敗 */
        class CallbackFailed(cause: Throwable? = null) : AuthError("認証処理に失敗しました", cause)
    }

    /**
     * ビジネスロジックエラー
     */
    sealed class BusinessError(message: String? = null, cause: Throwable? = null) : DomainError(message, cause) {
        /** レコード作成失敗 */
        class RecordCreationFailed(cause: Throwable? = null) : BusinessError("エピソードの記録に失敗しました", cause)

        /** レコード削除失敗 */
        class RecordDeletionFailed(cause: Throwable? = null) : BusinessError("記録の削除に失敗しました", cause)

        /** ステータス更新失敗 */
        class StatusUpdateFailed(cause: Throwable? = null) : BusinessError("ステータスの更新に失敗しました", cause)

        /** アニメ詳細情報取得失敗 */
        class AnimeDetailNotFound(cause: Throwable? = null) : BusinessError("アニメ詳細情報の取得に失敗しました", cause)

        /** プログラムデータ取得失敗 */
        class ProgramsLoadFailed(cause: Throwable? = null) : BusinessError("番組データの取得に失敗しました", cause)

        /** レコード取得失敗 */
        class RecordsLoadFailed(cause: Throwable? = null) : BusinessError("記録データの取得に失敗しました", cause)
    }

    /**
     * バリデーションエラー
     */
    sealed class ValidationError(message: String? = null) : DomainError(message) {
        /** 必須パラメータ不足 */
        class MissingRequiredParameter(parameterName: String) :
            ValidationError("必須パラメータが不足しています: $parameterName")

        /** 無効な入力値 */
        class InvalidInput(reason: String) : ValidationError("入力値が無効です: $reason")
    }

    /**
     * 不明なエラー
     */
    class Unknown(message: String? = "不明なエラーが発生しました", cause: Throwable? = null) : DomainError(message, cause)
}
