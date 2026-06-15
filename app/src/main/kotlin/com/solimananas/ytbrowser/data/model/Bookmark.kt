package com.solimananas.ytbrowser.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val favicon: String? = null,
    val folder: String = "Default",
    val createdAt: Long = System.currentTimeMillis()
)
