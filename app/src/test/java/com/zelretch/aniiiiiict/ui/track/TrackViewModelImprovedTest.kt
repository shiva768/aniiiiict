package com.zelretch.aniiiiiict.ui.track

import com.zelretch.aniiiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.usecase.*
import com.zelretch.aniiiiiict.util.Logger
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*

/**
 * TrackViewModelの改善されたテストクラス
 * インターフェースベースのテストとテスト用ユーティリティを活用
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackViewModelImprovedTest : BehaviorSpec({
    val dispatcher = UnconfinedTestDispatcher()
    
    beforeSpec {
        Dispatchers.setMain(dispatcher)
    }
    
    afterSpec {
        Dispatchers.resetMain()
    }
    
    given("TrackViewModel with improved testability") {
        val filterStateFlow = MutableStateFlow(FilterState())
        val filterPreferences = mockk<FilterPreferences> {
            every { filterState } returns filterStateFlow
        }
        val loadProgramsUseCase = mockk<LoadProgramsUseCase>()
        val watchEpisodeUseCase = mockk<WatchEpisodeUseCase>()
        val filterProgramsUseCase = mockk<FilterProgramsUseCase>()
        val judgeFinalUseCase = mockk<JudgeFinaleUseCase>()
        val logger = mockk<Logger>(relaxed = true)
        
        lateinit var viewModel: TrackViewModel
        lateinit var viewModelContract: TrackViewModelContract
        lateinit var testableViewModel: TestableTrackViewModel
        lateinit var testScope: TestScope
        
        beforeTest {
            testScope = TestScope(dispatcher)
            
            // デフォルトのモック設定
            coEvery { loadProgramsUseCase.invoke() } returns flowOf(emptyList())
            every { filterProgramsUseCase.invoke(any(), any()) } answers { firstArg() }
            
            viewModel = TrackViewModel(
                loadProgramsUseCase,
                watchEpisodeUseCase,
                filterProgramsUseCase,
                filterPreferences,
                judgeFinalUseCase,
                logger
            )
            
            // インターフェースとして参照
            viewModelContract = viewModel
            testableViewModel = viewModel
            
            // テスト用スコープを設定
            testableViewModel.externalScope = testScope
            testScope.testScheduler.advanceUntilIdle()
        }
        
        `when`("インターフェースベースのUI操作テスト") {
            then("フィルター表示切り替えをインターフェース経由でテスト") {
                val initialState = viewModelContract.uiState.value
                initialState.isFilterVisible shouldBe false
                
                viewModelContract.toggleFilterVisibility()
                testScope.testScheduler.advanceUntilIdle()
                
                viewModelContract.uiState.value.isFilterVisible shouldBe true
            }
            
            then("詳細モーダル表示をインターフェース経由でテスト") {
                val mockProgram = mockk<ProgramWithWork>(relaxed = true)
                
                viewModelContract.showDetailModal(mockProgram)
                testScope.testScheduler.advanceUntilIdle()
                
                with(viewModelContract.uiState.value) {
                    isDetailModalVisible shouldBe true
                    selectedProgram shouldBe mockProgram
                }
                
                viewModelContract.hideDetailModal()
                testScope.testScheduler.advanceUntilIdle()
                
                with(viewModelContract.uiState.value) {
                    isDetailModalVisible shouldBe false
                    selectedProgram shouldBe null
                }
            }
            
            then("最終話確認ダイアログをインターフェース経由でテスト") {
                val workId = "test-work-id"
                val episodeNumber = 12
                
                viewModelContract.showFinaleConfirmation(workId, episodeNumber)
                testScope.testScheduler.advanceUntilIdle()
                
                with(viewModelContract.uiState.value) {
                    showFinaleConfirmationForWorkId shouldBe workId
                    showFinaleConfirmationForEpisodeNumber shouldBe episodeNumber
                }
                
                viewModelContract.hideFinaleConfirmation()
                testScope.testScheduler.advanceUntilIdle()
                
                with(viewModelContract.uiState.value) {
                    showFinaleConfirmationForWorkId shouldBe null
                    showFinaleConfirmationForEpisodeNumber shouldBe null
                }
            }
        }
        
        `when`("テスト用状態操作機能") {
            then("UI状態を直接設定してテストシナリオを作成") {
                val mockPrograms = listOf<ProgramWithWork>(mockk(relaxed = true), mockk(relaxed = true))
                val testState = TrackUiState(
                    programs = mockPrograms,
                    isLoading = false,
                    error = null,
                    isFilterVisible = true,
                    allPrograms = mockPrograms
                )
                
                // 複雑な状態をワンステップで設定
                testableViewModel.setUiStateForTest(testState)
                
                with(viewModelContract.uiState.value) {
                    programs.size shouldBe 2
                    isFilterVisible shouldBe true
                    allPrograms.size shouldBe 2
                }
            }
            
            then("エラー状態からの回復をテスト") {
                // エラー状態を設定
                val errorState = TrackUiState(
                    error = "データ取得エラー",
                    isLoading = false
                )
                testableViewModel.setUiStateForTest(errorState)
                
                viewModelContract.uiState.value.error shouldBe "データ取得エラー"
                
                // エラーをクリア
                viewModelContract.clearError()
                
                viewModelContract.uiState.value.error shouldBe null
            }
        }
        
        `when`("エピソード視聴記録のテスト") {
            then("エピソード視聴をインターフェース経由でテスト") {
                val mockProgram = mockk<ProgramWithWork>(relaxed = true)
                val mockEpisode = mockk<com.zelretch.aniiiiiict.data.model.Program>(relaxed = true)
                val mockEpisodeData = mockk<com.zelretch.aniiiiiict.data.model.Episode>(relaxed = true)
                
                every { mockProgram.programs } returns listOf(mockEpisode)
                every { mockEpisode.episode } returns mockEpisodeData
                every { mockEpisodeData.number } returns 5
                every { mockEpisodeData.id } returns "episode-id"
                every { mockProgram.work.id } returns "work-id"
                every { mockProgram.work.viewerStatusState } returns com.annict.type.StatusState.WATCHING
                
                coEvery { watchEpisodeUseCase.invoke(any(), any(), any()) } returns Result.success(Unit)
                
                viewModelContract.watchEpisode(mockProgram, 5)
                testScope.testScheduler.advanceUntilIdle()
                
                coVerify { watchEpisodeUseCase.invoke("episode-id", "work-id", com.annict.type.StatusState.WATCHING) }
            }
        }
    }
})