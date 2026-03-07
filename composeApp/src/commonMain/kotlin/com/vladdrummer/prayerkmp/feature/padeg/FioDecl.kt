package com.vladdrummer.prayerkmp.feature.padeg

internal class FioDecl {
    private val ends = Array(7) { "" }

    private fun setEndings(c1: String, c2: String, c3: String, c4: String, c5: String, c6: String) {
        ends[1] = c1
        ends[2] = c2
        ends[3] = c3
        ends[4] = c4
        ends[5] = c5
        ends[6] = c6
    }

    private fun clearEndings() = setEndings("", "", "", "", "", "")

    private fun leaveWithoutDeclension(lastName: String, firstName: String, middleName: String): String {
        val result = StringBuilder()
        when {
            firstName.isEmpty() -> result.append(lastName).append(' ').append(middleName)
            firstName.last() == '.' -> result.append(lastName).append(' ').append(firstName).append(middleName)
            else -> result.append(lastName).append(' ').append(firstName).append(' ').append(middleName)
        }
        return FioComm.proper(result.toString()).trim()
    }

    private fun addSpaceAfterPoint(s: String): String {
        var result = s.trim()
        if (result.isNotEmpty()) {
            val i = result.indexOf('.')
            if (i >= 0) result = result.substring(0, i).trim() + ". " + addSpaceAfterPoint(result.substring(i + 1))
        }
        return result
    }

    fun nonDeclension(anyWord: String, male: Boolean, isLastName: Boolean, multiple: Boolean): Boolean {
        if (anyWord.isEmpty()) return true
        val len = anyWord.length
        val last = anyWord.last()
        if (FioComm.VOCALIC.indexOf(last) >= 0 && "аяиы".indexOf(last) < 0) return true

        if (isLastName) {
            if (len <= 2) return true
            if (multiple) {
                if (
                    anyWord.equals("Бут", true) ||
                    anyWord.equals("Тер", true) ||
                    anyWord.equals("Аскер", true) ||
                    anyWord.equals("Кара", true) ||
                    anyWord.equals("Бонч", true) ||
                    anyWord.equals("Куй", true)
                ) return true
                if (TExceptionDic.exceptionDic.present(anyWord, "FirstPartLastName")) return true
            }

            val cEnd = anyWord.takeLast(2)
            if (cEnd == "иа" || cEnd == "ия") return true

            if (male) {
                if ((last == 'и' && anyWord.substring(len - 3, len - 1) != "ск") || (last == 'ы' && anyWord[len - 2] != 'н')) return true
                if ((cEnd == "их" || cEnd == "ых") && !TExceptionDic.exceptionDic.present(anyWord, "BaseNonRussian")) return true
            } else {
                if (FioComm.CONSONANT.indexOf(last) >= 0 || "ьиы".indexOf(last) >= 0) return true
                if (TExceptionDic.exceptionDic.present(anyWord, "LastNameW")) return true
            }

            if (TExceptionDic.exceptionDic.present(anyWord, "LastName")) return true
        } else {
            if ("иы".indexOf(last) >= 0) return true
            if (male) {
                if (TExceptionDic.exceptionDic.present(anyWord, "FirstNameM")) return true
            } else {
                if (FioComm.CONSONANT.indexOf(last) >= 0) return true
                if (TExceptionDic.exceptionDic.present(anyWord, "FirstNameW")) return true
            }
        }

        return false
    }

