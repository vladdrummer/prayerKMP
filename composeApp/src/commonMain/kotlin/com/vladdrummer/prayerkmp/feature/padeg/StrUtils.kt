package com.vladdrummer.prayerkmp.feature.padeg

internal object StrUtils {
    data class Border(val left: Int, val right: Int)

    fun wordPosition(n: Int, s: String, wordDelims: String): Int {
        var count = 0
        var i = 0
        var result = -1

        while (i < s.length && count < n) {
            while (i < s.length && wordDelims.indexOf(s[i]) >= 0) i++
            if (i < s.length) count++

            if (count < n) {
                while (i < s.length && wordDelims.indexOf(s[i]) < 0) i++
            } else {
                result = i
            }
        }

        return result
    }

    fun extractWord(n: Int, s: String, wordDelims: String): String {
        val i = wordPosition(n, s, wordDelims)
        if (i < 0) return ""
        var j = i
        while (j < s.length && wordDelims.indexOf(s[j]) < 0) j++
        return s.substring(i, j)
    }

    fun wordCount(s: String, wordDelims: String): Int {
        var result = 0
        var i = 0
        val len = s.length

        while (i < len) {
            while (i < len && wordDelims.indexOf(s[i]) >= 0) i++
            if (i < len) result++
            while (i < len && wordDelims.indexOf(s[i]) < 0) i++
        }
        return result
    }

    fun countWords(s: String, wordDelims: String, max: Int = 0): Array<Border> {
        val result = mutableListOf<Border>()
        var i = 0
        val len = s.length

        while (i < len) {
            while (i < len && wordDelims.indexOf(s[i]) >= 0) i++
            if (i < len) {
                val left = i
                i++
                while (i < len && wordDelims.indexOf(s[i]) < 0) i++
                result += Border(left, i)
                if (max > 0 && result.size >= max) break
            }
        }
        return result.toTypedArray()
    }

    fun properCase(s: String, wordDelims: String): String {
        val result = s.lowercase()
        val ca = result.toCharArray()
        var i = 0
        val len = result.length

        while (i < len) {
            while (i < len && wordDelims.indexOf(result[i]) >= 0) i++
            if (i < len) ca[i] = ca[i].uppercaseChar()
            while (i < len && wordDelims.indexOf(result[i]) < 0) i++
        }
        return ca.concatToString()
    }
}
