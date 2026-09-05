package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import java.util.concurrent.ConcurrentHashMap

/**
 * BattleChat translation dispatcher.
 *
 * Providers are tried in order. A failure only cools down the provider that failed,
 * instead of disabling translation globally. This keeps Ctrl+Click and outgoing
 * translation usable when one public endpoint is rate limited or unavailable.
 */
object TranslationManager {

    private data class Provider(
        val id: String,
        val request: (String, Language?, Language) -> RequestResult
    )

    private val cooldownUntil = ConcurrentHashMap<String, Long>()

    private val providers: List<Provider> by lazy {
        listOf(
            Provider("google") { text, from, to ->
                val requester = GoogleRequester()
                if (from == null || from.googleCode == "auto") requester.translateAuto(text, to)
                else requester.performTranslationRequest(text, from, to)
            },
            Provider("libretranslate-cutie") { text, from, to ->
                LibreTranslateRequester("https://translate.cutie.dating").performTranslationRequest(text, from, to)
            },
            Provider("libretranslate-fedilab") { text, from, to ->
                LibreTranslateRequester("https://translate.fedilab.app").performTranslationRequest(text, from, to)
            }
        )
    }

    fun translate(text: String, from: Language?, to: Language): TranslateResult? {
        if (to.googleCode == "auto") {
            ChatPlus.LOGGER.warn("Refusing translation request with Auto Detect as target language")
            return null
        }

        val now = System.currentTimeMillis()
        val failures = mutableListOf<String>()

        for (provider in providers) {
            val cooldown = cooldownUntil[provider.id] ?: 0L
            if (cooldown > now) {
                ChatPlus.LOGGER.debug(
                    "Skipping translation provider {} for another {} ms",
                    provider.id,
                    cooldown - now
                )
                continue
            }

            val result = try {
                provider.request(text, from, to)
            } catch (throwable: Throwable) {
                ChatPlus.LOGGER.warn("Translation provider {} failed unexpectedly", provider.id, throwable)
                RequestResult(1, throwable.message ?: "Unexpected provider error", null, to)
            }

            // The translated text is the important part. Some free providers return
            // valid translations but omit/rename the detected source language. Do not
            // throw away a good translation just because source-language metadata is missing.
            if (result.code == 200 && result.message.isNotBlank()) {
                cooldownUntil.remove(provider.id)
                ChatPlus.LOGGER.debug("Translation succeeded through provider {}", provider.id)
                return TranslateResult(result.message.trim(), result.from)
            }

            failures += "${provider.id}:${result.code}"
            val cooldownMillis = cooldownFor(result.code)
            if (cooldownMillis > 0) {
                cooldownUntil[provider.id] = System.currentTimeMillis() + cooldownMillis
            }

            ChatPlus.LOGGER.warn(
                "Translation provider {} failed with code {}: {}",
                provider.id,
                result.code,
                result.message.take(250)
            )
        }

        ChatPlus.LOGGER.error("All translation providers failed: {}", failures.joinToString(", "))
        return null
    }

    private fun cooldownFor(code: Int): Long = when (code) {
        429, 403 -> 5 * 60_000L
        in 500..599 -> 30_000L
        1 -> 15_000L
        else -> 10_000L
    }
}