    fun getFirstNameM(firstNameParam: String, padeg: Int): String {
        if (firstNameParam.isEmpty()) return ""
        var firstName = FioComm.proper(firstNameParam)
        var result = firstName
        if (firstName.equals("Пётр", true) && padeg >= 2) firstName = firstName.replace('ё', 'е')

        var len = firstName.length
        if (padeg > 1 && firstName.last() != '.') {
            if (firstName.last() == 'о') {
                firstName = FioComm.proper(TExceptionDic.exceptionDic.getRightPart(firstName, "FirstNameParallelForms"))
                len = firstName.length
            }

            if (nonDeclension(firstName, true, false, false)) return result
            val last = firstName.last()
            clearEndings()

            if (firstName.equals("Лев", true)) {
                firstName = "Льв"
                setEndings("", "а", "у", "а", "ом", "е")
            } else if (firstName.equals("Павел", true)) {
                firstName = "Павл"
                setEndings("", "а", "у", "а", "ом", "е")
            } else {
                val prev = if (len > 1) firstName[len - 2] else ' '
                if (last != 'й' && last != 'ь') {
                    when {
                        last == 'а' && FioComm.CONSONANT.indexOf(prev) >= 0 -> {
                            setEndings("а", "ы", "е", "у", "ой", "е")
                            if (FioComm.GKH.indexOf(prev) >= 0 || FioComm.SOFT_SIBILANT.indexOf(prev) >= 0 || "жш".indexOf(prev) >= 0) ends[2] = "и"
                            if (FioComm.HARD_SIBILANT.indexOf(prev) >= 0 || FioComm.SOFT_SIBILANT.indexOf(prev) >= 0 || prev == 'ж') ends[5] = "ей"
                            if (len > 2) firstName = firstName.dropLast(1)
                        }

                        last == 'я' && (FioComm.CONSONANT.indexOf(prev) >= 0 || prev == 'ь') -> {
                            setEndings("я", "и", "е", "ю", "ей", "е")
                            if (len > 2) firstName = firstName.dropLast(1)
                        }

                        last == 'о' -> {}
                        FioComm.CONSONANT.indexOf(last) >= 0 -> setEndings("", "а", "у", "а", "ом", "е")
                    }
                } else {
                    setEndings(last.toString(), "я", "ю", "я", "ем", "е")
                    if (prev == 'и') ends[6] = "и"
                    firstName = firstName.dropLast(1)
                }
                if (FioComm.HARD_SIBILANT.indexOf(last) >= 0 || FioComm.SOFT_SIBILANT.indexOf(last) >= 0 || last == 'ж') ends[5] = "ем"
            }

            result = firstName + ends[padeg]
        }
        return result
    }

    private fun getFirstNameW(firstNameParam: String, padeg: Int): String {
        if (firstNameParam.isEmpty()) return ""
        var firstName = FioComm.proper(firstNameParam)
        var result = firstName
        val len = firstName.length
        if (padeg > 1 && firstName.last() != '.') {
            if (nonDeclension(firstName, false, false, false)) return firstName
            clearEndings()
            val last = firstName.last()
            val prev = if (len > 1) firstName[len - 2] else ' '
            when (last) {
                'а' -> {
                    setEndings("а", "ы", "е", "у", "ой", "е")
                    if (FioComm.GKH.indexOf(prev) >= 0 || FioComm.SOFT_SIBILANT.indexOf(prev) >= 0 || "жш".indexOf(prev) >= 0) ends[2] = "и"
                    if (FioComm.HARD_SIBILANT.indexOf(prev) >= 0 || FioComm.SOFT_SIBILANT.indexOf(prev) >= 0 || prev == 'ж') ends[5] = "ей"
                    firstName = firstName.dropLast(1)
                }

                'я' -> {
                    setEndings("я", "и", "е", "ю", "ей", "е")
                    if (prev == 'и' && TExceptionDic.exceptionDic.accentInfo(firstName) != FioComm.ACCENT_LAST) {
                        ends[3] = "и"
                        ends[6] = "и"
                    }
                    firstName = firstName.dropLast(1)
                }

                'ь' -> {
                    setEndings("ь", "и", "и", "ь", "ью", "и")
                    firstName = firstName.dropLast(1)
                }
            }
            result = firstName + ends[padeg]
        }
        return result
    }

