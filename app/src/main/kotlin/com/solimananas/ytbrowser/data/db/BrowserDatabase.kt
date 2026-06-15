package com.solimananas.ytbrowser.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.solimananas.ytbrowser.data.model.Bookmark
import com.solimananas.ytbrowser.data.model.Download
import com.solimananas.ytbrowser.data.model.HistoryEntry

@Database(
    entities = [Bookmark::class, HistoryEntry::class, Download::class],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile private var instance: BrowserDatabase? = null

        fun getInstance(context: Context): BrowserDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "browser.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
