package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class ビュー状態更新ユースケーステスト : BehaviorSpec({
    val repository = mockk<AnnictRepository>()
    val useCase = UpdateViewStateUseCase(repository)

    前提("ステータス更新") {
        場合("リポジトリがtrueを返す") {
            そのとき("Result.successになる") {
                coEvery { repository.updateWorkViewStatus(any(), any()) } returns true
                val result = runBlocking { useCase("w1", StatusState.WATCHING) }
                result.isSuccess shouldBe true
            }
        }
        場合("リポジトリがfalseを返す") {
            そのとき("Result.successになる（警告ログ出力）") {
                coEvery { repository.updateWorkViewStatus(any(), any()) } returns false
                val result = runBlocking { useCase("w1", StatusState.WATCHING) }
                result.isSuccess shouldBe true
            }
        }
    }
})
