package com.zelretch.aniiiiict.data.model

data class LibraryEntriesPage(
    val entries: List<LibraryEntry>,
    val hasNextPage: Boolean,
    val endCursor: String?
)
