package dev.shchuko.vet_assistant.bot

import java.text.MessageFormat
import java.util.*

object MessagesResourceBundle {
    val EN_LOCALE: Locale = Locale.ENGLISH
    val RU_LOCALE: Locale = Locale("ru")

    private val EN: ResourceBundle = ResourceBundle.getBundle("messages", EN_LOCALE)
    private val RU: ResourceBundle = ResourceBundle.getBundle("messages", RU_LOCALE)

    private val DEFAULT_LOCALE_BUNDLE = EN

    val DEFAULT_LOCALE = DEFAULT_LOCALE_BUNDLE.locale

    fun getString(key: String, locale: Locale = DEFAULT_LOCALE, vararg args: Any?): String {
        val (pattern, effectiveLocale) = when (locale) {
            RU_LOCALE -> RU.getWithLocaleOrFallback(key)
            EN_LOCALE -> EN.getWithLocaleOrFallback(key)
            else -> DEFAULT_LOCALE_BUNDLE.getWithLocaleOrFallback(key)
        }
        return MessageFormat(pattern, effectiveLocale).format(args)
    }

    private fun ResourceBundle.getWithLocaleOrFallback(key: String): Pair<String, Locale> {
        val pattern = this.getStringOrDefault(key, null)
        if (pattern != null) {
            return pattern to this.locale
        }
        return DEFAULT_LOCALE_BUNDLE.getStringOrDefault(key, key)!! to DEFAULT_LOCALE
    }

    private fun ResourceBundle.getStringOrDefault(key: String, default: String?) = try {
        getString(key)
    } catch (_: MissingResourceException) {
        default
    }


}