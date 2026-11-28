package com.example.query.blog.support

import com.example.query.blog.support.dto.CursorDirection
import com.example.query.blog.support.dto.CursorPageResponse
import com.example.query.blog.support.dto.CursorRequest
import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Expression
import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport

abstract class MyQuerydslSupport (c: Class<*>) : QuerydslRepositorySupport(c) {
    protected lateinit var queryFactory: JPAQueryFactory

    // JPAQueryFactory 초기화 후 부모 클래스에 전달
    @PersistenceContext
    override fun setEntityManager(entityManager: EntityManager) {
        this.queryFactory = JPAQueryFactory(entityManager)
        super.setEntityManager(entityManager)
    }

    // select문
    // Expression : 조회할 대상 지정
    protected fun <T> select(expr: Expression<T>): JPAQuery<T> {
        return queryFactory.select(expr)
    }

    // selectFrom문
    // EntityPath : 엔티티 하나를 가리키는 Querydsl의 경로
    protected fun <T> selectFrom(from: EntityPath<T>): JPAQuery<T> {
        return queryFactory.selectFrom(from)
    }

    // content, count 코루틴 병렬 처리 추출
    protected fun <T: Any> fetchPageParallel(
        pageable: Pageable,
        contentQuery: (JPAQueryFactory) -> JPAQuery<T>,
        countQuery: (JPAQueryFactory) -> JPAQuery<Long>
    ): Page<T> = runBlocking {
        val content = async(Dispatchers.IO) { querydsl!!.applyPagination(pageable, contentQuery(queryFactory)).fetch() }
        val total = async(Dispatchers.IO) { countQuery(queryFactory).fetchFirst() ?: 0L }
        PageImpl(content.await(), pageable, total.await())
    }

    // slice 처리 추출
    protected fun <T: Any> fetchSlice(
        pageable: Pageable,
        contentQuery: (JPAQueryFactory) -> JPAQuery<T>
    ): Slice<T> {
        val content = querydsl!!.applyPagination(pageable, contentQuery(queryFactory))
            .limit((pageable.pageSize + 1).toLong())
            .fetch()

        val hasNext = content.size > pageable.pageSize
        return SliceImpl(content.take(pageable.pageSize), pageable, hasNext)
    }

    // 커서 기반 페이지네이션 메서드
    // 방향은 클라이언트가 보내고, cursorPagination이 그 값으로 조회 조건과 정렬을 정합니다.
    protected fun <T> cursorPagination(
        cursorRequest: CursorRequest,
        cursorPath: NumberPath<Long>,
        cursorKeyOf: (T) -> Long, // 결과 항목에서 cursorPath와 같은 키 값을 꺼내는 방법
        contentQuery: (JPAQueryFactory) -> JPAQuery<T>
    ): CursorPageResponse<T> {
        val direction = cursorRequest.direction
        val pageSize = cursorRequest.pageSize
        val cursorValue = cursorRequest.cursorKey

        val query = contentQuery(queryFactory)

        when(direction) {
            CursorDirection.FIRST, CursorDirection.LAST -> Unit
            CursorDirection.NEXT -> {
                // cursorValue (커서 값) 없이 NEXT를 보내면 예외 발생시킴
                requireNotNull(cursorValue) { "NEXT direction : cursorValue가 존재하지 않습니다." }
                query.where(cursorPath.lt(cursorValue))
            }
            CursorDirection.PREV -> {
                requireNotNull(cursorValue) { "PREV direction : cursorValue가 존재하지 않습니다." }
                query.where(cursorPath.gt(cursorValue))
            }
        }

        // 정렬 조건
        query.orderBy(
            when {
                direction.isForward -> cursorPath.desc()
                else -> cursorPath.asc()
            }
        )

        query.limit((pageSize + 1).toLong())
        val content = query.fetch()

        // 응답 객체는 팩토리 함수(invoke)를 통해 방향별 커서와 플래그를 자동으로 계산
        // 실제로는 CursorPageResponse.Companion.invoke(...) 형태
        return CursorPageResponse(
            content = content,
            direction = direction,
            pageSize = pageSize,
            cursorKeyOf = cursorKeyOf,
        )
    }
}