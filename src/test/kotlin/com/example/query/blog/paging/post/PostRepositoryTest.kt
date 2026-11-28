package com.example.query.blog.paging.post

import com.example.query.blog.paging.member.Member
import com.example.query.blog.paging.post.repository.PostRepository
import com.example.query.blog.paging.support.dto.CursorDirection
import com.example.query.blog.paging.support.dto.CursorRequest
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 내장 DB 없이 application.properties의 MySQL을 사용 — 각 테스트는 트랜잭션 롤백되어 데이터가 남지 않음
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest @Autowired constructor(
    private val postRepository: PostRepository,
    private val em: EntityManager,
) {
    @BeforeEach
    fun setUp() {
        val kim = Member("kim").also(em::persist)
        em.persist(Post("Querydsl 입문", "내용1", kim))
        em.persist(Post("Querydsl 심화", "내용2", kim))
        em.persist(Post("JPA 기초", "내용3", kim))
        em.flush()
        em.clear()
    }

    @Test
    fun `selectFrom - 키워드가 포함된 게시글 엔티티를 조회한다`() {
        val result = postRepository.findUseSelectFrom("Querydsl")

        assertEquals(setOf("Querydsl 입문", "Querydsl 심화"), result.map { it.title }.toSet())
    }

    @Test
    fun `select - 키워드가 포함된 게시글의 id만 조회한다`() {
        val expected = postRepository.findUseSelectFrom("Querydsl").map { it.id }.toSet()

        val result = postRepository.findUseSelect("Querydsl")

        assertEquals(expected, result.toSet())
    }

    // 요청 스레드와 코루틴 실행 스레드 로그 확인용 — 결과 검증은 하지 않음
    @Test
    fun `findPageByKeyword - 내용과 count 쿼리를 코루틴으로 실행한다`() {
        postRepository.findPageByKeyword("Querydsl", PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")))
    }

    @Test
    fun `findSliceByKeyword - count 없이 다음 페이지 존재 여부를 판단한다`() {
        val first = postRepository.findSliceByKeyword("Querydsl", PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")))
        val last = postRepository.findSliceByKeyword("Querydsl", PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "id")))

        assertEquals(listOf("Querydsl 심화"), first.content.map { it.title })
        assertTrue(first.hasNext())
        assertEquals(listOf("Querydsl 입문"), last.content.map { it.title })
        assertFalse(last.hasNext())
    }

    @Test
    fun `findPageByKeywordSequentially - 내용과 전체 개수를 순차로 조회한다`() {
        val result = postRepository.findPageByKeywordSequentially("Querydsl", PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")))

        assertEquals(listOf("Querydsl 심화"), result.content.map { it.title })
        assertEquals(2L, result.totalElements)
    }

    // 데이터: "Querydsl 입문"(id 작음), "Querydsl 심화"(id 큼) — pageSize 1로 한 건씩 이동
    @Test
    fun `findCursorByKeyword - FIRST, NEXT, PREV 순서로 커서를 따라 이동한다`() {
        val first = postRepository.findCursorByKeyword("Querydsl", CursorRequest(CursorDirection.FIRST, pageSize = 1))

        assertEquals(listOf("Querydsl 심화"), first.content.map { it.title })
        assertTrue(first.hasNext)
        assertFalse(first.hasPrev)
        assertEquals(first.content.last().id, first.nextCursor)

        val next = postRepository.findCursorByKeyword(
            "Querydsl",
            CursorRequest(CursorDirection.NEXT, pageSize = 1, cursorKey = first.nextCursor)
        )

        assertEquals(listOf("Querydsl 입문"), next.content.map { it.title })
        assertFalse(next.hasNext)
        assertTrue(next.hasPrev)

        val prev = postRepository.findCursorByKeyword(
            "Querydsl",
            CursorRequest(CursorDirection.PREV, pageSize = 1, cursorKey = next.prevCursor)
        )

        assertEquals(listOf("Querydsl 심화"), prev.content.map { it.title })
        assertTrue(prev.hasNext)
        assertFalse(prev.hasPrev)
    }

    @Test
    fun `findCursorByKeyword - LAST는 마지막 페이지를 화면 순서로 반환한다`() {
        val last = postRepository.findCursorByKeyword("Querydsl", CursorRequest(CursorDirection.LAST, pageSize = 1))

        assertEquals(listOf("Querydsl 입문"), last.content.map { it.title })
        assertFalse(last.hasNext)
        assertTrue(last.hasPrev)
    }
}
