package com.jeremysu0818.igthreadsdl.i18n

enum class AppLanguage(
    val code: String,
    val resourceLocaleName: String,
    val nativeName: String,
) {
    SYSTEM("system", "", "System"),
    AR("ar", "values-ar", "العربية"),
    CS("cs", "values-cs", "Čeština"),
    DE("de", "values-de", "Deutsch"),
    EN("en", "values", "English"),
    ES("es", "values-es", "Español"),
    FR("fr", "values-fr", "Français"),
    HI("hi", "values-hi", "हिन्दी"),
    HU("hu", "values-hu", "Magyar"),
    ID("id", "values-id", "Bahasa Indonesia"),
    IT("it", "values-it", "Italiano"),
    JA("ja", "values-ja", "日本語"),
    KO("ko", "values-ko", "한국어"),
    NL("nl", "values-nl", "Nederlands"),
    PL("pl", "values-pl", "Polski"),
    PT_BR("pt-br", "values-pt-rBR", "Português (Brasil)"),
    RU("ru", "values-ru", "Русский"),
    TR("tr", "values-tr", "Türkçe"),
    VI("vi", "values-vi", "Tiếng Việt"),
    ZH_CN("zh-cn", "values-zh-rCN", "简体中文"),
    ZH_TW("zh-tw", "values-zh-rTW", "繁體中文");

    val endonym: String get() = nativeName

    companion object {
        val supportedLanguages: List<AppLanguage>
            get() = values().filter { it != SYSTEM }

        fun fromCode(code: String?): AppLanguage =
            values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
    }
}
