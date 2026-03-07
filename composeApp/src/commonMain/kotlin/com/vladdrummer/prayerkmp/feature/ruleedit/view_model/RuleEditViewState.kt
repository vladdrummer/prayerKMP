package com.vladdrummer.prayerkmp.feature.ruleedit.view_model

enum class RuleType {
    Morning,
    Evening,
}

data class RulePartUi(
    val index: Int,
    val title: String,
    val enabled: Boolean,
    val html: String,
)

data class AdditionalPrayerUi(
    val resId: String,
    val title: String,
)

data class RuleEditViewState(
    val isLoading: Boolean = false,
    val selectedRule: RuleType = RuleType.Morning,
    val parts: List<RulePartUi> = emptyList(),
    val additionalPrayers: List<AdditionalPrayerUi> = emptyList(),
    val previewTitle: String? = null,
    val previewHtml: String? = null,
    val isPreviewLoading: Boolean = false,
)
