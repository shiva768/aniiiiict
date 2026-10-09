package com.zelretch.aniiiiict.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Annict への通信結果から障害を検知し、[AnnictStatusMonitor] に報告する Interceptor。
 *
 * 障害とみなすもの:
 * - 502 / 503 / 504（ゲートウェイエラー、メンテナンス、タイムアウト）
 * - 接続タイムアウト、接続拒否
 *
 * 500 は特定クエリの不具合でも返る（例: Series.works）ため障害とはみなさない。
 * UnknownHostException は端末がオフラインの可能性が高いため対象外。
 */
class AnnictHealthInterceptor(private val monitor: AnnictStatusMonitor) : Interceptor {

    private companion object {
        val ANNICT_HOSTS = setOf("api.annict.com", "annict.com")
        val OUTAGE_STATUS_CODES = setOf(502, 503, 504)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host !in ANNICT_HOSTS) return chain.proceed(request)

        val response = try {
            chain.proceed(request)
        } catch (e: SocketTimeoutException) {
            monitor.reportOutage("timeout")
            throw e
        } catch (e: ConnectException) {
            monitor.reportOutage("connect failed")
            throw e
        }

        if (response.code in OUTAGE_STATUS_CODES) {
            monitor.reportOutage("HTTP ${response.code}")
        } else {
            monitor.reportHealthy()
        }
        return response
    }
}
