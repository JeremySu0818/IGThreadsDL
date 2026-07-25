package com.jeremysu0818.igthreadsdl.i18n

import android.content.Context
import com.jeremysu0818.igthreadsdl.i18n.locales.*
import java.util.Locale

object LanguageManager {
    const val PREFERENCES_NAME = "app_settings"
    const val KEY_LANGUAGE = "app_language"

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
        val script = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            deviceLocale.script
        } else {
            ""
        }

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
                script.equals("Hant", ignoreCase = true) || country in setOf("TW", "HK", "MO") ->
                    AppLanguage.ZH_TW
                script.equals("Hans", ignoreCase = true) || country in setOf("CN", "SG") ->
                    AppLanguage.ZH_CN
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
        return when (resolved) {
            AppLanguage.AR -> stringsAr
            AppLanguage.CS -> stringsCs
            AppLanguage.DE -> stringsDe
            AppLanguage.EN -> stringsEn
            AppLanguage.ES -> stringsEs
            AppLanguage.FR -> stringsFr
            AppLanguage.HI -> stringsHi
            AppLanguage.HU -> stringsHu
            AppLanguage.ID -> stringsId
            AppLanguage.IT -> stringsIt
            AppLanguage.JA -> stringsJa
            AppLanguage.KO -> stringsKo
            AppLanguage.NL -> stringsNl
            AppLanguage.PL -> stringsPl
            AppLanguage.PT_BR -> stringsPtBr
            AppLanguage.RU -> stringsRu
            AppLanguage.TR -> stringsTr
            AppLanguage.VI -> stringsVi
            AppLanguage.ZH_CN -> stringsZhCn
            AppLanguage.ZH_TW -> stringsZhTw
            AppLanguage.SYSTEM -> stringsEn
        }
    }
}
