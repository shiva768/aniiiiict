package com.zelretch.aniiiiict.testing

import com.annict.ViewerProgramsQuery
import com.annict.WorkDetailQuery
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiict.data.repository.AnnictRepository

/**
 * MockKがkotlin.Result（inline class）を正しく扱えないため、
 * instrumentation testではfake実装を使用する。
 */
open class FakeAnnictRepository : AnnictRepository {
    var authenticated = true
    var authUrl = "https://example.com/auth"
    var getAuthUrlError: Throwable? = null
    var handleAuthCallbackResult: Result<Unit> = Result.success(Unit)
    var createRecordResult: Result<Unit> = Result.success(Unit)
    var rawProgramsData: Result<List<ViewerProgramsQuery.Node?>> = Result.success(emptyList())
    var recordsResult: Result<PaginatedRecords> = Result.success(PaginatedRecords(emptyList(), false, null))
    var deleteRecordResult: Result<Unit> = Result.success(Unit)
    var updateWorkViewStatusResult: Result<Unit> = Result.success(Unit)
    var workDetailResult: Result<WorkDetailQuery.Node?> = Result.success(null)
    var libraryEntriesResult: Result<List<LibraryEntry>> = Result.success(emptyList())

    // 呼び出し記録
    val createRecordCalls = mutableListOf<Pair<String, String>>()
    val deleteRecordCalls = mutableListOf<String>()
    val updateWorkViewStatusCalls = mutableListOf<Pair<String, StatusState>>()
    val handleAuthCallbackCalls = mutableListOf<String>()
    val getRecordsCalls = mutableListOf<String?>()
    val getRawProgramsDataCallCount get() = _getRawProgramsDataCallCount
    private var _getRawProgramsDataCallCount = 0
    val isAuthenticatedCallCount get() = _isAuthenticatedCallCount
    private var _isAuthenticatedCallCount = 0
    val getAuthUrlCallCount get() = _getAuthUrlCallCount
    private var _getAuthUrlCallCount = 0

    override suspend fun isAuthenticated(): Boolean {
        _isAuthenticatedCallCount++
        return authenticated
    }

    override suspend fun getAuthUrl(): String {
        _getAuthUrlCallCount++
        getAuthUrlError?.let { throw it }
        return authUrl
    }

    override suspend fun handleAuthCallback(code: String): Result<Unit> {
        handleAuthCallbackCalls.add(code)
        return handleAuthCallbackResult
    }

    override suspend fun createRecord(episodeId: String, workId: String): Result<Unit> {
        createRecordCalls.add(episodeId to workId)
        return createRecordResult
    }

    override suspend fun getRawProgramsData(): Result<List<ViewerProgramsQuery.Node?>> {
        _getRawProgramsDataCallCount++
        return rawProgramsData
    }

    override suspend fun getRecords(after: String?): Result<PaginatedRecords> {
        getRecordsCalls.add(after)
        return recordsResult
    }

    override suspend fun deleteRecord(recordId: String): Result<Unit> {
        deleteRecordCalls.add(recordId)
        return deleteRecordResult
    }

    override suspend fun updateWorkViewStatus(workId: String, state: StatusState): Result<Unit> {
        updateWorkViewStatusCalls.add(workId to state)
        return updateWorkViewStatusResult
    }

    override suspend fun getWorkDetail(workId: String): Result<WorkDetailQuery.Node?> = workDetailResult

    override suspend fun getLibraryEntries(states: List<StatusState>, after: String?): Result<List<LibraryEntry>> =
        libraryEntriesResult
}
