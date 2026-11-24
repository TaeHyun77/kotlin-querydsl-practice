package com.example.query.blog.support.dto

data class CursorPageResponse<T> private constructor(
    val content: List<T>,
    val hasNext: Boolean,
    val hasPrev: Boolean,
    val nextCursor: T?,
    val prevCursor: T?,
) {
    companion object {
        operator fun <T> invoke(
            content: List<T>,
            direction: CursorDirection,
            pageSize: Int,
        ): CursorPageResponse<T> {
            // 1. pageSize + 1건을 받았는지로 "같은 방향에 더 있는지" 판단
            val hasMore = content.size > pageSize

            // 2. pageSize만큼 자르고, 역방향(LAST, PREV)이면 화면 순서로 뒤집기
            val sliced = content.take(pageSize)
            val ordered = if (direction.isForward) sliced else sliced.reversed()

            // 3. 방향별 hasNext / hasPrev
            val (hasNext, hasPrev) = when (direction) {
                CursorDirection.FIRST -> hasMore to false
                CursorDirection.NEXT  -> hasMore to true
                CursorDirection.LAST  -> false to hasMore
                CursorDirection.PREV  -> true to hasMore
            }

            // 4. 커서: 다음은 마지막 항목, 이전은 첫 항목
            return CursorPageResponse(
                content = ordered,
                hasNext = hasNext,
                hasPrev = hasPrev,
                nextCursor = if (hasNext) ordered.lastOrNull() else null,
                prevCursor = if (hasPrev) ordered.firstOrNull() else null,
            )
        }
    }
}