package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.local.WorkPriorityDao
import com.zelretch.aniiiiict.data.local.WorkPriorityEntity
import com.zelretch.aniiiiict.data.model.WorkPriority
import com.zelretch.aniiiiict.domain.priority.WorkPriorityPayload
import javax.inject.Inject

class SetWorkPriorityUseCase @Inject constructor(
    private val workPriorityDao: WorkPriorityDao
) {
    suspend operator fun invoke(workId: String, priority: WorkPriority): Result<Unit> = runCatching {
        if (priority == WorkPriority.NONE) {
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
    }.getOrNull() ?: WorkPriority.NONE
}

/** 別端末へ渡すため、この端末の優先度を文字列（QR / テキスト）にする */
class ExportWorkPrioritiesUseCase @Inject constructor(
    private val workPriorityDao: WorkPriorityDao
) {
    suspend operator fun invoke(): Result<String> = runCatching {
        val priorities = workPriorityDao.getAll().mapNotNull { entity ->
            runCatching { WorkPriority.valueOf(entity.priority) }.getOrNull()
                ?.takeIf { it != WorkPriority.NONE }
                ?.let { entity.workId to it }
        }.toMap()
        WorkPriorityPayload.encode(priorities)
    }
}

/** 別端末から受け取った優先度で、この端末の優先度を丸ごと置き換える */
class ImportWorkPrioritiesUseCase @Inject constructor(
    private val workPriorityDao: WorkPriorityDao
) {
    suspend operator fun invoke(priorities: Map<String, WorkPriority>): Result<Unit> = runCatching {
        workPriorityDao.replaceAll(
            priorities.filterValues { it != WorkPriority.NONE }
                .map { (workId, priority) -> WorkPriorityEntity(workId, priority.name) }
        )
    }
}
