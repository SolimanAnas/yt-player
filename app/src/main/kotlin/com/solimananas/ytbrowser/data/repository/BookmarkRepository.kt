package com.solimananas.ytbrowser.data.repository

import com.solimananas.ytbrowser.data.db.BookmarkDao
import com.solimananas.ytbrowser.data.model.Bookmark
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val dao: BookmarkDao) {

    fun getAllBookmarks(): Flow<List<Bookmark>> = dao.getAllBookmarks()

    fun getBookmarksByFolder(folder: String): Flow<List<Bookmark>> =
        dao.getBookmarksByFolder(folder)

    suspend fun addBookmark(url: String, title: String, favicon: String? = null): Bookmark {
        val bookmark = Bookmark(url = url, title = title, favicon = favicon)
        val id = dao.insert(bookmark)
        return bookmark.copy(id = id)
    }

    suspend fun removeBookmark(url: String) = dao.deleteByUrl(url)

    suspend fun updateBookmark(bookmark: Bookmark) = dao.update(bookmark)

    fun isBookmarked(url: String): Flow<Boolean> = dao.isBookmarked(url)

    fun search(query: String): Flow<List<Bookmark>> = dao.search(query)

    fun getFolders(): Flow<List<String>> = dao.getFolders()

    suspend fun toggleBookmark(url: String, title: String, favicon: String? = null): Boolean {
        val existing = dao.getByUrl(url)
        return if (existing != null) {
            dao.delete(existing)
            false
        } else {
            dao.insert(Bookmark(url = url, title = title, favicon = favicon))
            true
        }
    }
}
