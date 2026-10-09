package com.zelretch.aniiiiict.data.api

import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@DisplayName("AnnictHealthInterceptor")
class AnnictHealthInterceptorTest {

    private lateinit var monitor: AnnictStatusMonitor
    private lateinit var interceptor: AnnictHealthInterceptor

    @BeforeEach
    fun setup() {
        monitor = AnnictStatusMonitor()
        interceptor = AnnictHealthInterceptor(monitor)
    }

    private fun chain(url: String, code: Int = 200): Interceptor.Chain {
        val request = Request.Builder().url(url).build()
        return mockk {
            every { request() } returns request
            every { proceed(any()) } returns Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .body("".toResponseBody())
                .build()
        }
    }

    private fun failingChain(url: String, error: Exception): Interceptor.Chain {
        val request = Request.Builder().url(url).build()
        return mockk {
            every { request() } returns request
            every { proceed(any()) } throws error
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [502, 503, 504])
    @DisplayName("Annictが502/503/504を返すと障害と判定する")
    fun outageStatusCodes(code: Int) {
        interceptor.intercept(chain("https://api.annict.com/graphql", code))

        assertTrue(monitor.isDown.value)
    }

    @Test
    @DisplayName("500はクエリ起因でも返るため障害と判定しない")
    fun internalServerErrorIsNotOutage() {
        interceptor.intercept(chain("https://api.annict.com/graphql", 500))

        assertFalse(monitor.isDown.value)
    }

    @Test
    @DisplayName("タイムアウトは障害と判定し例外はそのまま投げる")
    fun timeoutIsOutage() {
        assertThrows<SocketTimeoutException> {
            interceptor.intercept(failingChain("https://api.annict.com/graphql", SocketTimeoutException()))
        }

        assertTrue(monitor.isDown.value)
    }

    @Test
    @DisplayName("接続拒否は障害と判定する")
    fun connectFailureIsOutage() {
        assertThrows<ConnectException> {
            interceptor.intercept(failingChain("https://annict.com/oauth/token", ConnectException()))
        }

        assertTrue(monitor.isDown.value)
    }

    @Test
    @DisplayName("名前解決失敗は端末オフラインの可能性が高いため障害と判定しない")
    fun unknownHostIsNotOutage() {
        assertThrows<UnknownHostException> {
            interceptor.intercept(failingChain("https://api.annict.com/graphql", UnknownHostException()))
        }

        assertFalse(monitor.isDown.value)
    }

    @Test
    @DisplayName("障害中に正常応答が返ると復旧と判定する")
    fun recoversOnSuccess() {
        interceptor.intercept(chain("https://api.annict.com/graphql", 503))
        assertTrue(monitor.isDown.value)

        interceptor.intercept(chain("https://api.annict.com/graphql", 200))

        assertFalse(monitor.isDown.value)
    }

    @Test
    @DisplayName("Annict以外のホストの応答は判定に使わない")
    fun ignoresOtherHosts() {
        interceptor.intercept(chain("https://api.myanimelist.net/v2/anime/1", 503))

        assertFalse(monitor.isDown.value)
    }
}
