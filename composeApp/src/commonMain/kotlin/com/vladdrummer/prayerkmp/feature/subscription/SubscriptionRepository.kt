package com.vladdrummer.prayerkmp.feature.subscription

interface SubscriptionRepository {
    fun hasActiveSubscription(email: String): Boolean
}

class StubSubscriptionRepository : SubscriptionRepository {
    override fun hasActiveSubscription(email: String): Boolean = false
}

