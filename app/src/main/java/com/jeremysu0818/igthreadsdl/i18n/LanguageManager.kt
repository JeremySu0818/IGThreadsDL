package com.jeremysu0818.igthreadsdl.i18n

import android.content.Context
import java.io.InputStream
import org.json.JSONObject
import java.util.Locale

object LanguageManager {
    const val PREFERENCES_NAME = "app_settings"
    const val KEY_LANGUAGE = "app_language"

    @Volatile
    private var catalog: Map<AppLanguage, AppStrings> = emptyMap()

    /** Load the bundled JSON before any UI, resolver, or service requests text. */
    fun initialize(context: Context) {
        context.assets.open("strings.json").use(::loadTranslations)
    }

    internal fun loadTranslations(input: InputStream) {
        val root = JSONObject(input.bufferedReader(Charsets.UTF_8).readText())
        val english = root.getJSONObject(AppLanguage.EN.code)
        // Publish only after every language has been decoded, so readers never see
        // a partially loaded catalog. Missing translated keys fall back to English.
        catalog = AppLanguage.supportedLanguages.associateWith { language ->
            AppStrings.fromJson(root.optJSONObject(language.code) ?: english, english)
        }
    }

    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.code)
        return AppLanguage.fromCode(code)
    }

    fun saveLanguage(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun resolveAppLanguage(
        selected: AppLanguage,
        deviceLocale: Locale = Locale.getDefault(),
    ): AppLanguage {
        if (selected != AppLanguage.SYSTEM) return selected

        val lang = deviceLocale.language.lowercase(Locale.US)
        val country = deviceLocale.country.uppercase(Locale.US)
        val script = deviceLocale.script

        return when (lang) {
            "ar" -> AppLanguage.AR
            "cs" -> AppLanguage.CS
            "de" -> AppLanguage.DE
            "en" -> AppLanguage.EN
            "es" -> AppLanguage.ES
            "fr" -> AppLanguage.FR
            "hi" -> AppLanguage.HI
            "hu" -> AppLanguage.HU
            "id", "in" -> AppLanguage.ID
            "it" -> AppLanguage.IT
            "ja" -> AppLanguage.JA
            "ko" -> AppLanguage.KO
            "nl" -> AppLanguage.NL
            "pl" -> AppLanguage.PL
            "pt" -> if (country == "BR") AppLanguage.PT_BR else AppLanguage.EN
            "ru" -> AppLanguage.RU
            "tr" -> AppLanguage.TR
            "vi" -> AppLanguage.VI
            "zh" -> when {
                script.equals("Hant", ignoreCase = true) ->
                    AppLanguage.ZH_TW
                script.equals("Hans", ignoreCase = true) ->
                    AppLanguage.ZH_CN
                country in setOf("CN", "SG") -> AppLanguage.ZH_CN
                else -> AppLanguage.ZH_TW
            }
            else -> AppLanguage.EN
        }
    }

    fun getStrings(
        selected: AppLanguage,
        deviceLocale: Locale = Locale.getDefault(),
    ): AppStrings {
        val resolved = resolveAppLanguage(selected, deviceLocale)
        return checkNotNull(catalog[resolved]) {
            "LanguageManager.initialize must be called before requesting translations"
        }
    }
}
