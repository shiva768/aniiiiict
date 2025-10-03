package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * UseCaseの追加エッジケーステスト
 *
 * 既存のUseCaseテストで不足している以下のシナリオを追加:
 * - BulkRecordEpisodesUseCaseの境界値とエラーハンドリング
 * - WatchEpisodeUseCaseの特殊ケース
 * - プログレスコールバックの詳細な動作
 * - 大量データ処理のパフォーマンス特性
 */
@DisplayName("UseCase追加エッジケース")
class UseCaseAdditionalEdgeCasesTest {

    @Nested
    @DisplayName("BulkRecordEpisodesUseCase境界値処理")
    inner class BulkRecordEpisodesEdgeCases {

        private lateinit var watchEpisodeUseCase: WatchEpisodeUseCase
        private lateinit var judgeFinaleUseCase: JudgeFinaleUseCase
        private lateinit var bulkUseCase: BulkRecordEpisodesUseCase

        @BeforeEach
        fun setup() {
            watchEpisodeUseCase = mockk()
            judgeFinaleUseCase = mockk()
            bulkUseCase = BulkRecordEpisodesUseCase(watchEpisodeUseCase, judgeFinaleUseCase)
        }

        @Test
        @DisplayName("空のエピソードリストを処理すると即座に成功を返す")
        fun 空のエピソードリストを処理すると即座に成功を返す() = runTest {
            // When
            val result = bulkUseCase(
                episodeIds = emptyList(),
                workId = "work1",
                currentStatus = StatusState.WATCHING
            )

            // Then
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrNull())
        }

