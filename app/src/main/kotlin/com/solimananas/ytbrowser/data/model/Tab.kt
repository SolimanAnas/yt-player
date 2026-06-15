package com.solimananas.ytbrowser.data.model

import java.util.UUID

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "about:blank",
    val title: String = "New Tab",
    val favicon: String? = null,
    val isIncognito: Boolean = false,
    val isLoading: Boolean = false,
    val loadProgress: Float = 0f,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val scrollY: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
