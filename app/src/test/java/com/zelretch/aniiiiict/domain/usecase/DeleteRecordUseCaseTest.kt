package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class レコード削除ユースケーステスト : BehaviorSpec({
    val repository = mockk<AnnictRepository>()
    val useCase = DeleteRecordUseCase(repository)

    前提("レコード削除") {
        場合("repositoryがtrueを返す場合") {
            そのとき("trueが返る") {
                coEvery { repository.deleteRecord("record1") } returns true
                val result = runBlocking { useCase("record1") }
                result shouldBe true
            }
        }
        場合("repositoryがfalseを返す場合") {
            そのとき("falseが返る") {
                coEvery { repository.deleteRecord("record2") } returns false
                val result = runBlocking { useCase("record2") }
                result shouldBe false
            }
        }
    }
})
