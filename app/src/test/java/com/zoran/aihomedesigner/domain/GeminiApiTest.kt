package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiTest {

    private fun image(body: String) = GeminiApi.parseResponse(200, body) as GeminiResult.Image

    private fun failure(status: Int, body: String) = GeminiApi.parseResponse(status, body) as GeminiResult.Failure

    @Test
    fun endpointContainsTheModelAndNoKey() {
        val url = GeminiApi.endpoint(" gemini-nano-banana-2.1 ")
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-nano-banana-2.1:generateContent", url)
        assertFalse(url.contains("key"))
    }

    @Test
    fun acceptsOnlySafeModelNames() {
        assertTrue(GeminiApi.isValidModelName("gemini-3.1-flash-image"))
        assertTrue(GeminiApi.isValidModelName("  gemini-2.5-flash-image "))
        assertFalse(GeminiApi.isValidModelName(""))
        assertFalse(GeminiApi.isValidModelName("model/../x"))
        assertFalse(GeminiApi.isValidModelName("a b"))
        assertFalse(GeminiApi.isValidModelName("m?key=1"))
        assertFalse(GeminiApi.isValidModelName("a".repeat(81)))
    }

    @Test
    fun requestBodyEscapesThePromptAndAsksForAnImage() {
        val body = GeminiApi.requestBody("Soba \"A\"\nnova\\linija\tx")
        assertEquals(
            "{\"contents\":[{\"parts\":[{\"text\":\"Soba \\\"A\\\"\\nnova\\\\linija\\tx\"}]}]," +
                "\"generationConfig\":{\"responseModalities\":[\"TEXT\",\"IMAGE\"]}}",
            body
        )
    }

    @Test
    fun controlCharactersAreEscaped() {
        assertEquals("\"a\\u0001b\"", GeminiApi.jsonString("a\u0001b"))
    }

    @Test
    fun readsTheImageFromASuccessfulResponse() {
        val body = """{"candidates":[{"content":{"parts":[{"text":"Evo slike"},
            {"inlineData":{"mimeType":"image/png","data":"AAAA/BBB+CC=="}}]}}]}"""
        val result = image(body)
        assertEquals("image/png", result.mimeType)
        assertEquals("AAAA/BBB+CC==", result.base64)
    }

    @Test
    fun readsEscapedCharactersInTheImageData() {
        val body = """{"candidates":[{"content":{"parts":[{"inlineData":{"mimeType":"image/jpeg","data":"QUJD\/REVG\u002bR0hJ\u003d"}}]}}]}"""
        assertEquals("QUJD/REVG+R0hJ=", image(body).base64)
    }

    @Test
    fun readsSnakeCaseFieldsToo() {
        val body = """{"candidates":[{"content":{"parts":[{"inline_data":{"mime_type":"image/webp","data":"ZZZZ"}}]}}]}"""
        val result = image(body)
        assertEquals("image/webp", result.mimeType)
        assertEquals("ZZZZ", result.base64)
    }

    @Test
    fun readsAVeryLargeImageWithoutFailing() {
        val data = "QUJD".repeat(1_000_000) // oko 4 MB
        val body = """{"candidates":[{"content":{"parts":[{"inlineData":{"mimeType":"image/png","data":"$data"}}]}}]}"""
        assertEquals(data.length, image(body).base64.length)
    }

    @Test
    fun reportsTheServiceMessageOnAnHttpError() {
        val result = failure(400, """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}""")
        assertEquals(GeminiResult.Kind.HTTP_ERROR, result.kind)
        assertEquals("API key not valid. Please pass a valid API key.", result.detail)
    }

    @Test
    fun treatsStatus429AsRateLimited() {
        val result = failure(429, """{"error":{"code":429,"message":"Quota exceeded"}}""")
        assertEquals(GeminiResult.Kind.RATE_LIMITED, result.kind)
        assertEquals("Quota exceeded", result.detail)
    }

    @Test
    fun fallsBackToTheStatusWhenThereIsNoMessage() {
        val result = failure(503, "<html>greška</html>")
        assertEquals(GeminiResult.Kind.HTTP_ERROR, result.kind)
        assertEquals("HTTP 503", result.detail)
    }

    @Test
    fun reportsABlockedRequest() {
        val result = failure(200, """{"promptFeedback":{"blockReason":"SAFETY"}}""")
        assertEquals(GeminiResult.Kind.BLOCKED, result.kind)
        assertEquals("SAFETY", result.detail)
    }

    @Test
    fun reportsAResponseWithoutAnImage() {
        val result = failure(200, """{"candidates":[{"content":{"parts":[{"text":"Ne mogu da nacrtam."}]}}]}""")
        assertEquals(GeminiResult.Kind.NO_IMAGE, result.kind)
    }

    @Test
    fun refusesAttachmentsThatAreNotImages() {
        val result = failure(200, """{"candidates":[{"content":{"parts":[{"inlineData":{"mimeType":"text/html","data":"AAAA"}}]}}]}""")
        assertEquals(GeminiResult.Kind.NO_IMAGE, result.kind)
    }

    @Test
    fun refusesAnImageWithEmptyData() {
        val result = failure(200, """{"candidates":[{"content":{"parts":[{"inlineData":{"mimeType":"image/png","data":""}}]}}]}""")
        assertEquals(GeminiResult.Kind.NO_IMAGE, result.kind)
    }
}
