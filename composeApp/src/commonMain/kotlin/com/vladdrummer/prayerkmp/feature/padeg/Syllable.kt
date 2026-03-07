package com.vladdrummer.prayerkmp.feature.padeg

internal class Syllable {
    private var i = 0
    private var j = 0
    private var nSyllable = 0
    private val outStr = StringBuilder()
    private var vocalicExist = false
    private var inString = ""
    private lateinit var lowChars: CharArray

    private fun setCut() {
        if (nSyllable > 0 && vocalicExist && outStr.last() != '-') {
            outStr.append('-')
            nSyllable--
            i++
            vocalicExist = false
        }
    }

    private fun copyChar() {
        if (j < inString.length) {
            outStr.append(inString[j])
            if (VOCALIC.indexOf(lowChars[j]) >= 0) vocalicExist = true
            j++
            if (j < lowChars.size && "ъь".indexOf(lowChars[j]) >= 0) {
                outStr.append(inString[j])
                j++
            }
        }
    }

    fun divideOnSyllableInt(anyWord: String): String {
        val separator = anyWord.indexOf('-')
        if (separator >= 0) {
            return divideOnSyllableInt(anyWord.substring(0, separator)) +
                "--" +
                divideOnSyllableInt(anyWord.substring(separator + 1))
        }

        nSyllable = countSyllable(anyWord)
        if (nSyllable <= 1) return anyWord
        nSyllable--

        vocalicExist = false
        inString = anyWord
        lowChars = anyWord.lowercase().toCharArray()
        val sonicChars = strToSonic(anyWord).toCharArray()
        i = 0
        j = 0

        do {
            if (sonicChars[i] < sonicChars[i + 1]) {
                copyChar()
                i++
            } else if (sonicChars[i] == sonicChars[i + 1]) {
                when (sonicChars[i]) {
                    '1' -> {
                        if (i == 0) {
                            copyChar()
                            i++
                        } else if (lowChars[j] == lowChars[j + 1]) {
                            copyChar()
                            setCut()
                        } else if (vocalicExist) {
                            setCut()
                            copyChar()
                        } else {
                            copyChar()
                            i++
                        }
                    }

                    '2' -> {
                        if (sonicChars[i + 1] < sonicChars[i + 2]) {
                            copyChar()
                            setCut()
                        } else {
                            i++
                        }
                    }

                    '3' -> {
                        copyChar()
                        setCut()
                    }
                }
            } else if (sonicChars[i] > sonicChars[i + 1]) {
                copyChar()
                if (lowChars[j] == 'й') {
                    copyChar()
                    setCut()
                    i++
                } else if (sonicChars[i + 1] > sonicChars[i + 2]) {
                    i++
                } else if (sonicChars[i + 1] < sonicChars[i + 2]) {
                    if (
                        lowChars[j + 1] == 'ь' &&
                        j > 2 &&
                        VOCALIC.indexOf(lowChars[j + 2]) < 0 &&
                        sonicChars[i + 1] != '1'
                    ) {
                        copyChar()
                        setCut()
                        i++
                    } else if (vocalicExist) {
                        setCut()
                    } else {
                        i++
                    }
                } else {
                    i++
                }
            }

            if (j < lowChars.size && !isLegalChar(lowChars[j])) {
                outStr.append(inString[j])
                j++
            }
        } while (nSyllable > 0 && j < lowChars.size && i + 2 < sonicChars.size)

        if (j < inString.length) outStr.append(inString.substring(j))
        return outStr.toString()
    }

    companion object {
        private const val VOCALIC = "аоуыэяеёюи"
        private const val LEGAL_CHAR = "абвгдежзийклмнопрстуфхцчшщъыьэюя"

        private fun sonic(index: Char): Char = "31111311321222312113111111^3^333"[index.code - 'а'.code]

        fun countSyllable(anyWord: String): Int {
            var result = 0
            val lower = anyWord.lowercase()
            for (ch in lower) if (VOCALIC.indexOf(ch) >= 0) result++
            return result
        }

        private fun isLegalChar(ch: Char): Boolean = ch in LEGAL_CHAR.first()..LEGAL_CHAR.last()

        fun strToSonic(value: String): String {
            val result = StringBuilder()
            val lower = value.lowercase().replace("ё", "е")
            for (ch in lower) {
                if (isLegalChar(ch)) {
                    val test = sonic(ch)
                    if (test != '^') result.append(test)
                }
            }
            return result.toString()
        }

        fun divideOnSyllable(anyWord: String): String = Syllable().divideOnSyllableInt(anyWord)
    }
}
