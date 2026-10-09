package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.local.WorkPriorityDao
import com.zelretch.aniiiiict.data.local.WorkPriorityEntity
import com.zelretch.aniiiiict.data.model.WorkPriority
import javax.inject.Inject

class SetWorkPriorityUseCase @Inject constructor(
    private val workPriorityDao: WorkPriorityDao
) {
    suspend operator fun invoke(workId: String, priority: WorkPriority): Result<Unit> = runCatching {
        if (priority == WorkPriority.NORMAL) {
            workPriorityDao.delete(workId)
        } else {
            workPriorityDao.upsert(WorkPriorityEntity(workId, priority.name))
        }
    }
}

class GetWorkPriorityUseCase @Inject constructor(
    private val workPriorityDao: WorkPriorityDao
) {
    suspend operator fun invoke(workId: String): WorkPriority = runCatching {
        workPriorityDao.get(workId)?.priority?.let { WorkPriority.valueOf(it) }
    }.getOrNull() ?: WorkPriority.NORMAL
}
