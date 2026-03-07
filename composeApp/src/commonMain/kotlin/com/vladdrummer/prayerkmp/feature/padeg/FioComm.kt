package com.vladdrummer.prayerkmp.feature.padeg

internal object FioComm {
    const val VOCALIC = "аоуыэяеёюи"
    const val CONSONANT = "бвгджзйклмнпрстфхцчшщ"
    const val SOFT_SIBILANT = "чщ"
    const val HARD_SIBILANT = "цш"
    const val GKH = "гкх"
    const val WORD_DELIMS = " \t"
    const val PROPER_DELIMS = " -.,'\""
    const val exInvalidCase = -1
    const val exInvalidSex = -2
    const val ACCENT_LAST = 1

    private data class TErrorRec(val code: Int, val ident: String)

    private val declenErrorMap = arrayOf(
        TErrorRec(-1, "Недопустимое значение падежа"),
        TErrorRec(-2, "Недопустимое значение рода"),
    )
    private val rusEnds2 = listOf("ов", "ев", "ёв", "ин", "ая", "яя")
    private val rusEnds3 = listOf("ова", "ева", "ёва", "ина", "ска", "цка")

    fun createDeclenError(errorCode: Int, invalidParam: String): EDeclenError {
        val i = -errorCode - 1
        val rec = declenErrorMap.getOrNull(i)
        return if (rec != null) {
            EDeclenError("${rec.ident} ($invalidParam)").also { it.errorCode = errorCode }
        } else {
            EDeclenError("Ошибка ($invalidParam)").also { it.errorCode = errorCode }
        }
    }

    fun isRangeInt(beg: Int, end: Int, value: Int): Boolean = value in beg..end

    fun separateFIO(value: String, fio: FIO) {
        val trimmed = value.trim()
        val b = StrUtils.countWords(trimmed, WORD_DELIMS, 4)
        fio.lastName = if (b.size < 1) "" else trimmed.substring(b[0].left, b[0].right)
        fio.firstName = if (b.size < 2) "" else trimmed.substring(b[1].left, b[1].right)
        fio.middleName = if (b.size < 3) "" else trimmed.substring(b[2].left, b[2].right)
        val test = if (b.size < 4) "" else trimmed.substring(b[3].left, b[3].right)
        val s = test.lowercase()
        if (s == "оглы" || s == "кызы") fio.middleName = "${fio.middleName} $test"

        val pointPos = fio.firstName.indexOf('.')
        if (pointPos > 0) {
            val len = fio.firstName.length
            if (pointPos != len) {
                fio.middleName = fio.firstName.substring(pointPos + 1)
                fio.firstName = fio.firstName.substring(0, pointPos + 1)
            }
        }
    }

    private fun processCurrSyll(i: Int, currSyll: String): Long {
        val sonicVal = Syllable.strToSonic(currSyll).toLongOrNull() ?: 0L
        return sonicVal * i * i * i
    }

    private fun process(s: String, extraSyll: Boolean): Long {
        if (s.isEmpty()) return 0L
        var sonicSum = 0L
        var tmpS = Syllable.divideOnSyllable(s)
        var p = tmpS.indexOf('-')
        var i = 1
        while (p > 0) {
            val currSyll = tmpS.substring(0, p)
            tmpS = tmpS.substring(p + 1)
            sonicSum += processCurrSyll(i, currSyll)
            i++
            p = tmpS.indexOf('-')
        }
        if (!extraSyll) sonicSum += processCurrSyll(i, tmpS)
        return sonicSum
    }

    private fun isPadeg(s: String): Boolean {
        val l = s.length
        val end = if (l > 1) s.substring(l - 2) else ""
        return "аяуюе".indexOf(s.last()) >= 0 || end == "ом" || end == "ем"
    }

    fun chinaName(cf: String, ci: String, co: String): Boolean {
        val len = cf.length
        var end = ""
        if (len > 2) {
            end = cf.substring(len - 2)
            if (end in rusEnds2) return false
        }
        if (len > 3) {
            end = cf.substring(len - 3)
            if (end in rusEnds3) return false
        }

        val l1 = Syllable.countSyllable(cf)
        val l2 = Syllable.countSyllable(ci)
        val l3 = Syllable.countSyllable(co)
        if ((l1 == 0 && cf.isNotEmpty()) || l2 == 0 || (l3 == 0 && co.isNotEmpty())) return false

        val extraSyll = if (co.isNotEmpty()) isPadeg(co) else if (ci.isNotEmpty()) isPadeg(ci) else false
        var totalSyllable = l1 + l2 + l3
        if (extraSyll) totalSyllable--
        if (totalSyllable > 6) return false

        val fio = FIO(cf, ci, co)
        separateFIO("$cf $ci $co", fio)
        if (fio.middleName.isEmpty()) {
            val syllStr = Syllable.divideOnSyllable(fio.firstName)
            val p = syllStr.indexOf('-')
            if (p > 0) {
                fio.middleName = fio.firstName.substring(p + 1)
                fio.firstName = fio.firstName.substring(0, p)
            }
        }

        val sonicVal = process(fio.lastName, extraSyll) + process(fio.firstName, extraSyll) + process(fio.middleName, extraSyll)
        return sonicVal <= 4300L
    }

    fun getCutFIO(fio: String): String {
        val trimmed = fio.trim()
        val result = StringBuilder(trimmed)
        if (trimmed.isNotEmpty()) {
            val borders = StrUtils.countWords(trimmed, " .")
            if (borders.size > 1) {
                result.setLength(borders[0].right + 1)
                for (i in 1..2) {
                    if (i < borders.size) result.append(trimmed[borders[i].left]).append('.')
                }
            }
        }
        return result.toString()
    }

    fun getSex(middleName: String): Char {
        val len = middleName.length - 1
        var result = '\u0000'
        if (len > 0) {
            val s = middleName.lowercase()
            if (s[len] == 'ч' || s[len - 1] == 'ч' || s.endsWith("чем") || s.endsWith("оглы")) {
                result = 'м'
            } else if ((("аыеуй".indexOf(s[len]) >= 0 && "но".indexOf(s[len - 1]) >= 0 && CONSONANT.indexOf(s[len - 2]) >= 0) || s.endsWith("кызы"))) {
                result = 'ж'
            }
        }
        return result
    }

    fun proper(str: String): String {
        var result = str.lowercase()
        if (result == "фон" || result == "де") return result
        val ca = result.toCharArray()
        var i = 0
        while (i < ca.size) {
            while (i < ca.size && PROPER_DELIMS.indexOf(ca[i]) >= 0) i++
            if (i < ca.size) ca[i] = ca[i].uppercaseChar()
            while (i < ca.size && PROPER_DELIMS.indexOf(ca[i]) < 0) i++
        }
        result = ca.concatToString()
        return result
    }

    fun proper(str: StringBuilder): StringBuilder {
        for (i in 0 until str.length) str.set(i, str[i].lowercaseChar())
        val test = str.toString()
        if (test == "фон" || test == "де") return str
        var i = 0
        while (i < str.length) {
            while (i < str.length && PROPER_DELIMS.indexOf(str[i]) >= 0) i++
            if (i < str.length) str.set(i, str[i].uppercaseChar())
            while (i < str.length && PROPER_DELIMS.indexOf(str[i]) < 0) i++
        }
        return str
    }
}
