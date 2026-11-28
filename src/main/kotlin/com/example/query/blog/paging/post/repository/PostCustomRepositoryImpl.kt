package com.example.query.blog.paging.post.repository

import com.example.query.blog.paging.support.MyQuerydslSupport
import com.example.query.blog.paging.post.Post
import com.example.query.blog.paging.support.dto.CursorPageResponse
import com.example.query.blog.paging.support.dto.CursorRequest
import com.querydsl.jpa.impl.JPAQuery
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import com.example.query.blog.paging.post.QPost.post as qPost

class PostCustomRepositoryImpl : MyQuerydslSupport(Post::class.java), PostCustomRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    // MyQuerydslSupport에서 정의한 'select문' 사용 예시
    override fun findUseSelect(keyword: String): List<Long> {
        return select(qPost.id)
            .from(qPost)
            .where(qPost.title.contains(keyword))
            .fetch()
    }

    // MyQuerydslSupport에서 정의한 'selectFrom문' 사용 예시
    override fun findUseSelectFrom(keyword: String): List<Post> {
        return selectFrom(qPost)
            .where(qPost.title.contains(keyword))
            .fetch()
    }

    // 기본 페이징 로직
    // 조회 조건을 적용한 JPAQuery를 생성하고, 페이징된 데이터와 전체 데이터 수를 조회
    override fun findPagingBy(pageable: Pageable, keyword: String): Page<Post> {
        val query: JPAQuery<Post> = from(qPost).where(qPost.title.contains(keyword)) as JPAQuery<Post>
        val content: List<Post> = querydsl!!.applyPagination(pageable, query).fetch()
        val totalCount: Long = query.fetchCount()
        return PageImpl(content, pageable, totalCount)
    }

    // 무한 스크롤처럼 전체 개수가 필요 없을 때 사용, count 쿼리 없이 다음 페이지 존재 여부만 판단
    override fun findSliceByKeyword(keyword: String, pageable: Pageable): Slice<Post> {
        val query = from(qPost).where(qPost.title.contains(keyword))
        val content: List<Post> = querydsl!!.applyPagination(pageable, query)
            .limit(pageable.pageSize + 1L) // 다음 페이지 확인용으로 1건 더 조회 — applyPagination이 건 limit을 덮어씀
            .fetch()
        val hasNext = content.size > pageable.pageSize
        return SliceImpl(content.take(pageable.pageSize), pageable, hasNext)
    }

    // count 쿼리를 분리한 메서드
    override fun findPageByKeywordSequentially(keyword: String, pageable: Pageable): Page<Post> {
        val content: List<Post> = from(qPost)
            .where(qPost.title.contains(keyword))
            .run { querydsl!!.applyPagination(pageable, this).fetch() }

        val totalCount: Long = from(qPost)
            .select(qPost.count())
            .where(qPost.title.contains(keyword))
            .fetchFirst() ?: 0L

        return PageImpl(content, pageable, totalCount)
    }

    // content와 count 쿼리를 분리하고, 코루틴을 활용하여 병렬 처리
    override fun findPageByKeyword(keyword: String, pageable: Pageable): Page<Post> = runBlocking {
        log.info("findPageByKeyword thread : ${Thread.currentThread()}")
        val content: Deferred<List<Post>> = async(Dispatchers.IO) {
            log.info("content thread : ${Thread.currentThread()}")
            from(qPost)
                .where(qPost.title.contains(keyword))
                .run { querydsl!!.applyPagination(pageable, this).fetch() }
        }

        val totalCount: Deferred<Long> = async(Dispatchers.IO) {
            log.info("count thread : ${Thread.currentThread()}")
            from(qPost)
                .select(qPost.count())
                .where(qPost.title.contains(keyword))
                .fetchFirst() ?: 0L
        }
        PageImpl(content.await(), pageable, totalCount.await())
    }

    // 추출한 병렬 처리 메서드 사용
    fun useFetchPageParallel(
        pageable: Pageable,
        keyword: String
    ): Page<Post> {
        return fetchPageParallel(
            pageable = pageable,
            contentQuery = { selectFrom(qPost).where(qPost.title.contains(keyword)) },
            countQuery = { select(qPost.count()).from(qPost).where(qPost.title.contains(keyword)) }
        )
    }

    // 추출한 Slice 처리 메서드 사용
    fun useFetchSlice (
        pageable: Pageable,
        keyword: String
    ): Slice<Post> {
        return applySlicePagination(
            pageable = pageable,
            contentQuery = {
                selectFrom(qPost).where(qPost.title.contains(keyword))
            }
        )
    }

    // 추출한 커서 페이지네이션 메서드 사용 - 커서 조건과 정렬은 헬퍼가 붙이므로 검색 조건만 전달
    override fun findCursorByKeyword(
        keyword: String,
        cursorRequest: CursorRequest
    ): CursorPageResponse<Post> {
        return applyCursorPagination(
            cursorRequest = cursorRequest,
            cursorPath = qPost.id,
            cursorKeyOf = { it.id!! }, // 조회된 엔티티는 영속화된 상태라 id가 항상 존재
            contentQuery = { selectFrom(qPost).where(qPost.title.contains(keyword)) }
        )
    }
}
