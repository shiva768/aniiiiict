package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AnnictRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class アニクト認証ユースケーステスト {

    @Mock
    private lateinit var repository: AnnictRepository

    private lateinit var useCase: AnnictAuthUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = AnnictAuthUseCase(repository)
    }

    @Test
    fun `isAuthenticated should return true when repository returns true`() = runTest {
        `when`(repository.isAuthenticated()).thenReturn(true)
        assertTrue(useCase.isAuthenticated())
    }

    @Test
    fun `isAuthenticated should return false when repository returns false`() = runTest {
        `when`(repository.isAuthenticated()).thenReturn(false)
        assertFalse(useCase.isAuthenticated())
    }

    @Test
    fun `getAuthUrl should return correct url`() = runTest {
        val authUrl = "https://example.com/auth"
        `when`(repository.getAuthUrl()).thenReturn(authUrl)
        assertEquals(authUrl, useCase.getAuthUrl())
    }

    @Test
    fun `handleAuthCallback should return true when code is valid`() = runTest {
        val code = "valid_code"
        `when`(repository.handleAuthCallback(code)).thenReturn(true)
        assertTrue(useCase.handleAuthCallback(code))
    }

    @Test
    fun `handleAuthCallback should return false when code is null`() = runTest {
        assertFalse(useCase.handleAuthCallback(null))
    }

    @Test
    fun `handleAuthCallback should return false when code is invalid`() = runTest {
        val code = "invalid_code"
        `when`(repository.handleAuthCallback(code)).thenReturn(false)
        assertFalse(useCase.handleAuthCallback(code))
    }
}
