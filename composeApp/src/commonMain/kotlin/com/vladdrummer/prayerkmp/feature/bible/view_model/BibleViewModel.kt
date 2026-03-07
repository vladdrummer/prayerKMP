package com.vladdrummer.prayerkmp.feature.bible.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.bible.BibleRepository
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BibleViewModel(
    private val storage: AppStorage,
    private val initialTarget: BibleReaderTarget? = null,
) : ViewModel() {
    private companion object {
        private const val FONT_COUNT = 5
        private const val DEFAULT_FONT_SIZE_SP = 20
        private const val DEFAULT_FONT_INDEX = 1
    }

    private val viewStateFlow = MutableStateFlow(BibleViewState())
    val viewState: StateFlow<BibleViewState> = viewStateFlow.asStateFlow()

    init {
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch {
            viewStateFlow.value = viewStateFlow.value.copy(isLoading = true, errorText = null)
            runCatching {
                val books = BibleRepository.getBooks().map { it.name }
                val lastBook = storage.stringFlow(AppStorageKeys.BibleLastBook, "").first().ifBlank { null }
                val lastChapter = storage.stringFlow(AppStorageKeys.BibleLastChapter, "").first().toIntOrNull()
                val fontSize = storage.stringFlow(AppStorageKeys.BibleFontSizeSp, "").first().toIntOrNull()
                    ?.coerceIn(12, 50) ?: DEFAULT_FONT_SIZE_SP
                val fontIndex = storage.stringFlow(AppStorageKeys.BibleFontIndex, "").first().toIntOrNull()
                    ?.coerceIn(0, FONT_COUNT - 1) ?: DEFAULT_FONT_INDEX
                LoadedBibleData(
                    books = books,
                    lastBook = lastBook,
                    lastChapter = lastChapter,
                    fontSizeSp = fontSize,
                    fontIndex = fontIndex,
                )
            }.onSuccess { data ->
                viewStateFlow.value = viewStateFlow.value.copy(
                    isLoading = false,
                    errorText = null,
                    mode = BibleMode.Books,
                    books = data.books,
                    expandedBook = null,
                    selectedBook = null,
                    chapters = emptyList(),
                    chapterTexts = emptyMap(),
                    loadingChapters = emptySet(),
                    lastReadBook = data.lastBook,
                    lastReadChapter = data.lastChapter,
                    fontSizeSp = data.fontSizeSp,
                    fontIndex = data.fontIndex,
                    currentReaderPage = 0,
                )
                initialTarget?.let { openReaderAt(it.book, it.chapter) }
            }.onFailure {
                viewStateFlow.value = viewStateFlow.value.copy(
                    isLoading = false,
                    errorText = "Не удалось загрузить Библию",
                )
            }
        }
    }

    fun toggleBookExpansion(bookName: String) {
        val current = viewStateFlow.value
        if (current.expandedBook == bookName) {
            viewStateFlow.value = current.copy(
                expandedBook = null,
                selectedBook = null,
                chapters = emptyList(),
                chapterTexts = emptyMap(),
                loadingChapters = emptySet(),
            )
            return
        }
        viewModelScope.launch {
            viewStateFlow.value = viewStateFlow.value.copy(errorText = null)
            runCatching {
                BibleRepository.getChapters(bookName)
            }.onSuccess { chapters ->
                viewStateFlow.value = viewStateFlow.value.copy(
                    errorText = null,
                    mode = BibleMode.Books,
                    expandedBook = bookName,
                    selectedBook = bookName,
                    chapters = chapters,
                    chapterTexts = emptyMap(),
                    loadingChapters = emptySet(),
                    currentReaderPage = 0,
                )
            }.onFailure {
                viewStateFlow.value = viewStateFlow.value.copy(
                    errorText = "Не удалось загрузить список глав",
                )
            }
        }
    }

    fun openChapter(chapterNumber: Int) {
        val state = viewStateFlow.value
        val chapters = state.chapters
        val pageIndex = chapters.indexOf(chapterNumber).takeIf { it >= 0 } ?: 0
        viewStateFlow.value = state.copy(
            mode = BibleMode.Reader,
            currentReaderPage = pageIndex,
            readerSessionId = state.readerSessionId + 1,
            lastReadBook = state.selectedBook,
            lastReadChapter = chapterNumber,
        )
        persistLastRead(state.selectedBook, chapterNumber)
        ensureChapterLoaded(chapterNumber)
        chapters.getOrNull(pageIndex - 1)?.let(::ensureChapterLoaded)
        chapters.getOrNull(pageIndex + 1)?.let(::ensureChapterLoaded)
    }

    fun openReaderAt(bookName: String, chapterNumber: Int) {
        viewModelScope.launch {
            runCatching {
                BibleRepository.getChapters(bookName)
            }.onSuccess { chapters ->
                viewStateFlow.value = viewStateFlow.value.copy(
                    expandedBook = bookName,
                    selectedBook = bookName,
                    chapters = chapters,
                    chapterTexts = emptyMap(),
                    loadingChapters = emptySet(),
                )
                openChapter(chapterNumber)
            }.onFailure {
                viewStateFlow.value = viewStateFlow.value.copy(
                    errorText = "Не удалось открыть книгу",
                )
            }
        }
    }

    fun continueLastRead() {
        val state = viewStateFlow.value
        val book = state.lastReadBook ?: return
        val chapter = state.lastReadChapter ?: return
        openReaderAt(book, chapter)
    }

    fun onReaderPageChanged(pageIndex: Int) {
        val state = viewStateFlow.value
        val chapterNumber = state.chapters.getOrNull(pageIndex) ?: return
        viewStateFlow.value = state.copy(
            currentReaderPage = pageIndex,
            lastReadBook = state.selectedBook,
            lastReadChapter = chapterNumber,
        )
        persistLastRead(state.selectedBook, chapterNumber)
        ensureChapterLoaded(chapterNumber)
        state.chapters.getOrNull(pageIndex - 1)?.let(::ensureChapterLoaded)
        state.chapters.getOrNull(pageIndex + 1)?.let(::ensureChapterLoaded)
    }

    fun backToBooks() {
        viewStateFlow.value = viewStateFlow.value.copy(
            mode = BibleMode.Books,
            selectedBook = viewStateFlow.value.expandedBook,
        )
    }

    fun increaseFontSize() {
        val size = (viewStateFlow.value.fontSizeSp + 1).coerceAtMost(50)
        viewStateFlow.value = viewStateFlow.value.copy(fontSizeSp = size)
        persistBibleFont()
    }

    fun decreaseFontSize() {
        val size = (viewStateFlow.value.fontSizeSp - 1).coerceAtLeast(12)
        viewStateFlow.value = viewStateFlow.value.copy(fontSizeSp = size)
        persistBibleFont()
    }

    fun switchFont() {
        val next = (viewStateFlow.value.fontIndex + 1) % FONT_COUNT
        viewStateFlow.value = viewStateFlow.value.copy(fontIndex = next)
        persistBibleFont()
    }

    fun resetFontDefaults() {
        viewStateFlow.value = viewStateFlow.value.copy(
            fontSizeSp = DEFAULT_FONT_SIZE_SP,
            fontIndex = DEFAULT_FONT_INDEX,
        )
        persistBibleFont()
    }

    private fun ensureChapterLoaded(chapterNumber: Int) {
        val current = viewStateFlow.value
        val selectedBook = current.selectedBook ?: return
        if (current.chapterTexts.containsKey(chapterNumber) || current.loadingChapters.contains(chapterNumber)) return
        viewStateFlow.value = current.copy(loadingChapters = current.loadingChapters + chapterNumber)
        viewModelScope.launch {
            val html = runCatching {
                BibleRepository.getChapterHtml(selectedBook, chapterNumber).orEmpty()
            }.getOrDefault("")
            val state = viewStateFlow.value
            viewStateFlow.value = state.copy(
                chapterTexts = state.chapterTexts + (chapterNumber to html),
                loadingChapters = state.loadingChapters - chapterNumber,
            )
        }
    }

    private fun persistLastRead(book: String?, chapter: Int) {
        if (book.isNullOrBlank()) return
        viewModelScope.launch {
            storage.setString(AppStorageKeys.BibleLastBook, book)
            storage.setString(AppStorageKeys.BibleLastChapter, chapter.toString())
        }
    }

    private fun persistBibleFont() {
        val state = viewStateFlow.value
        viewModelScope.launch {
            storage.setString(AppStorageKeys.BibleFontSizeSp, state.fontSizeSp.toString())
            storage.setString(AppStorageKeys.BibleFontIndex, state.fontIndex.toString())
        }
    }
}

data class BibleReaderTarget(
    val book: String,
    val chapter: Int,
)

private data class LoadedBibleData(
    val books: List<String>,
    val lastBook: String?,
    val lastChapter: Int?,
    val fontSizeSp: Int,
    val fontIndex: Int,
)
