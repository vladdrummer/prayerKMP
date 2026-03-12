package com.vladdrummer.prayerkmp.feature.subscription

class HasPremiumSubscriptionUseCase(
    private val repository: SubscriptionRepository,
) {
    operator fun invoke(email: String): Boolean {
        if (email.isBlank()) return false
        return repository.hasActiveSubscription(email)
    }
}

