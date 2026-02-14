package com.zelretch.aniiiiict.data.model

data class ProgramWithWork(val programs: List<Program>, val work: Work) {
    val firstProgram: Program get() = programs.first()
}
