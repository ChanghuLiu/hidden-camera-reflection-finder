package com.hidden.camera.reflection.finder

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_FRENCH = "fr"
    const val LANGUAGE_HINDI = "hi"
    const val LANGUAGE_PORTUGUESE_BRAZIL = "pt-BR"
    const val LANGUAGE_INDONESIAN = "id"
    const val LANGUAGE_SPANISH = "es"
    const val LANGUAGE_TURKISH = "tr"
    const val LANGUAGE_GERMAN = "de"
    const val LANGUAGE_JAPANESE = "ja"
    const val LANGUAGE_CHINESE_SIMPLIFIED = "zh-CN"
    const val LANGUAGE_CHINESE_TRADITIONAL = "zh-TW"

    private const val PREFS_NAME = "language_preferences"
    private const val KEY_LANGUAGE = "selected_language"

    private val supportedLanguageTags = setOf(
        LANGUAGE_ENGLISH,
        LANGUAGE_FRENCH,
        LANGUAGE_HINDI,
        LANGUAGE_PORTUGUESE_BRAZIL,
        LANGUAGE_INDONESIAN,
        LANGUAGE_SPANISH,
        LANGUAGE_TURKISH,
        LANGUAGE_GERMAN,
        LANGUAGE_JAPANESE,
        LANGUAGE_CHINESE_SIMPLIFIED,
        LANGUAGE_CHINESE_TRADITIONAL
    )

    fun getSavedLanguage(context: Context): String {
        val savedLanguage = prefs(context).getString(KEY_LANGUAGE, null) ?: return LANGUAGE_SYSTEM
        return savedLanguage.takeIf { isValidChoice(it) } ?: LANGUAGE_ENGLISH
    }

    fun saveLanguage(context: Context, languageTag: String) {
        prefs(context).edit().putString(
            KEY_LANGUAGE,
            languageTag.takeIf { isValidChoice(it) } ?: LANGUAGE_ENGLISH
        ).apply()
    }

    fun applyPersistedLanguage(context: Context) {
        applyLanguage(context, getSavedLanguage(context))
    }

    fun applyLanguage(context: Context, languageTag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager.applicationLocales = if (languageTag == LANGUAGE_SYSTEM) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(resolveLanguageTag(languageTag))
            }
        }
    }

    fun wrap(context: Context): ContextWrapper {
        val languageTag = getSavedLanguage(context)
        val resolvedTag = if (languageTag == LANGUAGE_SYSTEM) {
            resolveSystemLanguageTag(context)
        } else {
            resolveLanguageTag(languageTag)
        }
        return ContextWrapper(updateResources(context, Locale.forLanguageTag(resolvedTag)))
    }

    private fun resolveLanguageTag(languageTag: String): String =
        languageTag.takeIf { it in supportedLanguageTags } ?: LANGUAGE_ENGLISH

    private fun resolveSystemLanguageTag(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return when (locale.language) {
            "en" -> LANGUAGE_ENGLISH
            "fr" -> LANGUAGE_FRENCH
            "hi" -> LANGUAGE_HINDI
            "pt" -> if (locale.country.equals("BR", ignoreCase = true)) {
                LANGUAGE_PORTUGUESE_BRAZIL
            } else {
                LANGUAGE_ENGLISH
            }
            "id", "in" -> LANGUAGE_INDONESIAN
            "es" -> LANGUAGE_SPANISH
            "tr" -> LANGUAGE_TURKISH
            "de" -> LANGUAGE_GERMAN
            "ja" -> LANGUAGE_JAPANESE
            "zh" -> if (locale.script.equals("Hant", ignoreCase = true) ||
                locale.country.equals("TW", ignoreCase = true) ||
                locale.country.equals("HK", ignoreCase = true) ||
                locale.country.equals("MO", ignoreCase = true)
            ) {
                LANGUAGE_CHINESE_TRADITIONAL
            } else {
                LANGUAGE_CHINESE_SIMPLIFIED
            }
            else -> LANGUAGE_ENGLISH
        }
    }

    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        return context.createConfigurationContext(configuration)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun isValidChoice(languageTag: String): Boolean =
        languageTag == LANGUAGE_SYSTEM || languageTag in supportedLanguageTags
}
