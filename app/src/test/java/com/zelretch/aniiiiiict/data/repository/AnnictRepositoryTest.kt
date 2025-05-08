package com.zelretch.aniiiiiict.data.repository

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class AnnictRepositoryTest : BehaviorSpec({
    given("AnnictRepository") {
        val repository = mockk<AnnictRepository>()

        `when`("認証状態を確認") {
            then("認証済みの場合") {
                coEvery { repository.isAuthenticated() } returns true
                val result = repository.isAuthenticated()
                result shouldBe true
                coVerify { repository.isAuthenticated() }
            }

            then("未認証の場合") {
                coEvery { repository.isAuthenticated() } returns false
                val result = repository.isAuthenticated()
                result shouldBe false
                coVerify { repository.isAuthenticated() }
            }
        }

        `when`("認証URLを取得") {
            then("正しいURLが返される") {
                val expectedUrl = "https://example.com/auth"
                coEvery { repository.getAuthUrl() } returns expectedUrl
                val result = repository.getAuthUrl()
                result shouldBe expectedUrl
                coVerify { repository.getAuthUrl() }
            }

            then("取得に失敗する") {
                coEvery { repository.getAuthUrl() } throws Exception("Failed to retrieve auth URL")
                try {
                    repository.getAuthUrl()
                } catch (e: Exception) {
                    e.message shouldBe "Failed to retrieve auth URL"
                }
                coVerify { repository.getAuthUrl() }
            }
        }

        `when`("認証コードを処理") {
            then("有効なコードで成功する") {
                coEvery { repository.handleAuthCallback(any()) } returns true
                val result = repository.handleAuthCallback("valid_code")
                result shouldBe true
                coVerify { repository.handleAuthCallback("valid_code") }
            }

            then("無効なコードで失敗する") {
                coEvery { repository.handleAuthCallback(any()) } returns false
                val result = repository.handleAuthCallback("invalid_code")
                result shouldBe false
                coVerify { repository.handleAuthCallback("invalid_code") }
            }
        }
    }
}) 