    private fun getMiddleNameM(middleNameParam: String, padeg: Int): String {
        if (middleNameParam.isEmpty()) return ""
        val middleName = FioComm.proper(middleNameParam)
        if (padeg <= 1 || middleName.last() == '.') return middleName
        return if (FioComm.getSex(middleName) == 'м') {
            setEndings("", "а", "у", "а", "ем", "е")
            if (middleName.last() != 'ч') clearEndings()
            middleName + ends[padeg]
        } else {
            getFirstNameM(middleName, padeg)
        }
    }

    private fun getMiddleNameW(middleNameParam: String, padeg: Int): String {
        if (middleNameParam.isEmpty()) return ""
        var middleName = FioComm.proper(middleNameParam)
        if (padeg <= 1 || middleName.last() == '.') return middleName
        return if (FioComm.getSex(middleName) == 'ж') {
            setEndings("а", "ы", "е", "у", "ой", "е")
            if (middleName.last() == 'а') middleName = middleName.dropLast(1) else clearEndings()
            middleName + ends[padeg]
        } else {
            getFirstNameW(middleName, padeg)
        }
    }

    private fun getLastNameM(lastNameParam: String, padeg: Int, multiple: Boolean): String {
        if (lastNameParam.isEmpty()) return ""
        val delimPos = lastNameParam.indexOf('-')
        if (delimPos >= 0) {
            val fstPart = lastNameParam.substring(0, delimPos).trim()
            val endPart = lastNameParam.substring(delimPos + 1).trim()
            return if (StrUtils.wordCount(lastNameParam, "-") == 2 && endPart.lowercase().contains("оглы")) {
                "${StrUtils.properCase(fstPart, " ")}-$endPart"
            } else {
                "${getLastNameM(fstPart, padeg, true)}-${getLastNameM(endPart, padeg, false)}"
            }
        }

        var lastName = FioComm.proper(lastNameParam)
        var result = lastName
        var len = lastName.length
        clearEndings()

        if (padeg > 1) {
            if (nonDeclension(lastName, true, true, multiple)) return lastName
            var cEnd3 = if (len >= 3) lastName.takeLast(3) else lastName
            if (cEnd3 == "ски") {
                lastName += 'й'
                len++
            }
            if ((if (len >= 2) lastName.takeLast(2) else lastName) == "ны") {
                lastName += 'й'
                len++
            }
            val cEnd2 = if (len >= 2) lastName.takeLast(2) else lastName

            when (cEnd2) {
                "ый" -> {
                    setEndings("", "ого", "ому", "ого", "ым", "ом")
                    lastName = lastName.dropLast(2)
                }
                "ой" -> {
                    setEndings("", "ого", "ому", "ого", "ым", "ом")
                    val prev = lastName[len - 3]
                    if (Syllable.countSyllable(lastName) != 1 && prev != 'р') {
                        if (FioComm.GKH.indexOf(prev) >= 0 || FioComm.SOFT_SIBILANT.indexOf(prev) >= 0) ends[5] = "им"
                        lastName = lastName.dropLast(2)
                    } else {
                        setEndings("й", "я", "ю", "я", "ем", "е")
                        lastName = lastName.dropLast(1)
                    }
                }
                "ий", "ей", "ай", "яй", "уй" -> {
                    val last = lastName[len - 3]
                    if (cEnd2 == "ий" && FioComm.GKH.indexOf(last) >= 0) {
                        setEndings(cEnd2, "ого", "ому", "ого", "им", "ом")
                        lastName = lastName.dropLast(2)
                    } else if (cEnd2 == "ий" && (FioComm.HARD_SIBILANT.indexOf(last) >= 0 || FioComm.SOFT_SIBILANT.indexOf(last) >= 0 || "нж".indexOf(last) >= 0)) {
                        setEndings(cEnd2, "его", "ему", "его", "им", "ем")
                        lastName = lastName.dropLast(2)
                    } else {
                        setEndings("й", "я", "ю", "я", "ем", "е")
                        lastName = lastName.dropLast(1)
                    }
                }
                "ын", "ин", "ев", "ёв", "ов", "ич", "ач" -> {
                    setEndings("", "а", "у", "а", "ым", "е")
                    if (lastName.equals("Лев", true)) lastName = "Льв"
                    if (Syllable.countSyllable(lastName) == 1) ends[5] = "ом"
                    if (lastName.equals("Львов", true)) ends[5] = "ым"
                    if (TExceptionDic.exceptionDic.present(lastName, "BaseNonRussian")) ends[5] = "ом"
                }
                else -> {
                    val last = lastName.last()
                    val prev = if (lastName.length > 1) lastName[lastName.length - 2] else ' '
                    when {
                        cEnd2 == "ок" -> setEndings("", "а", "у", "а", "ом", "е")
                        cEnd2 == "ец" -> setEndings("", "а", "у", "а", "ем", "е")
                        last == 'а' && FioComm.CONSONANT.indexOf(prev) >= 0 -> {
                            setEndings("а", "ы", "е", "у", "ой", "е")
                            if (FioComm.GKH.indexOf(prev) >= 0 || FioComm.SOFT_SIBILANT.indexOf(prev) >= 0 || "жш".indexOf(prev) >= 0) ends[2] = "и"
                            if ((FioComm.SOFT_SIBILANT.indexOf(prev) >= 0 || FioComm.HARD_SIBILANT.indexOf(prev) >= 0 || prev == 'ж') &&
                                TExceptionDic.exceptionDic.accentInfo(lastName) != FioComm.ACCENT_LAST
                            ) ends[5] = "ей"
                            lastName = lastName.dropLast(1)
                        }
                        last == 'я' && (FioComm.CONSONANT.indexOf(prev) >= 0 || prev == 'ь') -> {
                            setEndings("я", "и", "е", "ю", "ей", "е")
                            lastName = lastName.dropLast(1)
                        }
                        last == 'ь' -> {
                            setEndings("", "я", "ю", "я", "ем", "е")
                            lastName = lastName.dropLast(1)
                        }
                        FioComm.CONSONANT.indexOf(last) >= 0 -> {
                            setEndings("", "а", "у", "а", "ом", "е")
                            if (FioComm.HARD_SIBILANT.indexOf(last) >= 0) ends[5] = "ем"
                        }
                    }
                }
            }

            result = lastName + ends[padeg]
        }
        return result
    }

