package com.zelretch.aniiiiiict.data.model

import com.google.gson.annotations.SerializedName
import com.zelretch.aniiiiiict.data.model.rest.AnnictProgram

data class ProgramsResponse(
    @SerializedName("programs")
    val programs: List<AnnictProgram>
) 