package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Lightweight LibreTranslate client used as a fallback provider. */
class LibreTranslateRequester(private val baseUrl: String) {

    private val gson = GsonBuilder().create()
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(4))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun performTranslationRequest(message: String, from: Language?, to: Language): RequestResult {
        val source = from?.googleCode ?: "auto"
        val payload = JsonObject().apply {
            addProperty("q", message)
            addProperty("source", source)
            addProperty("target", to.googleCode)
            addProperty("format", "text")
        }

        val request = try {
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl.trimEnd('/') + "/translate"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build()
        } catch (exception: Exception) {
            return RequestResult(2, "Invalid LibreTranslate endpoint: ${exception.message}", null, to)
        }

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                return RequestResult(response.statusCode(), extractError(response.body()), null, to)
            }

            val json = gson.fromJson(response.body(), JsonObject::class.java)
            val translatedText = json.get("translatedText")?.asString.orEmpty()
            val detectedCode = json.getAsJsonObject("detectedLanguage")
                ?.get("language")
                ?.asString
                ?: source.takeUnless { it == "auto" }

            val detectedLanguage = detectedCode?.let(LanguageManager::findLanguageFromGoogle)
            if (translatedText.isBlank()) {
                RequestResult(500, "LibreTranslate returned an empty translation", detectedLanguage, to)
            } else if (detectedLanguage == null) {
                RequestResult(500, "LibreTranslate did not return a supported source language", null, to)
            } else {
                RequestResult(200, translatedText, detectedLanguage, to)
            }
        } catch (exception: Exception) {
            ChatPlus.LOGGER.debug("LibreTranslate request to {} failed", baseUrl, exception)
            RequestResult(1, "Connection error: ${exception.message ?: exception.javaClass.simpleName}", null, to)
        }
    }

    private fun extractError(body: String): String {
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            json.get("error")?.asString ?: body.take(250)
        } catch (_: Exception) {
            body.take(250)
        }
    }
}
