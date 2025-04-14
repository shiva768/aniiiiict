package com.zelretch.aniiiiiict.data.model

data class ProgramWithWork(
    var programs: List<Program>,
    val firstProgram: Program,
    val work: Work
) {
    fun removePrograms(programs: List<Program>) {
//        programs.drop()
    }
}