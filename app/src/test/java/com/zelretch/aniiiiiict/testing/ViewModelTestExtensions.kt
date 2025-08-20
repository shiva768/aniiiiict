package com.zelretch.aniiiiiict.testing

import com.zelretch.aniiiiiict.MainUiState
import com.zelretch.aniiiiiict.MainViewModel
import com.zelretch.aniiiiiict.ui.base.BaseUiState
import com.zelretch.aniiiiiict.ui.track.TrackUiState
import com.zelretch.aniiiiiict.ui.track.TrackViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.reflect.Field

/**
 * テスト専用のViewModel拡張機能
 * プロダクションコードを汚染することなく、テスト時のUI状態操作を可能にする
 */

/**
 * テスト可能なViewModel共通インターフェース
 */
interface TestableViewModel<T : BaseUiState> {
    fun setUiStateForTest(state: T)
    fun setErrorForTest(error: String?)
    fun setLoadingForTest(isLoading: Boolean)
    fun resetToInitialState()
}

/**
 * MainViewModelのテスト用拡張インターフェース
 */
interface TestableMainViewModel : TestableViewModel<MainUiState>

/**
 * TrackViewModelのテスト用拡張インターフェース
 */
interface TestableTrackViewModel : TestableViewModel<TrackUiState>

/**
 * ViewModelをテスト可能にするラッパークラス（汎用）
 */
open class ViewModelTestWrapper<T : BaseUiState>(
    private val viewModel: Any,
    private val initialStateFactory: () -> T,
    private val uiStateFieldName: String = "_uiState"
) : TestableViewModel<T> {

    private val uiStateField: Field by lazy {
        viewModel::class.java.getDeclaredField(uiStateFieldName).apply {
            isAccessible = true
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getMutableUiState(): MutableStateFlow<T> = uiStateField.get(viewModel) as MutableStateFlow<T>

    override fun setUiStateForTest(state: T) {
        getMutableUiState().value = state
    }

    override fun setErrorForTest(error: String?) {
        val currentState = getMutableUiState().value
        // BaseUiStateのコピーメソッドを使用（リフレクションで型安全に）
        try {
            val copyMethod = currentState::class.java.getMethod("copy", String::class.java)
            @Suppress("UNCHECKED_CAST")
            getMutableUiState().value = copyMethod.invoke(currentState, error) as T
        } catch (e: Exception) {
            Timber.e(e, "リフレクションによるsetErrorForTestの実行に失敗。直接フィールドを更新します。")
            // Fallback: 直接フィールドを更新
            setErrorDirectly(error)
        }
    }

    override fun setLoadingForTest(isLoading: Boolean) {
        val currentState = getMutableUiState().value
        try {
            val copyMethod = currentState::class.java.getMethod("copy", Boolean::class.java)
            @Suppress("UNCHECKED_CAST")
            getMutableUiState().value = copyMethod.invoke(currentState, isLoading) as T
        } catch (e: Exception) {
            Timber.e(e, "リフレクションによるsetLoadingForTestの実行に失敗。直接フィールドを更新します。")
            // Fallback: 直接フィールドを更新
            setLoadingDirectly(isLoading)
        }
    }

    override fun resetToInitialState() {
        getMutableUiState().value = initialStateFactory()
    }

    private fun setErrorDirectly(error: String?) {
        val currentState = getMutableUiState().value
        val errorField = BaseUiState::class.java.getDeclaredField("error")
        errorField.isAccessible = true
        errorField.set(currentState, error)
    }

    private fun setLoadingDirectly(isLoading: Boolean) {
        val currentState = getMutableUiState().value
        val loadingField = BaseUiState::class.java.getDeclaredField("isLoading")
        loadingField.isAccessible = true
        loadingField.set(currentState, isLoading)
    }
}

/**
 * MainViewModel専用ラッパー
 */
class MainViewModelTestWrapper(viewModel: MainViewModel) :
    ViewModelTestWrapper<MainUiState>(viewModel, { MainUiState() }), TestableMainViewModel

/**
 * TrackViewModel専用ラッパー
 */
class TrackViewModelTestWrapper(viewModel: TrackViewModel) :
    ViewModelTestWrapper<TrackUiState>(viewModel, { TrackUiState() }), TestableTrackViewModel

/**
 * MainViewModel用の拡張関数
 */
fun MainViewModel.asTestable(): TestableMainViewModel = MainViewModelTestWrapper(this)

/**
 * TrackViewModel用の拡張関数
 */
fun TrackViewModel.asTestable(): TestableTrackViewModel = TrackViewModelTestWrapper(this)

/**
 * 汎用的なViewModel状態操作のためのヘルパー関数群
 */
object ViewModelTestHelpers {

    /**
     * 任意のViewModelの状態を直接設定する（リフレクション使用）
     */
    inline fun <reified T : BaseUiState> setViewModelState(viewModel: Any, state: T, fieldName: String = "_uiState") {
        val field = viewModel::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<T>
        stateFlow.value = state
    }

    /**
     * ViewModelの状態フィールドにアクセスする
     */
    inline fun <reified T : BaseUiState> getViewModelState(
        viewModel: Any,
        fieldName: String = "_uiState"
    ): StateFlow<T> {
        val field = viewModel::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(viewModel) as StateFlow<T>
    }
}

/**
 * テスト用のMainUiState作成ヘルパー
 */
object MainUiStateBuilder {
    fun loading() = MainUiState(isLoading = true)

    fun error(message: String) = MainUiState(error = message)

    fun authenticating() = MainUiState(isAuthenticating = true)

    fun authenticated() = MainUiState(isAuthenticated = true)

    fun custom(
        isLoading: Boolean = false,
        error: String? = null,
        isAuthenticating: Boolean = false,
        isAuthenticated: Boolean = false
    ) = MainUiState(
        isLoading = isLoading,
        error = error,
        isAuthenticating = isAuthenticating,
        isAuthenticated = isAuthenticated
    )
}

/**
 * テスト用のTrackUiState作成ヘルパー
 */
object TrackUiStateBuilder {
    fun loading() = TrackUiState(isLoading = true)

    fun error(message: String) = TrackUiState(error = message)

    fun withPrograms(programs: List<com.zelretch.aniiiiiict.data.model.ProgramWithWork>) =
        TrackUiState(programs = programs)

    fun recording() = TrackUiState(isRecording = true)

    fun filterVisible() = TrackUiState(isFilterVisible = true)

    fun custom(
        programs: List<com.zelretch.aniiiiiict.data.model.ProgramWithWork> = emptyList(),
        isLoading: Boolean = false,
        error: String? = null,
        isRecording: Boolean = false,
        isFilterVisible: Boolean = false
    ) = TrackUiState(
        programs = programs,
        isLoading = isLoading,
        error = error,
        isRecording = isRecording,
        isFilterVisible = isFilterVisible
    )
}
