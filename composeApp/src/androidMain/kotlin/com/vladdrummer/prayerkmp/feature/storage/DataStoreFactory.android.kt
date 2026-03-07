package com.vladdrummer.prayerkmp.feature.storage

import android.content.Context

internal fun createDataStore(context: Context) = createDataStore(
    producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
)
