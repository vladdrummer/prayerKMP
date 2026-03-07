package com.vladdrummer.prayerkmp.feature.bible.view_model

data class BibleViewState(
    val isLoading: Boolean = true,
    val errorText: String? = null,
    val mode: BibleMode = BibleMode.Books,
    val books: List<String> = emptyList(),
    val expandedBook: String? = null,
    val selectedBook: String? = null,
    val chapters: List<Int> = emptyList(),
    val chapterTexts: Map<Int, String> = emptyMap(),
    val loadingChapters: Set<Int> = emptySet(),
    val lastReadBook: String? = null,
    val lastReadChapter: Int? = null,
    val fontSizeSp: Int = 20,
    val fontIndex: Int = 1,
    val currentReaderPage: Int = 0,
    val readerSessionId: Long = 0L,
)

enum class BibleMode {
    Books,
    Reader,
}
