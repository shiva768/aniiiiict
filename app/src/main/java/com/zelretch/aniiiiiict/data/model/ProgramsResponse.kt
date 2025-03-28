package com.zelretch.aniiiiiict.data.model

import com.google.gson.annotations.SerializedName

data class ProgramsResponse(
    @SerializedName("programs")
    val programs: List<AnnictProgram>
) 