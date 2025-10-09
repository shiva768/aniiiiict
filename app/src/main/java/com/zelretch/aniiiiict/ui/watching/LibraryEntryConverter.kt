package com.zelretch.aniiiiict.ui.watching

import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import java.time.LocalDateTime

/**
 * LibraryEntryをProgramWithWorkに変換するユーティリティ
 * DetailModalはProgramWithWorkを要求するため、視聴中作品から呼び出す際に変換が必要
 */
object LibraryEntryConverter {

    /**
     * LibraryEntryをProgramWithWorkに変換する
     *
     * 注意: 視聴中作品には放送スケジュール情報がないため、
     * ダミーのProgramデータを作成する
     */
    fun toProgramWithWork(entry: LibraryEntry): ProgramWithWork {
        // 次エピソードがある場合はそれを使用、なければダミーのエピソードを作成
        val episode = entry.nextEpisode ?: com.zelretch.aniiiiict.data.model.Episode(
            id = "dummy-episode-id",
            number = null,
            numberText = null,
            title = null,
            viewerDidTrack = null
        )

        // ダミーのチャンネル情報（視聴中作品には放送情報がない）
        val dummyChannel = Channel(
            name = "視聴中"
        )

        // ダミーのProgramを作成
        val program = Program(
            id = "dummy-program-id",
            startedAt = LocalDateTime.now(),
            channel = dummyChannel,
            episode = episode
        )

        return ProgramWithWork(
            programs = listOf(program),
            firstProgram = program,
            work = entry.work
        )
    }
}
