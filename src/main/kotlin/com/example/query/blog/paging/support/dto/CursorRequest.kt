package com.example.query.blog.paging.support.dto

data class CursorRequest(
    val direction: CursorDirection = CursorDirection.FIRST,
    val pageSize: Int = 20,
    val cursorKey: Long? = null
)

enum class CursorDirection(val isForward: Boolean) {
    FIRST(true),
    NEXT(true),
    LAST(false),
    PREV(false)
}