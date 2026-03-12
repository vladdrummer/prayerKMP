package com.vladdrummer.prayerkmp.feature.subscription


enum class SubscriptionSourceType {
    Backend,
    GooglePlay,
    AppStore,
}

sealed interface SubscriptionSourceResult {
    val source: SubscriptionSourceType

    data class Active(
        override val source: SubscriptionSourceType,
        val expirationAt: String? = null,
    ) : SubscriptionSourceResult

    data class Inactive(
        override val source: SubscriptionSourceType,
        val expirationAt: String? = null,
    ) : SubscriptionSourceResult

    data class Unavailable(
        override val source: SubscriptionSourceType,
        val reason: String? = null,
    ) : SubscriptionSourceResult
}

interface SubscriptionSourceChecker {
    val sourceType: SubscriptionSourceType
    suspend fun check(email: String): SubscriptionSourceResult
}

data class AggregatedSubscriptionStatus(
    val email: String,
    val hasActiveSubscription: Boolean,
    val source: SubscriptionSourceType?,
    val expirationAt: String?,
    val checkedAtEpochMillis: Long,
    val resolved: Boolean,
    val checks: List<SubscriptionSourceResult>,
)

class SubscriptionAggregator(
    private val sources: List<SubscriptionSourceChecker>,
) {
    suspend fun resolve(email: String): AggregatedSubscriptionStatus {
        if (email.isBlank()) {
            return AggregatedSubscriptionStatus(
                email = email,
                hasActiveSubscription = false,
                source = null,
                expirationAt = null,
                checkedAtEpochMillis = 0L,
                resolved = true,
                checks = emptyList()
            )
        }

        val checks = sources.map { it.check(email) }
        val active = checks.firstOrNull { it is SubscriptionSourceResult.Active } as SubscriptionSourceResult.Active?
        val hasResolvedValue = checks.any {
            it is SubscriptionSourceResult.Active || it is SubscriptionSourceResult.Inactive
        }

        return AggregatedSubscriptionStatus(
            email = email,
            hasActiveSubscription = active != null,
            source = active?.source,
            expirationAt = active?.expirationAt,
            checkedAtEpochMillis = 0L,
            resolved = hasResolvedValue,
            checks = checks
        )
    }
}
