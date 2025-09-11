package com.zelretch.aniiiiict.ui.works.components

import com.annict.type.SeasonName

data class FilterOptions(
    val media: List<String>,
    val seasons: List<SeasonName>,
    val years: List<Int>,
    val channels: List<String>
)
