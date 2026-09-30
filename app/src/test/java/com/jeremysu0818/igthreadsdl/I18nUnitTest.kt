package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.i18n.AppLanguage
import com.jeremysu0818.igthreadsdl.i18n.AppStrings
import com.jeremysu0818.igthreadsdl.i18n.LanguageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.Before
import org.json.JSONObject
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class I18nUnitTest {

    @Test
    fun testSupportedLanguagesCount() {
        assertEquals(21, AppLanguage.values().size)
        assertEquals(20, AppLanguage.supportedLanguages.size)
        assertTrue(AppLanguage.values().contains(AppLanguage.SYSTEM))
    }

    private fun translationFile(): File =
        File("src/main/assets/strings.json").takeIf { it.exists() }
            ?: File("app/src/main/assets/strings.json")

    @Before
    fun loadCatalog() {
        translationFile().inputStream().use(LanguageManager::loadTranslations)
    }

    @Test
    fun jsonContainsEveryLanguageAndKey() {
        val root = JSONObject(translationFile().readText())
        assertEquals(AppLanguage.supportedLanguages.map { it.code }.toSet(), root.keys().asSequence().toSet())
        val expectedKeys = AppStrings::class.java.declaredFields
            .filter { it.type == String::class.java }.map { it.name }.toSet()
        for (language in AppLanguage.supportedLanguages) {
            assertEquals(language.code, expectedKeys, root.getJSONObject(language.code).keys().asSequence().toSet())
        }
    }

    @Test
    fun missingBlankOrInvalidTranslationFallsBackToEnglish() {
        val root = JSONObject(translationFile().readText())
        root.getJSONObject("ja").remove("navHome")
        root.getJSONObject("ja").put("navSettings", " ")
        root.getJSONObject("ja").put("navQueue", 123)
        root.remove("de")
        root.toString().byteInputStream().use(LanguageManager::loadTranslations)
        val english = LanguageManager.getStrings(AppLanguage.EN)
        val japanese = LanguageManager.getStrings(AppLanguage.JA)
        assertEquals(english.navHome, japanese.navHome)
        assertEquals(english.navSettings, japanese.navSettings)
        assertEquals(english.navQueue, japanese.navQueue)
        assertEquals(english, LanguageManager.getStrings(AppLanguage.DE))
        assertEquals(root.getJSONObject("ja").getString("navHistory"), japanese.navHistory)
    }

    @Test
    fun invalidCatalogDoesNotReplaceLoadedTranslations() {
        val before = LanguageManager.getStrings(AppLanguage.JA)
        assertThrows(org.json.JSONException::class.java) {
            "{}".byteInputStream().use(LanguageManager::loadTranslations)
        }
        assertEquals(before, LanguageManager.getStrings(AppLanguage.JA))
    }

    @Test
    fun explicitSelectionOverridesDeviceAndChineseScriptOverridesRegion() {
        assertEquals(AppLanguage.JA, LanguageManager.resolveAppLanguage(AppLanguage.JA, Locale.forLanguageTag("ar")))
        assertEquals(AppLanguage.ZH_TW, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.forLanguageTag("zh-Hant-CN")))
        assertEquals(AppLanguage.ZH_CN, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.forLanguageTag("zh-Hans-TW")))
        assertEquals(AppLanguage.ZH_TW, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.forLanguageTag("zh-HK")))
        assertEquals(AppLanguage.ZH_CN, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.forLanguageTag("zh-SG")))
        assertEquals(AppLanguage.PT_BR, AppLanguage.fromCode("pt-BR"))
    }

    @Test
    fun testAllLanguagesHaveNonEmptyStringsReflectively() {
        val fields = AppStrings::class.java.declaredFields.filter { it.type == String::class.java }
        assertTrue("AppStrings should have 100+ properties", fields.size >= 100)

        for (lang in AppLanguage.supportedLanguages) {
            val strings = LanguageManager.getStrings(lang)
            for (field in fields) {
                field.isAccessible = true
                val value = field.get(strings) as? String
                assertNotNull("Field ${field.name} in ${lang.code} should not be null", value)
                assertTrue("Field ${field.name} in ${lang.code} should not be blank", value!!.isNotBlank())
            }
        }
    }

    @Test
    fun testFormatPlaceholdersMatchEnglishReference() {
        val fields = AppStrings::class.java.declaredFields.filter { it.type == String::class.java }
        val englishStrings = LanguageManager.getStrings(AppLanguage.EN)

        val specifierRegex = Regex("""%(?:(\d+)\$)?([a-zA-Z])""")

        fun extractSpecifiers(text: String): List<String> {
            return specifierRegex.findAll(text).map { match ->
                val index = match.groupValues[1]
                val type = match.groupValues[2]
                if (index.isNotEmpty()) "$index$$type" else type
            }.toList()
        }

        for (field in fields) {
            field.isAccessible = true
            val enValue = field.get(englishStrings) as String
            val enSpecifiers = extractSpecifiers(enValue)

            if (enSpecifiers.isNotEmpty()) {
                for (lang in AppLanguage.supportedLanguages) {
                    if (lang == AppLanguage.EN) continue
                    val strings = LanguageManager.getStrings(lang)
                    val langValue = field.get(strings) as String
                    val langSpecifiers = extractSpecifiers(langValue)

                    assertEquals(
                        "Placeholders mismatch in field ${field.name} for locale ${lang.code}",
                        enSpecifiers.sorted(),
                        langSpecifiers.sorted()
                    )
                }
            }
        }
    }

    @Test
    fun testXmlLocaleResourceKeysMatchDefault() {
        val baseResDir = if (File("src/main/res").exists()) {
            File("src/main/res")
        } else {
            File("app/src/main/res")
        }
        assertTrue("res dir must exist", baseResDir.exists())

        fun extractKeys(xmlFile: File): Set<String> {
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(xmlFile)
            val nodes = doc.getElementsByTagName("string")
            val keys = mutableSetOf<String>()
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val translatable = node.attributes.getNamedItem("translatable")?.nodeValue
                if (translatable == "false") continue
                val name = node.attributes.getNamedItem("name")?.nodeValue ?: continue
                keys.add(name)
            }
            return keys
        }

        val defaultXml = File(baseResDir, "values/strings.xml")
        assertTrue("default strings.xml must exist", defaultXml.exists())
        val defaultKeys = extractKeys(defaultXml)
        assertTrue("default strings.xml should contain keys", defaultKeys.isNotEmpty())

        val xmlLocaleDirs = AppLanguage.supportedLanguages.map { lang ->
            File(baseResDir, "${lang.resourceLocaleName}/strings.xml")
        }

        for (localeXml in xmlLocaleDirs) {
            assertTrue("Xml file ${localeXml.path} must exist", localeXml.exists())
            val localeKeys = extractKeys(localeXml)
            assertEquals(
                "XML key set mismatch in ${localeXml.parentFile?.name}",
                defaultKeys,
                localeKeys
            )
        }
    }

    @Test
    fun testSystemLocaleResolutionAndFallback() {
        assertEquals(AppLanguage.EN, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.ENGLISH))
        assertEquals(AppLanguage.ZH_TW, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.TRADITIONAL_CHINESE))
        assertEquals(AppLanguage.ZH_CN, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.SIMPLIFIED_CHINESE))
        assertEquals(AppLanguage.JA, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.JAPANESE))
        assertEquals(AppLanguage.KO, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.KOREAN))
        assertEquals(AppLanguage.DE, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.GERMAN))
        assertEquals(AppLanguage.FR, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale.FRENCH))
        assertEquals(AppLanguage.PT_BR, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("pt", "BR")))
        assertEquals(AppLanguage.CS, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("cs", "CZ")))
        assertEquals(AppLanguage.AR, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("ar", "SA")))
        assertEquals(AppLanguage.HI, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("hi", "IN")))
        assertEquals(AppLanguage.HU, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("hu", "HU")))
        assertEquals(AppLanguage.ID, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("id", "ID")))
        assertEquals(AppLanguage.IT, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("it", "IT")))
        assertEquals(AppLanguage.NL, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("nl", "NL")))
        assertEquals(AppLanguage.PL, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("pl", "PL")))
        assertEquals(AppLanguage.RU, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("ru", "RU")))
        assertEquals(AppLanguage.TR, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("tr", "TR")))
        assertEquals(AppLanguage.VI, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("vi", "VN")))

        // Test Unsupported locale fallbacks to English
        assertEquals(AppLanguage.EN, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("fi", "FI")))
        assertEquals(AppLanguage.EN, LanguageManager.resolveAppLanguage(AppLanguage.SYSTEM, Locale("sw", "TZ")))
    }
}
