package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GoogleRequester(private val baseUrl: String = DEFAULT_BASE_URL) {

    companion object {
        const val DEFAULT_BASE_URL = "https://translate.googleapis.com/translate_a/single"
        const val FALLBACK_BASE_URL = "https://translate.google.com/translate_a/single"
    }

    fun translateAuto(message: String, to: Language?): RequestResult {
        return performTranslationRequest(message, LanguageManager.autoLang, to!!)
    }

    fun performTranslationRequest(message: String, from: Language, to: Language): RequestResult {
        val encodedMessage = encodeMessage(message)
            ?: return RequestResult(2, "Failed to encode message", null, null)

        val requestUrl = buildString {
            append(baseUrl)
            append("?client=gtx")
            append("&sl=").append(from.googleCode)
            append("&tl=").append(to.googleCode)
            append("&dt=t")
            append("&q=").append(encodedMessage)
        }

        val connection = try {
            URI.create(requestUrl).toURL().openConnection() as HttpURLConnection
        } catch (exception: Exception) {
            return RequestResult(2, "Invalid Google translation URL: ${exception.message}", null, to)
        }

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 3_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BattleChat/0.1")

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()

            when (status) {
                200 -> processSuccessfulResponse(body, from, to)
                429 -> RequestResult(429, "Google Translate rate limited this endpoint", null, to)
                403 -> RequestResult(403, "Google Translate denied this endpoint", null, to)
                else -> {
                    ChatPlus.LOGGER.debug("Google translation endpoint {} returned {}: {}", baseUrl, status, body.take(250))
                    RequestResult(status, body.ifBlank { "Google translation API error" }, null, to)
                }
            }
        } catch (exception: Exception) {
            ChatPlus.LOGGER.debug("Google translation request to {} failed", baseUrl, exception)
            RequestResult(1, "Connection error: ${exception.message ?: exception.javaClass.simpleName}", null, to)
        } finally {
            connection.disconnect()
        }
    }

    private fun encodeMessage(message: String): String? {
        return try {
            URLEncoder.encode(message, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun processSuccessfulResponse(body: String, requestedSource: Language, targetLanguage: Language): RequestResult {
        return try {
            val gson: Gson = GsonBuilder().setLenient().create()
            val json: JsonArray = gson.fromJson(body, JsonArray::class.java)
            val detectedCode = runCatching { json[2].asString }.getOrNull()
            val detectedSource = detectedCode?.let(LanguageManager::findLanguageFromGoogle)
                ?: requestedSource.takeUnless { it.googleCode == "auto" }
            val translatedText = json[0].asJsonArray
                .joinToString("") { it.asJsonArray[0].asString }

            if (translatedText.isBlank()) {
                RequestResult(500, "Google returned an empty translation", detectedSource, targetLanguage)
            } else {
                RequestResult(200, translatedText, detectedSource, targetLanguage)
            }
        } catch (exception: Exception) {
            RequestResult(500, "Failed to parse Google response: ${exception.message}", null, targetLanguage)
        }
    }
}

data class RequestResult(val code: Int, val message: String, val from: Language?, val to: Language?)
