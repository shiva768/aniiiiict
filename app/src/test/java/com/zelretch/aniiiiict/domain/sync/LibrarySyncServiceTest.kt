package com.zelretch.aniiiiict.domain.sync

import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.local.LibraryEntryDao
import com.zelretch.aniiiiict.data.local.toEntity
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntriesPage
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LibrarySyncService.syncEntry")
class LibrarySyncServiceTest {

    private lateinit var repository: AnnictRepository
    private lateinit var dao: LibraryEntryDao
    private lateinit var service: LibrarySyncService

    private fun entry(
        id: String = "entry1",
        seasonYear: Int? = 2024,
        seasonName: SeasonName? = SeasonName.AUTUMN,
        nextEpisodeNumber: Int = 3
    ) = LibraryEntry(
        id = id,
        work = Work(
            id = "work-$id",
            title = "作品$id",
            seasonYear = seasonYear,
            seasonName = seasonName,
            viewerStatusState = StatusState.WATCHING
        ),
        nextEpisode = Episode(id = "ep$nextEpisodeNumber", number = nextEpisodeNumber),
        statusState = StatusState.WATCHING
    )

    private fun page(vararg entries: LibraryEntry) =
        Result.success(LibraryEntriesPage(entries.toList(), hasNextPage = false, endCursor = null))

    @BeforeEach
    fun setup() {
        repository = mockk()
        dao = mockk(relaxed = true)
        service = LibrarySyncService(repository, dao)
    }

    @Test
    @DisplayName("作品のシーズンで絞った一覧から見つけて更新する（消さない）")
    fun updatesFromSeasonList() = runTest {
        coEvery { dao.getById("entry1") } returns entry(nextEpisodeNumber = 3).toEntity()
        val updated = entry(nextEpisodeNumber = 4)
        coEvery { repository.getLibraryEntries(any(), any(), listOf("2024-autumn")) } returns
            page(entry("other"), updated)

        service.syncEntry("entry1")

        coVerify { dao.upsert(match { it.id == "entry1" && it.nextEpisodeNumber == 4 }) }
        coVerify(exactly = 0) { dao.deleteById(any()) }
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }

    @Test
    @DisplayName("シーズン内に見つからないときは消さずに全件同期する")
    fun fallsBackToFullSyncWhenNotFound() = runTest {
        coEvery { dao.getById("entry1") } returns entry().toEntity()
        coEvery { repository.getLibraryEntries(any(), any(), listOf("2024-autumn")) } returns page()
        coEvery { repository.getLibraryEntries(any(), any(), null) } returns page(entry("other"))

        service.syncEntry("entry1")

        coVerify(exactly = 0) { dao.deleteById(any()) }
        coVerify { dao.replaceAll(match { list -> list.map { it.id } == listOf("other") }) }
        assertEquals(SyncStatus.Idle, service.status.value)
    }

    @Test
    @DisplayName("シーズン不明の作品は全件同期する")
    fun fullSyncWhenSeasonUnknown() = runTest {
        coEvery { dao.getById("entry1") } returns entry(seasonYear = null, seasonName = null).toEntity()
        coEvery { repository.getLibraryEntries(any(), any(), null) } returns page(entry(nextEpisodeNumber = 4))

        service.syncEntry("entry1")

        coVerify { dao.replaceAll(match { list -> list.single().nextEpisodeNumber == 4 }) }
    }

    @Test
    @DisplayName("取得に失敗したら何も変えない")
    fun keepsRowOnFailure() = runTest {
        coEvery { dao.getById("entry1") } returns entry().toEntity()
        coEvery { repository.getLibraryEntries(any(), any(), any()) } returns Result.failure(RuntimeException("x"))

        service.syncEntry("entry1")

        coVerify(exactly = 0) { dao.deleteById(any()) }
        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }

    @Test
    @DisplayName("advanceEntry は次の話を差し替え、視聴予定なら視聴中にする")
    fun advanceEntryUpdatesNextEpisode() = runTest {
        val wannaWatch = entry(nextEpisodeNumber = 1).copy(statusState = StatusState.WANNA_WATCH)
        coEvery { dao.getById("entry1") } returns wannaWatch.toEntity()

        service.advanceEntry("entry1", Episode(id = "ep2", number = 2, title = "二話"))

        coVerify {
            dao.upsert(
                match {
                    it.nextEpisodeId == "ep2" &&
                        it.nextEpisodeNumber == 2 &&
                        it.nextEpisodeTitle == "二話" &&
                        it.statusState == "WATCHING"
                }
            )
        }
    }

    @Test
    @DisplayName("advanceEntry で次の話が無ければ空にする（すべて視聴済み）")
    fun advanceEntryClearsWhenNoNext() = runTest {
        coEvery { dao.getById("entry1") } returns entry().toEntity()

        service.advanceEntry("entry1", null)

        coVerify { dao.upsert(match { it.nextEpisodeId == null && it.nextEpisodeNumber == null }) }
    }

    @Test
    @DisplayName("applyStatus で対象外（中止）にすると取り直さずに手元から消す")
    fun applyStatusRemovesWhenNotTarget() = runTest {
        service.applyStatus("entry1", StatusState.STOP_WATCHING)

        coVerify { dao.deleteById("entry1") }
        coVerify(exactly = 0) { repository.getLibraryEntries(any(), any(), any()) }
    }

    @Test
    @DisplayName("applyStatus で対象のステータスなら手元の行のステータスを書き換える")
    fun applyStatusUpdatesWhenTarget() = runTest {
        coEvery { dao.getById("entry1") } returns entry().toEntity()

        service.applyStatus("entry1", StatusState.ON_HOLD)

        coVerify { dao.upsert(match { it.statusState == "ON_HOLD" && it.workViewerStatusState == "ON_HOLD" }) }
        coVerify(exactly = 0) { dao.deleteById(any()) }
    }
}
