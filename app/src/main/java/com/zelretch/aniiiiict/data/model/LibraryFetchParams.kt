package com.zelretch.aniiiiict.data.model

import com.annict.type.SeasonName
import com.annict.type.StatusState

data class LibraryFetchParams(
    val selectedStates: List<StatusState>,
    val seasonFromYear: Int,
    val seasonFromName: SeasonName
) {
    val seasonFrom: String
        get() = "$seasonFromYear-${seasonFromName.rawValue.lowercase()}"

    val hash: String
        get() = "${selectedStates.map { it.name }.sorted()}|$seasonFrom"
}
