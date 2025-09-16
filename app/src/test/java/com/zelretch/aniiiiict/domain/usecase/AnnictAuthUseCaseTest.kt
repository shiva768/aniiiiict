package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class AnnictAuthUseCaseTest : BehaviorSpec({

    given("Annict認証UseCase") {
        val repository = mockk<AnnictRepository>()
        val useCase = AnnictAuthUseCase(repository)

        `when`("Repositoryが認証済みを返す") {
            then("認証済みとして返される") {
                coEvery { repository.isAuthenticated() } returns true
                useCase.isAuthenticated() shouldBe true
            }
        }

        `when`("Repositoryが未認証を返す") {
            then("未認証として返される") {
                coEvery { repository.isAuthenticated() } returns false
                useCase.isAuthenticated() shouldBe false
            }
        }

        `when`("認証URLが要求される") {
            then("正しいURLが返される") {
                val authUrl = "https://example.com/auth"
                coEvery { repository.getAuthUrl() } returns authUrl
                useCase.getAuthUrl() shouldBe authUrl
            }
        }

        `when`("有効なコードで認証コールバックを処理する") {
            then("成功として返される") {
                val code = "valid_code"
                coEvery { repository.handleAuthCallback(code) } returns true
                useCase.handleAuthCallback(code) shouldBe true
            }
        }

        `when`("nullコードで認証コールバックを処理する") {
            then("失敗として返される") {
                useCase.handleAuthCallback(null) shouldBe false
            }
        }

        `when`("無効なコードで認証コールバックを処理する") {
            then("失敗として返される") {
                val code = "invalid_code"
                coEvery { repository.handleAuthCallback(code) } returns false
                useCase.handleAuthCallback(code) shouldBe false
            }
        }
    }
})
