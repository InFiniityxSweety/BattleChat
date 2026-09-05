package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import java.util.concurrent.ConcurrentHashMap

/**
 * BattleChat translation dispatcher.
 *
 * Providers are isolated from each other, transient failures are retried once,
 * cooldowns are deliberately short, and successful translations are cached.
 */
object TranslationManager {

    private data class Provider(
        val id: String,
        val request: (String, Language?, Language) -> RequestResult
    )

    private data class CacheKey(val text: String, val from: String, val to: String)
    private data class CacheEntry(val result: TranslateResult, val expiresAt: Long)

    private val cooldownUntil = ConcurrentHashMap<String, Long>()
    private val cache = ConcurrentHashMap<CacheKey, CacheEntry>()

    @Volatile
    var lastFailureSummary: String = ""
        private set

    private val providers: List<Provider> by lazy {
        listOf(
            Provider("google-api") { text, from, to ->
                val requester = GoogleRequester(GoogleRequester.DEFAULT_BASE_URL)
                if (from == null || from.googleCode == "auto") requester.translateAuto(text, to)
                else requester.performTranslationRequest(text, from, to)
            },
            Provider("google-web") { text, from, to ->
                val requester = GoogleRequester(GoogleRequester.FALLBACK_BASE_URL)
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
        return translateInternal(text, from, to, allowCooldownReset = true)
    }

    private fun translateInternal(text: String, from: Language?, to: Language, allowCooldownReset: Boolean): TranslateResult? {
        if (to.googleCode == "auto") {
            lastFailureSummary = "invalid target: auto"
            ChatPlus.LOGGER.warn("Refusing translation request with Auto Detect as target language")
            return null
        }

        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) {
            lastFailureSummary = "empty input"
            return null
        }

        val key = CacheKey(normalizedText, from?.googleCode ?: "auto", to.googleCode)
        val now = System.currentTimeMillis()
        cache[key]?.let { cached ->
            if (cached.expiresAt > now) {
                lastFailureSummary = ""
                return cached.result
            }
            cache.remove(key)
        }

        cooldownUntil.entries.removeIf { it.value <= now }
        val failures = mutableListOf<String>()
        var attempted = 0

        for (provider in providers) {
            val cooldown = cooldownUntil[provider.id] ?: 0L
            if (cooldown > now) {
                failures += "${provider.id}:cooldown"
                continue
            }

            attempted++
            var result = safeRequest(provider, normalizedText, from, to)
            if (shouldRetry(result.code)) {
                try {
                    Thread.sleep(150)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                result = safeRequest(provider, normalizedText, from, to)
            }

            if (result.code == 200 && result.message.isNotBlank()) {
                cooldownUntil.remove(provider.id)
                lastFailureSummary = ""
                val translated = TranslateResult(result.message.trim(), result.from)
                if (cache.size >= 256) {
                    cache.entries.removeIf { it.value.expiresAt <= now }
                    if (cache.size >= 256) cache.clear()
                }
                cache[key] = CacheEntry(translated, now + 15 * 60_000L)
                ChatPlus.LOGGER.debug("Translation succeeded through provider {}", provider.id)
                return translated
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

        if (attempted == 0 && allowCooldownReset) {
            ChatPlus.LOGGER.info("All translation providers were cooling down; resetting transient cooldowns and retrying now")
            cooldownUntil.clear()
            return translateInternal(normalizedText, from, to, allowCooldownReset = false)
        }

        lastFailureSummary = failures.joinToString(", ")
        ChatPlus.LOGGER.error("All translation providers failed: {}", lastFailureSummary)
        return null
    }

    private fun safeRequest(provider: Provider, text: String, from: Language?, to: Language): RequestResult {
        return try {
            provider.request(text, from, to)
        } catch (throwable: Throwable) {
            ChatPlus.LOGGER.warn("Translation provider {} failed unexpectedly", provider.id, throwable)
            RequestResult(1, throwable.message ?: "Unexpected provider error", null, to)
        }
    }

    private fun shouldRetry(code: Int): Boolean = code == 1 || code in 500..599

    private fun cooldownFor(code: Int): Long = when (code) {
        429 -> 20_000L
        403 -> 45_000L
        in 500..599 -> 5_000L
        1 -> 3_000L
        else -> 5_000L
    }
}
