package com.example.query.blog.support.dto

data class CursorRequest(
    val direction: CursorDirection = CursorDirection.FIRST,
    val pageSize: Int = 20,
    val cursorKey: String? = null
)

enum class CursorDirection(val isForward: Boolean) {
    FIRST(true),
    NEXT(true),
    LAST(false),
    PREV(false)
}