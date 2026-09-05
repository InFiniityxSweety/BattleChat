package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Keyless fallback using the public MyMemory translation API. */
class MyMemoryRequester {

    companion object {
        private const val BASE_URL = "https://api.mymemory.translated.net/get"
    }

    fun performTranslationRequest(message: String, from: Language?, to: Language): RequestResult {
        val source = from?.googleCode?.takeUnless { it == "auto" } ?: "autodetect"
        val target = to.googleCode
        val query = URLEncoder.encode(message, StandardCharsets.UTF_8)
        val pair = URLEncoder.encode("$source|$target", StandardCharsets.UTF_8)
        val requestUrl = "$BASE_URL?q=$query&langpair=$pair&mt=1"

        val connection = try {
            URI.create(requestUrl).toURL().openConnection() as HttpURLConnection
        } catch (exception: Exception) {
            return RequestResult(2, "Invalid MyMemory URL: ${exception.message}", null, to)
        }

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 4_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BattleChat/0.1")

            val httpStatus = connection.responseCode
            val stream = if (httpStatus in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (httpStatus !in 200..299) {
                return RequestResult(httpStatus, body.ifBlank { "MyMemory HTTP $httpStatus" }, null, to)
            }

            val root = JsonParser.parseString(body).asJsonObject
            val responseStatus = runCatching { root.get("responseStatus")?.asInt ?: 200 }.getOrDefault(200)
            val responseData = root.getAsJsonObject("responseData")
            val translated = responseData?.get("translatedText")?.asString.orEmpty().trim()

            if (responseStatus !in 200..299) {
                val details = root.get("responseDetails")?.asString ?: "MyMemory API error"
                RequestResult(responseStatus, details, null, to)
            } else if (translated.isBlank()) {
                RequestResult(500, "MyMemory returned an empty translation", null, to)
            } else {
                RequestResult(200, translated, null, to)
            }
        } catch (exception: Exception) {
            ChatPlus.LOGGER.debug("MyMemory translation request failed", exception)
            RequestResult(1, "MyMemory connection error: ${exception.message ?: exception.javaClass.simpleName}", null, to)
        } finally {
            connection.disconnect()
        }
    }
}
