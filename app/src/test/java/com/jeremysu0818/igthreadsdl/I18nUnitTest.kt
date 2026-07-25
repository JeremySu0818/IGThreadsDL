package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.i18n.AppLanguage
import com.jeremysu0818.igthreadsdl.i18n.AppStrings
import com.jeremysu0818.igthreadsdl.i18n.LanguageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
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

    @Test
    fun testKotlinLocaleFilenamesExactMatch() {
        val actualDir = if (File("src/main/java/com/jeremysu0818/igthreadsdl/i18n/locales").exists()) {
            File("src/main/java/com/jeremysu0818/igthreadsdl/i18n/locales")
        } else {
            File("app/src/main/java/com/jeremysu0818/igthreadsdl/i18n/locales")
        }

        assertTrue("Locale directory must exist", actualDir.exists())
        val files = actualDir.listFiles { _, name -> name.endsWith(".kt") } ?: emptyArray()
        val actualNames = files.map { it.name }.toSet()
        val expectedNames = setOf(
            "Ar.kt", "Cs.kt", "De.kt", "En.kt", "Es.kt",
            "Fr.kt", "Hi.kt", "Hu.kt", "Id.kt", "It.kt",
            "Ja.kt", "Ko.kt", "Nl.kt", "Pl.kt", "PtBr.kt",
            "Ru.kt", "Tr.kt", "Vi.kt", "ZhCn.kt", "ZhTw.kt"
        )

        assertEquals("Kotlin locale filenames must match exact expected 20 files", expectedNames, actualNames)
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
