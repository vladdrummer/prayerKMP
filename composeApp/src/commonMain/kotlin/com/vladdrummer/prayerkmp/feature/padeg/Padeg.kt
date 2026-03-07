package com.vladdrummer.prayerkmp.feature.padeg

object Padeg {
    private var currentEncoding: String = "Cp1251"
    private val fioDecl = FioDecl()

    fun getCurrentEncoding(): String = currentEncoding

    fun setCurrentEncoding(currentEncoding: String) {
        this.currentEncoding = currentEncoding
    }

    private fun sexFromBoolean(sex: Boolean): Char = if (sex) 'м' else 'ж'

    fun getFIOPadeg(lastName: String, firstName: String, middleName: String, sex: Boolean, padeg: Int): String =
        fioDecl.getFIO(lastName, firstName, middleName, sexFromBoolean(sex), padeg)

    fun getFIOPadegAS(lastName: String, firstName: String, middleName: String, padeg: Int): String =
        fioDecl.getFIO(lastName, firstName, middleName, '\u0000', padeg)

    fun getCutFIOPadeg(lastName: String, firstName: String, middleName: String, sex: Boolean, padeg: Int): String =
        FioComm.getCutFIO(getFIOPadeg(lastName, firstName, middleName, sex, padeg))

    fun getFIOPadegFS(fio: String, sex: Boolean, padeg: Int): String =
        fioDecl.getFIOFromStr(fio, sexFromBoolean(sex), padeg)

    fun getFIOPadegFSAS(fio: String, padeg: Int): String = fioDecl.getFIOFromStr(fio, '\u0000', padeg)

    fun getCutFIOPadegFS(fio: String, sex: Boolean, padeg: Int): String = FioComm.getCutFIO(getFIOPadegFS(fio, sex, padeg))

    fun getIFPadeg(firstName: String, lastName: String, sex: Boolean, padeg: Int): String =
        fioDecl.getIF(firstName, lastName, sexFromBoolean(sex), padeg)

    fun getIFPadegFS(cIF: String, sex: Boolean, padeg: Int): String = fioDecl.getIFFromStr(cIF, sexFromBoolean(sex), padeg)

    fun updateExceptions(): Boolean = TExceptionDic.exceptionDic.updateExceptions()

    fun setDictionary(dicName: String): Boolean {
        TExceptionDic.exceptionDic.setFileName(dicName)
        return TExceptionDic.exceptionDic.updateExceptions()
    }

    fun getExceptionsFileName(): String = TExceptionDic.exceptionDic.getFileName()

    fun getSex(middleName: String): Int = when (FioComm.getSex(middleName)) {
        'ж' -> 0
        'м' -> 1
        else -> -1
    }

    fun getFioParts(cFIO: String, fio: FIO) {
        FioComm.separateFIO(cFIO, fio)
    }
}
