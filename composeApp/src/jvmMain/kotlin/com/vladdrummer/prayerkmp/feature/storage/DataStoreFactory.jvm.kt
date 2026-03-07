package com.vladdrummer.prayerkmp.feature.storage

import java.io.File

internal fun createDataStore() = createDataStore(
    producePath = {
        val baseDir = File(System.getProperty("user.home"), ".prayerkmp")
        baseDir.mkdirs()
        File(baseDir, dataStoreFileName).absolutePath
    }
)