    private fun getLastNameW(lastNameParam: String, padeg: Int, multiple: Boolean): String {
        if (lastNameParam.isEmpty()) return ""
        val delimPos = lastNameParam.indexOf('-')
        if (delimPos >= 0) {
            val fstPart = lastNameParam.substring(0, delimPos).trim()
            val endPart = lastNameParam.substring(delimPos + 1).trim()
            return if (StrUtils.wordCount(lastNameParam, "-") == 2 && endPart.lowercase().contains("кызы")) {
                "${StrUtils.properCase(fstPart, " ")}-$endPart"
            } else {
                "${getLastNameW(fstPart, padeg, true)}-${getLastNameW(endPart, padeg, false)}"
            }
        }

        var lastName = FioComm.proper(lastNameParam)
        var result = lastName
        val len = lastName.length
        clearEndings()
        if (padeg > 1) {
            if (nonDeclension(lastName, false, true, multiple)) return lastName
            val cEnd4 = if (len > 3) lastName.takeLast(4) else lastName
            if (cEnd4 == "цына") {
                setEndings("а", "ой", "ой", "у", "ой", "ой")
                lastName = lastName.dropLast(1)
            } else {
                val cEnd2 = if (len > 1) lastName.takeLast(2) else lastName
                when (cEnd2) {
                    "ая" -> {
                        setEndings("ая", "ой", "ой", "ую", "ой", "ой")
                        val prev = lastName[len - 3]
                        if (FioComm.HARD_SIBILANT.indexOf(prev) >= 0 || FioComm.SOFT_SIBILANT.indexOf(prev) >= 0 || prev == 'ж') {
                            setEndings("ая", "ей", "ей", "ую", "ей", "ей")
                        }
                        lastName = lastName.dropLast(2)
                    }
                    "яя" -> {
                        setEndings("яя", "ей", "ей", "юю", "ей", "ей")
                        lastName = lastName.dropLast(2)
                    }
                    else -> {
                        val last = lastName.last()
                        val prev = if (lastName.length > 1) lastName[lastName.length - 2] else ' '
                        when {
                            cEnd2 == "ова" || cEnd2 == "ева" || cEnd2 == "ёва" || cEnd2 == "ина" -> {
                                setEndings("а", "ой", "ой", "у", "ой", "ой")
                                lastName = lastName.dropLast(1)
                            }
                            last == 'а' && FioComm.CONSONANT.indexOf(prev) >= 0 -> {
                                setEndings("а", "ы", "е", "у", "ой", "е")
                                if (FioComm.GKH.indexOf(prev) >= 0 || FioComm.SOFT_SIBILANT.indexOf(prev) >= 0 || "жш".indexOf(prev) >= 0) ends[2] = "и"
                                if ((FioComm.HARD_SIBILANT.indexOf(prev) >= 0 || FioComm.SOFT_SIBILANT.indexOf(prev) >= 0 || prev == 'ж') &&
                                    TExceptionDic.exceptionDic.accentInfo(lastName) != FioComm.ACCENT_LAST
                                ) ends[5] = "ей"
                                lastName = lastName.dropLast(1)
                            }
                            last == 'я' && (FioComm.CONSONANT.indexOf(prev) >= 0 || prev == 'ь') -> {
                                setEndings("я", "и", "е", "ю", "ей", "е")
                                lastName = lastName.dropLast(1)
                            }
                        }
                    }
                }
            }
            result = lastName + ends[padeg]
        }
        return result
    }

