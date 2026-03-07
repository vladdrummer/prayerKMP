package com.vladdrummer.prayerkmp.feature.readings.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.padeg.Padeg
import com.vladdrummer.prayerkmp.feature.readings.ReadingsRepository
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import com.vladdrummer.prayerkmp.feature.strings.getString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReadingsViewModel(
    private val storage: AppStorage,
) : ViewModel() {
    private val viewStateFlow = MutableStateFlow(ReadingsViewState())
    val viewState: StateFlow<ReadingsViewState> = viewStateFlow.asStateFlow()

    init {
        readingsLog("ReadingsViewModel init")
        load()
    }

    fun load() {
        readingsLog("load requested")
        viewModelScope.launch {
            readingsLog("load coroutine started")
            viewStateFlow.value = viewStateFlow.value.copy(isLoading = true, errorText = null)
            readingsLog("state -> loading=true, error=null")
            runCatching {
                val nameImenit = storage.stringFlow(AppStorageKeys.NameImenit, DEFAULT_NAME_IMENIT).first()
                val isMale = storage.booleanFlow(AppStorageKeys.MyGenderMale, false).first()
                val nameInPadeg4 = if (nameImenit == DEFAULT_NAME_IMENIT || nameImenit.isBlank()) {
                    DEFAULT_NAME_IMENIT
                } else {
                    runCatching { Padeg.getFIOPadeg("", nameImenit, "", isMale, 4) }.getOrDefault(nameImenit)
                }
                val isNameUnknown = nameImenit == DEFAULT_NAME_IMENIT || nameImenit.isBlank()
                val who = when {
                    isNameUnknown -> "${getString("readings_prayer_raba")}/${getString("readings_prayer_rabu")}"
                    isMale -> getString("readings_prayer_raba")
                    else -> getString("readings_prayer_rabu")
                }
                val outroBase = getString("readings_prayer_end")
                val outroHtml = when {
                    isNameUnknown -> outroBase.replace("раба Твоего", "раба Твоего/рабу Твою")
                    isMale -> outroBase
                    else -> outroBase.replace("раба Твоего", "рабу Твою")
                }
                val introBase = getString("readings_prayer_beginning").trimEnd()
                val introBaseWithComma = if (introBase.endsWith(",")) introBase else "$introBase,"
                val introHtml = "$introBaseWithComma $who $nameInPadeg4 $outroHtml"
                readingsLog("intro prepared, isMale=$isMale, name=$nameImenit, namePadeg4=$nameInPadeg4")
                ReadingsRepository.loadTodayHtml(storage = storage, introHtml = introHtml, outroHtml = introHtml)
            }
                .onSuccess { html ->
                    readingsLog("repository success, html chars=${html.length}")
                    viewStateFlow.value = viewStateFlow.value.copy(
                        isLoading = false,
                        htmlText = html,
                        errorText = null,
                    )
                    readingsLog("state -> loading=false, html chars=${viewStateFlow.value.htmlText.length}, error=null")
                }
                .onFailure { error ->
                    readingsLog("repository failed: ${error.message}")
                    viewStateFlow.value = viewStateFlow.value.copy(
                        isLoading = false,
                        htmlText = "",
                        errorText = "Не удалось загрузить евангельские чтения",
                    )
                    readingsLog("state -> loading=false, html cleared, error set")
                }
        }
    }

    private fun readingsLog(message: String) {
        println("readings: vm $message")
    }

    private companion object {
        private const val DEFAULT_NAME_IMENIT = "(Имя)"
    }
}
