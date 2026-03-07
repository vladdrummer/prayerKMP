package com.vladdrummer.prayerkmp.feature.padeg

internal class TExceptionDic(private var fileName: String = EXCEPTION_DIC_FILE_NAME) {
    private var exceptionMap: Map<String, List<String>> = emptyMap()

    fun readExceptionStrings(lines: Iterator<String>) {
        val exceptions = mutableMapOf<String, MutableList<String>>()
        var strings: MutableList<String>? = null

        while (lines.hasNext()) {
            var row = lines.next().trim()
            if (row.isEmpty() || row[0] == ';') continue

            if (row.first() == '[' && row.last() == ']') {
                strings?.sort()
                val section = row.substring(1, row.length - 1)
                strings = if (section.isNotEmpty()) mutableListOf<String>().also { exceptions[section] = it } else null
                continue
            }

            strings?.add(StrUtils.extractWord(1, row.lowercase(), "\t;").replace(" ", ""))
        }

        strings?.sort()
        exceptionMap = exceptions
    }

    fun present(anyWord: String, section: String): Boolean {
        val list = exceptionMap[section] ?: return false
        val word = anyWord.lowercase()
        if (list.binarySearch(word) >= 0) return true

        if (
            section.equals("LastName", true) ||
            section.equals("LastNameW", true) ||
            section.equals("DependedLastNameW", true) ||
            section.equals("FirstNameM", true) ||
            section.equals("FirstNameW", true) ||
            section.equals("FirstPartLastName", true) ||
            section.equals("Accent", true)
        ) {
            val masks = mutableListOf<String>()
            for (item in list) {
                if (item.startsWith('*')) masks += item.substring(1) else break
            }
            for (mask in masks) {
                val start = (word.length - mask.length).coerceAtLeast(0)
                val tail = word.substring(start)
                if (tail == mask || mask == "*") return true
            }
        }

        return false
    }

    fun accentInfo(anyWord: String): Int {
        val word = anyWord.lowercase()
        val vocalicIndexes = mutableListOf<Int>()
        for (i in word.indices) if (FioComm.VOCALIC.indexOf(word[i]) >= 0) vocalicIndexes += i
        if (vocalicIndexes.isEmpty()) return 0

        for (i in vocalicIndexes.indices) {
            val idx = vocalicIndexes[i]
            val candidate = buildString {
                append(word.substring(0, idx))
                append('"')
                append(word.substring(idx))
            }
            if (present(candidate, "Accent")) {
                return if (i == vocalicIndexes.lastIndex) 1 else 2
            }
        }
        return 0
    }

    fun getRightPart(leftPart: String, section: String): String {
        val left = leftPart.lowercase()
        val list = exceptionMap[section] ?: return left
        for (line in list) {
            val p = line.indexOf('=')
            if (p <= 0) continue
            if (left == line.substring(0, p).trim()) return line.substring(p + 1).trim()
        }
        return left
    }

    fun updateExceptions(): Boolean = exceptionMap.isNotEmpty()

    fun getFileName(): String = fileName

    fun setFileName(fileName: String) {
        this.fileName = fileName
    }

    companion object {
        const val EXCEPTION_DIC_FILE_NAME = "Except.dic"
        val exceptionDic = TExceptionDic()
    }
}
