package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Lightweight client for public Lingva Translate instances.
 *
 * Lingva exposes a keyless REST API and acts as an independent translation
 * fallback when Google's direct endpoints are rate limited for the player's IP.
 */
class LingvaRequester(private val baseUrl: String) {

    fun performTranslationRequest(message: String, from: Language?, to: Language): RequestResult {
        val source = from?.googleCode?.takeUnless { it == "auto" } ?: "auto"
        val target = to.googleCode
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20")
        val requestUrl = "${baseUrl.trimEnd('/')}/api/v1/$source/$target/$encoded"

        val connection = try {
            URI.create(requestUrl).toURL().openConnection() as HttpURLConnection
        } catch (exception: Exception) {
            return RequestResult(2, "Invalid Lingva URL: ${exception.message}", null, to)
        }

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 4_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BattleChat/0.1")

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                return RequestResult(status, body.ifBlank { "Lingva HTTP $status" }, null, to)
            }

            val json = JsonParser.parseString(body).asJsonObject
            val translated = json.get("translation")?.asString.orEmpty().trim()
            if (translated.isBlank()) {
                val error = json.get("error")?.asString ?: "Lingva returned an empty translation"
                RequestResult(500, error, null, to)
            } else {
                RequestResult(200, translated, null, to)
            }
        } catch (exception: Exception) {
            ChatPlus.LOGGER.debug("Lingva request failed for {}", baseUrl, exception)
            RequestResult(1, "Lingva connection error: ${exception.message ?: exception.javaClass.simpleName}", null, to)
        } finally {
            connection.disconnect()
        }
    }
}
