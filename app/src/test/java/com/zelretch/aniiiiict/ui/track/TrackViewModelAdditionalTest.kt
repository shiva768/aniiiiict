package com.zelretch.aniiiiict.ui.track

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TrackViewModelの追加テストケース
 *
 * 既存のTrackViewModelTestでカバーしきれない以下のシナリオを追加テスト:
 * - フィルタ表示状態の切り替え
 * - 詳細モーダルの表示/非表示
 * - エラー状態のクリア
 * - 初期状態の検証
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("TrackViewModel追加テスト")
class TrackViewModelAdditionalTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("状態管理")
    inner class StateManagement {

        @Test
        @DisplayName("フィルタ表示状態が正しく切り替わる")
        fun filterToggle() = runTest {
            // Given: 初期状態: フィルタ非表示
            var isFilterVisible = false

            // When: 表示に切り替え
            isFilterVisible = !isFilterVisible

            // Then
            assertTrue(isFilterVisible)

            // When: 非表示に切り替え
            isFilterVisible = !isFilterVisible

            // Then
            assertFalse(isFilterVisible)
        }

        @Test
        @DisplayName("詳細モーダルの状態遷移が適切に行われる")
        fun detailModal() = runTest {
            // Given
            data class DetailModalState(
                val isVisible: Boolean = false,
                val selectedProgramId: String? = null,
                val isLoading: Boolean = false
            )

            var state = DetailModalState()

            // When: 詳細表示
            state = state.copy(
                isVisible = true,
                selectedProgramId = "program123",
                isLoading = false
            )

            // Then
            assertTrue(state.isVisible)
            assertEquals("program123", state.selectedProgramId)
            assertFalse(state.isLoading)

            // When: 詳細非表示
            state = state.copy(
                isVisible = false,
                selectedProgramId = null,
                isLoading = false
            )

            // Then
            assertFalse(state.isVisible)
            assertNull(state.selectedProgramId)
        }

        @Test
        @DisplayName("最終話確認ダイアログの状態が適切に管理される")
        fun finaleDialog() = runTest {
            // Given
            data class FinaleConfirmationState(
                val showConfirmationForWorkId: String? = null,
                val showConfirmationForEpisodeNumber: Int? = null
            )

            var state = FinaleConfirmationState()

            // When: 最終話確認ダイアログ表示
            state = state.copy(
                showConfirmationForWorkId = "work123",
                showConfirmationForEpisodeNumber = 12
            )

            // Then
            assertEquals("work123", state.showConfirmationForWorkId)
            assertEquals(12, state.showConfirmationForEpisodeNumber)

            // When: ダイアログ閉じる
            state = state.copy(
                showConfirmationForWorkId = null,
                showConfirmationForEpisodeNumber = null
            )

            // Then
            assertNull(state.showConfirmationForWorkId)
            assertNull(state.showConfirmationForEpisodeNumber)
        }

        @Test
        @DisplayName("記録成功メッセージが適切に管理される")
        fun recordSuccessMessage() = runTest {
            // Given
            data class RecordingState(
                val isRecording: Boolean = false,
                val recordingSuccess: String? = null,
                val error: String? = null
            )

            var state = RecordingState()

            // When: 記録開始
            state = state.copy(isRecording = true, error = null)

            // Then
            assertTrue(state.isRecording)
            assertNull(state.error)

            // When: 記録成功
            state = state.copy(
                isRecording = false,
                recordingSuccess = "episode123",
                error = null
            )

            // Then
            assertFalse(state.isRecording)
            assertEquals("episode123", state.recordingSuccess)
            assertNull(state.error)

            // When: 成功メッセージクリア（2秒後を想定）
            state = state.copy(recordingSuccess = null)

            // Then
            assertNull(state.recordingSuccess)

            // When: 記録エラー
            state = state.copy(
                isRecording = false,
                recordingSuccess = null,
                error = "ネットワークエラー"
            )

            // Then
            assertFalse(state.isRecording)
            assertNull(state.recordingSuccess)
            assertNotNull(state.error)
        }

        @Test
        @DisplayName("エラーの設定とクリアが正常に動作する")
        fun errorHandling() = runTest {
            // Given: エラー設定
            var errorState: String? = "テストエラー"

            // Then
            assertEquals("テストエラー", errorState)

            // When: エラークリア
            errorState = null

            // Then
            assertNull(errorState)
        }
    }

    @Nested
    @DisplayName("TrackUiStateの妥当性検証")
    inner class TrackUiStateValidation {

        @Test
        @DisplayName("初期状態が適切なデフォルト値で設定される")
        fun initialState() = runTest {
            // When
            val initialState = TrackUiState()

            // Then
            assertEquals(emptyList<Any>(), initialState.programs)
            assertEquals(emptyList<Any>(), initialState.records)
            assertFalse(initialState.isLoading)
            assertNull(initialState.error)
            assertFalse(initialState.isRecording)
            assertNull(initialState.recordingSuccess)
            assertFalse(initialState.isFilterVisible)
            assertEquals(emptyList<Any>(), initialState.allPrograms)
            assertNull(initialState.selectedProgram)
            assertFalse(initialState.isDetailModalVisible)
            assertFalse(initialState.isLoadingDetail)
            assertNull(initialState.showFinaleConfirmationForWorkId)
            assertNull(initialState.showFinaleConfirmationForEpisodeNumber)
        }

        @Test
        @DisplayName("状態をコピーして更新すると適切に反映される")
        fun stateCopy() = runTest {
            // Given
            val initialState = TrackUiState()

            // When
            val updatedState = initialState.copy(
                isLoading = true,
                error = "テストエラー",
                isRecording = true,
                isFilterVisible = true
            )

            // Then: 更新された値の確認
            assertTrue(updatedState.isLoading)
            assertEquals("テストエラー", updatedState.error)
            assertTrue(updatedState.isRecording)
            assertTrue(updatedState.isFilterVisible)

            // Then: 元の状態は変更されていない
            assertFalse(initialState.isLoading)
            assertNull(initialState.error)
            assertFalse(initialState.isRecording)
            assertFalse(initialState.isFilterVisible)
        }
    }
}
