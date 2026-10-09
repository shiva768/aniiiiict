package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.local.WorkPriorityDao
import com.zelretch.aniiiiict.data.local.WorkPriorityEntity
import com.zelretch.aniiiiict.data.model.WorkPriority
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("作品の優先度ユースケース")
class WorkPriorityUseCasesTest {

    private lateinit var dao: WorkPriorityDao

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
    }

    @Test
    @DisplayName("注目/後回しは保存される")
    fun setStoresNonNormal() = runTest {
        SetWorkPriorityUseCase(dao)("work1", WorkPriority.DEFERRED)

        coVerify { dao.upsert(WorkPriorityEntity("work1", "DEFERRED")) }
    }

    @Test
    @DisplayName("ふつうにすると行を削除する")
    fun setNormalDeletes() = runTest {
        SetWorkPriorityUseCase(dao)("work1", WorkPriority.NORMAL)

        coVerify { dao.delete("work1") }
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    @DisplayName("保存が無い作品はふつうを返す")
    fun getDefaultsToNormal() = runTest {
        coEvery { dao.get("work1") } returns null

        assertEquals(WorkPriority.NORMAL, GetWorkPriorityUseCase(dao)("work1"))
    }

    @Test
    @DisplayName("保存されている優先度を返す")
    fun getReturnsStored() = runTest {
        coEvery { dao.get("work1") } returns WorkPriorityEntity("work1", "FEATURED")

        assertEquals(WorkPriority.FEATURED, GetWorkPriorityUseCase(dao)("work1"))
    }
}
