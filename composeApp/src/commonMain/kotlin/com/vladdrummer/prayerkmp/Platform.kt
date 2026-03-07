package com.vladdrummer.prayerkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