    fun getFIO(lastNameInput: String?, firstNameInput: String?, middleNameInput: String?, sexInput: Char, padeg: Int): String {
        var lastName = lastNameInput?.trim().orEmpty()
        var firstName = firstNameInput?.trim().orEmpty()
        var middleName = middleNameInput?.trim().orEmpty()
        var sex = sexInput
        var flagPoint = false

        if (!FioComm.isRangeInt(1, 6, padeg)) throw FioComm.createDeclenError(FioComm.exInvalidCase, padeg.toString())

        var midLen = middleName.length
        if (midLen > 0 && middleName.last() == '.') {
            val tmp = middleName.dropLast(1)
            if (FioComm.getSex(tmp) != '\u0000' || FioComm.chinaName(lastName, firstName, tmp)) {
                middleName = tmp
                midLen--
                flagPoint = true
            }
        }

        if (sex == '\u0000') {
            if (midLen <= 0 || middleName.last() == '.') return leaveWithoutDeclension(lastName, firstName, middleName)
            sex = FioComm.getSex(middleName).let { if (it != '\u0000') it else 'м' }
        }

        sex = sex.lowercaseChar()
        val declType: Char = if (midLen > 0) {
            if (middleName.last() != '.') FioComm.getSex(middleName) else sex
        } else sex

        val normalizedDeclType = if (declType == '\u0000') {
            if (FioComm.chinaName(lastName, firstName, middleName)) 'c' else sex
        } else declType

        val result = StringBuilder()
        when (sex) {
            'ж' -> {
                if (normalizedDeclType == 'ж') {
                    result.append(getLastNameW(lastName, padeg, false)).append(' ').append(getFirstNameW(firstName, padeg))
                    if (midLen > 0) {
                        if (result.last() != '.' || middleName.last() != '.') result.append(' ')
                        result.append(getMiddleNameW(middleName, padeg))
                    }
                } else {
                    result.append(leaveWithoutDeclension(lastName, firstName, middleName))
                }
            }
            'м' -> {
                when (normalizedDeclType) {
                    'c' -> {
                        val low = middleName.lowercase()
                        if (low.isNotEmpty() && ("бвгджзйклмнпрстфхцчшщь".indexOf(low.last()) >= 0)) {
                            FioComm.proper(result.append(lastName).append(' ').append(firstName)).append(' ').append(getFirstNameM(middleName, padeg))
                        } else {
                            result.append(leaveWithoutDeclension(lastName, firstName, middleName))
                        }
                    }
                    'ж' -> result.append(leaveWithoutDeclension(lastName, firstName, middleName))
                    'м' -> {
                        result.append(getLastNameM(lastName, padeg, false)).append(' ').append(getFirstNameM(firstName, padeg))
                        if (midLen > 0) {
                            if (result.last() != '.' || middleName.last() != '.') result.append(' ')
                            result.append(getMiddleNameM(middleName, padeg))
                        }
                    }
                    else -> result.append(leaveWithoutDeclension(lastName, firstName, middleName))
                }
            }
            else -> throw FioComm.createDeclenError(FioComm.exInvalidSex, "'$sex'")
        }

        return if (flagPoint) result.toString().trim() + '.' else result.toString().trim()
    }

