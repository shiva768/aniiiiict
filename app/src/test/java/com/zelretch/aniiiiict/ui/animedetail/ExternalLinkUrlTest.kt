package com.zelretch.aniiiiict.ui.animedetail

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("normalizeExternalUrl")
class ExternalLinkUrlTest {

    @Test
    @DisplayName("null・空文字・空白だけなら null（リンクを出さない）")
    fun blankIsNull() {
        assertNull(normalizeExternalUrl(null))
        assertNull(normalizeExternalUrl(""))
        assertNull(normalizeExternalUrl("  "))
    }

    @Test
    @DisplayName("スキームがあればそのまま（前後の空白は除く）")
    fun keepsScheme() {
        assertEquals("https://ja.wikipedia.org/wiki/A", normalizeExternalUrl(" https://ja.wikipedia.org/wiki/A "))
        assertEquals("http://example.com", normalizeExternalUrl("http://example.com"))
    }

    @Test
    @DisplayName("スキームが無ければ https を補う")
    fun addsHttps() {
        assertEquals("https://example.com/a", normalizeExternalUrl("example.com/a"))
    }
}
