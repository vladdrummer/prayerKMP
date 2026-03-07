package com.vladdrummer.prayerkmp.feature.padeg

class EDeclenError(message: String) : RuntimeException(message) {
    var errorCode: Int = 0
}
