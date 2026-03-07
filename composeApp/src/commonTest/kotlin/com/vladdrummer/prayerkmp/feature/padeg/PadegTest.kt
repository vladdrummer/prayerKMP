package com.vladdrummer.prayerkmp.feature.padeg

import kotlin.test.Test
import kotlin.test.assertEquals

class PadegTest {
    @Test
    fun maleFio_genitive_case2() {
        val result = Padeg.getFIOPadeg(
            lastName = "Петров",
            firstName = "Иван",
            middleName = "Иванович",
            sex = true,
            padeg = 2,
        )

        assertEquals("Петрова Ивана Ивановича", result)
    }
}
