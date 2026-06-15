package com.solimananas.ytbrowser.data.db

import androidx.room.*
import com.solimananas.ytbrowser.data.model.Download
import com.solimananas.ytbrowser.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY startedAt DESC")
    fun getAllDownloads(): Flow<List<Download>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY startedAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<Download>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): Download?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download): Long

    @Update
    suspend fun update(download: Download)

    @Delete
    suspend fun delete(download: Download)

    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun deleteByStatus(status: DownloadStatus)

    @Query("UPDATE downloads SET status = :status, downloadedBytes = :bytes WHERE id = :id")
    suspend fun updateProgress(id: Long, status: DownloadStatus, bytes: Long)
}
