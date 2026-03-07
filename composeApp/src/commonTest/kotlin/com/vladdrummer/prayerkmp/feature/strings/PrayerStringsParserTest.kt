package com.vladdrummer.prayerkmp.feature.strings

import kotlin.test.Test
import kotlin.test.assertEquals

class PrayerStringsParserTest {
    @Test
    fun parseAndroidStringsXml_extractsRegularAndCdataValues() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="simple">Hello &amp; Bye</string>
                <string name="rich"><![CDATA[<h1>Title</h1>]]></string>
                <string name="line_break">first\nsecond</string>
            </resources>
        """.trimIndent()

        val parsed = parseAndroidStringsXml(xml)

        assertEquals("Hello & Bye", parsed["simple"])
        assertEquals("<h1>Title</h1>", parsed["rich"])
        assertEquals("first\nsecond", parsed["line_break"])
    }
}
