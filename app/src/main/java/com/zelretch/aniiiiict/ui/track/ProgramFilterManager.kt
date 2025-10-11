package com.zelretch.aniiiiict.ui.track

import com.zelretch.aniiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.usecase.FilterProgramsUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * プログラムフィルタリング処理を管理するクラス
 *
 * TrackViewModelから抽出し、フィルタリング関連の処理をまとめる。
 */
class ProgramFilterManager @Inject constructor(
    private val filterProgramsUseCase: FilterProgramsUseCase,
    private val filterPreferences: FilterPreferences
) {
    /**
     * フィルタ状態のFlow
     */
    val filterState: Flow<FilterState> = filterPreferences.filterState

    /**
     * プログラムリストをフィルタリング
     */
    fun filterPrograms(programs: List<ProgramWithWork>, filterState: FilterState): List<ProgramWithWork> =
        filterProgramsUseCase(programs, filterState)

    /**
     * 利用可能なフィルタオプションを抽出
     */
    fun extractAvailableFilters(programs: List<ProgramWithWork>) =
        filterProgramsUseCase.extractAvailableFilters(programs)

    /**
     * フィルタ状態を更新
     */
    suspend fun updateFilterState(filterState: FilterState) {
        filterPreferences.updateFilterState(filterState)
    }
}