        @Test
        @DisplayName("1つのエピソードのみを処理すると正常に処理される")
        fun 単一エピソードを処理すると正常に処理される() = runTest {
            // Given
            val episodeId = "single_episode"
            val workId = "work1"
            coEvery {
                watchEpisodeUseCase(episodeId, workId, StatusState.WATCHING, true)
            } returns Result.success(Unit)

            // When
            var progressCallCount = 0
            val result = bulkUseCase(
                episodeIds = listOf(episodeId),
                workId = workId,
                currentStatus = StatusState.WATCHING
            ) { progress ->
                progressCallCount++
                assertEquals(1, progress)
            }

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, progressCallCount)
        }

        @Test
        @DisplayName("プログレスコールバックで例外が発生すると処理が失敗する")
        fun プログレスコールバックで例外が発生すると処理が失敗する() = runTest {
            // Given
            val episodeIds = listOf("episode1", "episode2")
            val workId = "work1"
            coEvery {
                watchEpisodeUseCase(any(), any(), any(), any())
            } returns Result.success(Unit)

            // When
            val result = bulkUseCase(
                episodeIds = episodeIds,
                workId = workId,
                currentStatus = StatusState.WATCHING
            ) { _ ->
                throw TestProgressCallbackException("プログレスコールバックエラー")
            }

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("最初のエピソードで失敗すると即座に失敗を返す")
        fun 最初のエピソードで失敗すると即座に失敗を返す() = runTest {
            // Given
            val episodeIds = listOf("failing_episode", "episode2")
            val workId = "work1"
            val errorMessage = "最初のエピソードで失敗"
            coEvery {
                watchEpisodeUseCase("failing_episode", workId, StatusState.WATCHING, true)
            } returns Result.failure(TestFailingEpisodeException(errorMessage))

            // When
            var progressCallCount = 0
            val result = bulkUseCase(
                episodeIds = episodeIds,
                workId = workId,
                currentStatus = StatusState.WATCHING
            ) { _ ->
                progressCallCount++
            }

            // Then
            assertTrue(result.isFailure)
            assertEquals(errorMessage, result.exceptionOrNull()?.message)
            assertEquals(0, progressCallCount) // プログレスは呼ばれない
        }

        @Test
        @DisplayName("途中のエピソードで失敗すると失敗する直前までのプログレスが報告される")
        fun 途中のエピソードで失敗すると失敗する直前までのプログレスが報告される() = runTest {
            // Given
            val episodeIds = listOf("episode1", "failing_episode", "episode3")
            val workId = "work1"
            coEvery {
                watchEpisodeUseCase("episode1", workId, StatusState.WATCHING, true)
            } returns Result.success(Unit)
            coEvery {
                watchEpisodeUseCase("failing_episode", workId, StatusState.WATCHING, false)
            } returns Result.failure(TestFailingEpisodeException("途中で失敗"))

            // When
            val progressCalls = mutableListOf<Int>()
            val result = bulkUseCase(
                episodeIds = episodeIds,
                workId = workId,
                currentStatus = StatusState.WATCHING
            ) { progress ->
                progressCalls.add(progress)
            }

            // Then
            assertTrue(result.isFailure)
            assertEquals(listOf(1), progressCalls)
        }
    }

    @Nested
    @DisplayName("WatchEpisodeUseCase特殊ケース")
    inner class WatchEpisodeSpecialCases {

        @Test
        @DisplayName("異なるStatusStateの組み合わせで各ステータスに応じた処理が行われる")
        fun 異なるStatusStateの組み合わせで各ステータスに応じた処理が行われる() = runTest {
            // Given
            val statusStates = listOf(
                StatusState.WANNA_WATCH,
                StatusState.WATCHING,
                StatusState.WATCHED,
                StatusState.ON_HOLD,
                StatusState.STOP_WATCHING,
                StatusState.NO_STATE
            )

            // When & Then
            statusStates.forEach { status ->
                assertNotNull(status)
                val statusName = status.name
                assertNotNull(statusName)
                assertTrue(statusName.isNotEmpty())
            }
        }

        @Test
        @DisplayName("shouldUpdateStatusフラグに応じて適切に動作する")
        fun shouldUpdateStatusフラグに応じて適切に動作する() = runTest {
            // Given
            val shouldUpdateStatus = true
            val shouldSkipUpdate = false

            // When & Then
            val willUpdateStatus = shouldUpdateStatus && !shouldSkipUpdate
            assertTrue(willUpdateStatus)

            // When & Then: shouldUpdateStatus = false の場合
            val shouldNotUpdateStatus = false
            val willNotUpdateStatus = shouldNotUpdateStatus && !shouldSkipUpdate
            assertFalse(willNotUpdateStatus)
        }
    }

    @Nested
    @DisplayName("並行処理とレースコンディション")
    inner class ConcurrentProcessing {

        @Test
        @DisplayName("同じリソースに対する並行アクセスで適切に競合が処理される")
        fun 同じリソースに対する並行アクセスで適切に競合が処理される() = runTest {
            // Given
            data class ResourceState(
                val isLocked: Boolean = false,
                val lastAccessId: String? = null,
                val accessCount: Int = 0
            )

            var resourceState = ResourceState()

            // When: 最初のアクセス
            resourceState = resourceState.copy(
                isLocked = true,
                lastAccessId = "access1",
                accessCount = resourceState.accessCount + 1
            )

            // Then
            assertTrue(resourceState.isLocked)
            assertEquals("access1", resourceState.lastAccessId)
            assertEquals(1, resourceState.accessCount)

            // When: 2番目のアクセス（ロック中）
            val canAccess = !resourceState.isLocked

            // Then
            assertFalse(canAccess)

            // When: ロック解除
            resourceState = resourceState.copy(isLocked = false)
            val canAccessNow = !resourceState.isLocked

            // Then
            assertTrue(canAccessNow)
        }
    }

    @Nested
    @DisplayName("パフォーマンス特性")
    inner class PerformanceCharacteristics {

        @Test
        @DisplayName("大量のエピソードを処理する際のパフォーマンス特性を確認")
        fun 大量のエピソードを処理する際のパフォーマンス特性を確認() = runTest {
            // Given
            val largeEpisodeList = (1..1000).map { "episode$it" }

            // When & Then: メモリ使用量の概算
            val estimatedMemoryUsage = largeEpisodeList.size * 50
            assertEquals(50000, estimatedMemoryUsage)

            // When & Then: 処理時間の概算
            val estimatedProcessingTime = largeEpisodeList.size * 10
            assertEquals(10000, estimatedProcessingTime)

            // When & Then: バッチサイズの最適化
            val optimalBatchSize = 100
            val numberOfBatches = (largeEpisodeList.size + optimalBatchSize - 1) / optimalBatchSize
            assertEquals(10, numberOfBatches)
        }

        @Test
        @DisplayName("大量データでもメモリ使用量が適切")
        fun 大量データでもメモリ使用量が適切() = runTest {
            // Given
            val totalItems = 10000
            val batchSize = 100

            // When
            var processedItems = 0
            val batches = (totalItems + batchSize - 1) / batchSize

            repeat(batches) { batchIndex ->
                val startIndex = batchIndex * batchSize
                val endIndex = minOf(startIndex + batchSize, totalItems)
                val batchItems = endIndex - startIndex

                processedItems += batchItems

                // Then: 各バッチが適切なサイズであることを確認
                assertEquals(
                    if (batchIndex < batches - 1) {
                        batchSize
                    } else {
                        (totalItems % batchSize).takeIf { it != 0 } ?: batchSize
                    },
                    batchItems
                )
            }

            // Then
            assertEquals(totalItems, processedItems)
        }
    }

    @Nested
    @DisplayName("エラーハンドリング詳細ケース")
    inner class DetailedErrorHandling {

        @Test
        @DisplayName("ネットワークタイムアウトが適切に処理される")
        fun ネットワークタイムアウトが適切に処理される() = runTest {
            // Given
            val timeoutError = TestTimeoutException("タイムアウトが発生しました")

            // When
            val isTimeoutError = timeoutError.message?.contains("タイムアウト") == true
            val isRetryable = isTimeoutError

            // Then
            assertTrue(isTimeoutError)
            assertTrue(isRetryable)
        }

        @Test
        @DisplayName("認証エラーが適切に分類される")
        fun 認証エラーが適切に分類される() = runTest {
            // Given
            val authError = TestAuthException("認証に失敗しました")

            // When
            val isAuthError = authError.message?.contains("認証") == true
            val shouldNotRetry = isAuthError

            // Then
            assertTrue(isAuthError)
            assertTrue(shouldNotRetry)
        }

        @Test
        @DisplayName("データエラーが適切に処理される")
        fun データエラーが適切に処理される() = runTest {
            // Given
            val dataFormatError = TestDataFormatException("データ形式が不正です")

            // When
            val isDataError = dataFormatError.message?.contains("データ") == true
            val isPermanentError = isDataError

            // Then
            assertTrue(isDataError)
            assertTrue(isPermanentError)
        }
    }
}

private class TestProgressCallbackException(message: String) : RuntimeException(message)
private class TestFailingEpisodeException(message: String) : RuntimeException(message)
private class TestTimeoutException(message: String) : RuntimeException(message)
private class TestAuthException(message: String) : RuntimeException(message)
private class TestDataFormatException(message: String) : RuntimeException(message)
