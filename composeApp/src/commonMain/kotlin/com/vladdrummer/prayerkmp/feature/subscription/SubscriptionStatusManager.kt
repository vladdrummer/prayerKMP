package com.vladdrummer.prayerkmp.feature.subscription

import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val email: String = "",
    val hasActiveSubscription: Boolean = false,
    val source: SubscriptionSourceType? = null,
    val checkedAtEpochMillis: Long = 0L,
    val isLoading: Boolean = false,
)

class SubscriptionStatusManager(
    private val storage: AppStorage,
    private val scope: CoroutineScope,
    private val aggregator: SubscriptionAggregator = defaultSubscriptionAggregator(),
) {
    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    fun start(email: String) {
        scope.launch {
            if (email.isBlank()) {
                _state.value = SubscriptionUiState()
                return@launch
            }
            val cachedEmail = storage.stringFlow(AppStorageKeys.PremiumSubscriptionEmail, "").first()
            val cachedActive = storage.booleanFlow(AppStorageKeys.PremiumSubscriptionActive, false).first()
            val checkedAt = storage.stringFlow(AppStorageKeys.PremiumSubscriptionCheckedAt, "0").first().toLongOrNull() ?: 0L
            val sourceRaw = storage.stringFlow(AppStorageKeys.PremiumSubscriptionSource, "").first()
            val source = sourceRaw.toSourceTypeOrNull()

            _state.value = SubscriptionUiState(
                email = email,
                hasActiveSubscription = cachedActive && cachedEmail.equals(email, ignoreCase = true),
                source = source,
                checkedAtEpochMillis = checkedAt,
                isLoading = true
            )

            refresh(email = email, force = false)
        }
    }

    fun refresh(email: String, force: Boolean) {
        scope.launch {
            if (email.isBlank()) {
                _state.value = SubscriptionUiState()
                return@launch
            }

            val current = _state.value

            _state.value = current.copy(
                email = email,
                isLoading = true
            )

            val aggregated = aggregator.resolve(email)
            if (aggregated.resolved) {
                storage.setString(AppStorageKeys.PremiumSubscriptionEmail, email)
                storage.setBoolean(AppStorageKeys.PremiumSubscriptionActive, aggregated.hasActiveSubscription)
                storage.setString(AppStorageKeys.PremiumSubscriptionSource, aggregated.source?.name.orEmpty())
                storage.setString(AppStorageKeys.PremiumSubscriptionCheckedAt, aggregated.checkedAtEpochMillis.toString())

                _state.value = SubscriptionUiState(
                    email = email,
                    hasActiveSubscription = aggregated.hasActiveSubscription,
                    source = aggregated.source,
                    checkedAtEpochMillis = aggregated.checkedAtEpochMillis,
                    isLoading = false
                )
            } else {
                _state.value = _state.value.copy(
                    email = email,
                    isLoading = false
                )
            }
        }
    }

    private fun String.toSourceTypeOrNull(): SubscriptionSourceType? {
        return SubscriptionSourceType.entries.firstOrNull { it.name == this }
    }

}

fun defaultSubscriptionAggregator(): SubscriptionAggregator {
    return SubscriptionAggregator(
        sources = listOf(
            BackendSubscriptionSource(),
            GooglePlaySubscriptionSourceStub(),
            AppStoreSubscriptionSourceStub(),
        )
    )
}
