package dev.shchuko.vet_assistant.bot

import java.text.MessageFormat
import java.util.*

object VetBotUiBundle {
    private val EN_LOCALE: Locale = Locale.ENGLISH
    private val RU_LOCALE: Locale = Locale("ru")

    private val knownLocalesAndBundles = mapOf<Locale, ResourceBundle>(
        EN_LOCALE to ResourceBundle.getBundle("vet_bot_ui", EN_LOCALE),
        RU_LOCALE to ResourceBundle.getBundle("vet_bot_ui", RU_LOCALE)
    )

    private val DEFAULT_LOCALE_BUNDLE = knownLocalesAndBundles.getValue(RU_LOCALE)

    fun getString(key: String, locale: Locale? = null, vararg args: Any?): String {
        val effectiveBundle = knownLocalesAndBundles[locale] ?: DEFAULT_LOCALE_BUNDLE

        return when {
            effectiveBundle.containsKey(key) -> MessageFormat(
                effectiveBundle.getString(key),
                effectiveBundle.locale
            ).format(args)

            DEFAULT_LOCALE_BUNDLE.containsKey(key) -> MessageFormat(
                DEFAULT_LOCALE_BUNDLE.getString(key),
                effectiveBundle.locale
            ).format(args)

            else -> key
        }
    }

    fun getStringInAllLocales(key: String, vararg args: Any?): Set<String> =
        knownLocalesAndBundles.keys.asSequence().map { getString(key, it, args) }.toSet()
}