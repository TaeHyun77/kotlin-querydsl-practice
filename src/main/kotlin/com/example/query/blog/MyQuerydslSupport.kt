package com.example.query.blog

import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Expression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
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
}