package com.solimananas.ytbrowser.data.repository

import com.solimananas.ytbrowser.data.db.HistoryDao
import com.solimananas.ytbrowser.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {

    fun getAllHistory(): Flow<List<HistoryEntry>> = dao.getAllHistory()

    fun search(query: String): Flow<List<HistoryEntry>> = dao.search(query)

    suspend fun addVisit(url: String, title: String) {
        val existing = dao.getByUrl(url)
        if (existing != null) {
            dao.update(existing.copy(
                title = title,
                visitedAt = System.currentTimeMillis(),
                visitCount = existing.visitCount + 1
            ))
        } else {
            dao.insert(HistoryEntry(url = url, title = title))
        }
    }

    suspend fun delete(entry: HistoryEntry) = dao.delete(entry)

    suspend fun clearAll() = dao.clearAll()

    suspend fun clearBefore(timestamp: Long) = dao.clearBefore(timestamp)

    fun getHistorySince(since: Long): Flow<List<HistoryEntry>> = dao.getHistorySince(since)
}