    fun getFIOFromStr(cFIO: String, sex: Char, padeg: Int): String {
        val fio = FIO("", "", "")
        FioComm.separateFIO(cFIO, fio)
        if (fio.middleName.isEmpty() && FioComm.chinaName(fio.lastName, fio.firstName, fio.middleName)) {
            fio.middleName = fio.firstName
            fio.firstName = fio.lastName
            fio.lastName = ""
        }
        return getFIO(fio.lastName, fio.firstName, fio.middleName, sex, padeg)
    }

    fun getIF(firstNameInput: String, lastNameInput: String, sexInput: Char, padeg: Int): String {
        val lastName = lastNameInput.trim()
        var firstName = firstNameInput.trim()
        var sex = sexInput
        if (!FioComm.isRangeInt(1, 6, padeg)) throw FioComm.createDeclenError(FioComm.exInvalidCase, padeg.toString())

        sex = sex.lowercaseChar()
        if (firstName.contains('.')) firstName = addSpaceAfterPoint(firstName).trim()

        val borders = StrUtils.countWords(firstName, " \t")
        val result = StringBuilder()

        when (sex) {
            'м' -> {
                for (b in borders) {
                    val someName = firstName.substring(b.left, b.right)
                    result.append(if (someName.last() == '.') FioComm.proper(someName) else getFirstNameM(someName, padeg))
                }
                result.clear().append(result.toString().trim()).append(' ').append(getLastNameM(lastName, padeg, false))
            }
            'ж' -> {
                for (b in borders) {
                    val someName = firstName.substring(b.left, b.right)
                    result.append(if (someName.last() == '.') FioComm.proper(someName) else getFirstNameW(someName, padeg))
                }
                result.clear().append(result.toString().trim()).append(' ').append(getLastNameW(lastName, padeg, false))
            }
            '\u0000' -> result.append(FioComm.proper("$firstName $lastName"))
            else -> throw FioComm.createDeclenError(FioComm.exInvalidSex, "'$sex'")
        }

        return result.toString().trim()
    }

    fun getIFFromStr(cifInput: String, sex: Char, padeg: Int): String {
        val cif = cifInput.trim()
        val n = StrUtils.wordCount(cif, " \t.")
        val (ci, cf) = if (n > 1) {
            var i = cif.length - 1
            while (i > 0) {
                val ch = cif[i]
                if (ch == '.' || " \t".indexOf(ch) >= 0) break
                i--
            }
            cif.substring(0, i) to cif.substring(i + 1)
        } else {
            "" to cif
        }
        return getIF(ci, cf, sex, padeg)
    }
}
