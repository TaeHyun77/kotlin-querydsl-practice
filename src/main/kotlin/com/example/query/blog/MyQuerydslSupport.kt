package com.example.query.blog

import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Expression
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
}