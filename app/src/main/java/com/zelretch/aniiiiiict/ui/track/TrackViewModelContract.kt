package com.zelretch.aniiiiiict.ui.track

import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.ui.base.ViewModelContract
import kotlinx.coroutines.CoroutineScope

/**
 * TrackViewModelの契約を定義するインターフェース
 * テスト容易性向上のため、ViewModelの公開APIを定義
 */
interface TrackViewModelContract : ViewModelContract<TrackUiState> {
    /**
     * エピソードを視聴済みとして記録する
     * @param program 対象の番組
     * @param episodeNumber エピソード番号
     */
    fun watchEpisode(program: ProgramWithWork, episodeNumber: Int)

    /**
     * フィルターの表示/非表示を切り替える
     */
    fun toggleFilterVisibility()

    /**
     * 詳細モーダルを表示する
     * @param program 対象の番組
     */
    fun showDetailModal(program: ProgramWithWork)

    /**
     * 詳細モーダルを閉じる
     */
    fun hideDetailModal()

    /**
     * 最終話確認ダイアログを表示する
     * @param workId 作品ID
     * @param episodeNumber エピソード番号
     */
    fun showFinaleConfirmation(workId: String, episodeNumber: Int)

    /**
     * 最終話確認ダイアログを閉じる
     */
    fun hideFinaleConfirmation()

    /**
     * 最終話として記録する
     * @param workId 作品ID
     * @param episodeNumber エピソード番号
     */
    fun recordFinale(workId: String, episodeNumber: Int)
}

/**
 * TrackViewModel専用のテスト用インターフェース
 */
interface TestableTrackViewModel {
    /**
     * テスト用: コルーチンスコープを設定する
     * @param scope テスト用のCoroutineScope
     */
    var externalScope: CoroutineScope?

    /**
     * テスト用: UI状態を直接設定する
     * @param state 設定するUI状態
     */
    fun setUiStateForTest(state: TrackUiState)
}
