package com.example.query.blog.post.repository

import com.example.query.blog.post.Post
import com.example.query.blog.support.dto.CursorPageResponse
import com.example.query.blog.support.dto.CursorRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface PostCustomRepository {
    fun findUseSelectFrom(keyword: String): List<Post>
    fun findUseSelect(keyword: String): List<Long>
    fun findPageByKeyword(keyword: String, pageable: Pageable): Page<Post>
    fun findPagingBy(pageable: Pageable, keyword: String): Page<Post>
    fun findSliceByKeyword(keyword: String, pageable: Pageable): Slice<Post>
    fun findPageByKeywordSequentially(keyword: String, pageable: Pageable): Page<Post>
    fun findCursorByKeyword(keyword: String, cursorRequest: CursorRequest): CursorPageResponse<Post>
}
