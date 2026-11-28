package com.example.query.blog.batch.writer

import com.example.query.blog.batch.writer.dto.WriterUpdate
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

// 내장 DB 없이 MySQL 사용 — 서비스가 트랜잭션에 참여하므로 Querydsl-SQL로 넣은 데이터도 테스트 종료 시 롤백됨
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WriterBatchService::class)
class WriterBatchServiceTest @Autowired constructor(
    private val writerBatchService: WriterBatchService,
    private val writerRepository: WriterRepository,
    private val em: EntityManager,
) {
    @Test
    fun `batchInsert - 전달한 Writer를 한 번에 insert 한다`() {
        val before = writerRepository.count()

        writerBatchService.batchInsert(writers(3))

        assertEquals(before + 3, writerRepository.count())
    }

    @Test
    fun `batchUpdate - id로 지정한 Writer의 name만 변경한다`() {
        val saved = writers(2).onEach(em::persist)
        em.flush() // Querydsl-SQL은 영속성 컨텍스트를 보지 않으므로 먼저 DB에 반영

        val updated = writerBatchService.batchUpdate(saved.map { WriterUpdate(it.id!!, "changed-${it.id}") })
        em.clear() // 1차 캐시에 남은 옛 name 대신 DB 값을 다시 읽기 위해 비움

        assertEquals(2, updated)
        saved.forEach {
            val reloaded = writerRepository.findById(it.id!!).orElseThrow()
            assertEquals("changed-${it.id}", reloaded.name)
            assertEquals(it.email, reloaded.email) // 지정하지 않은 컬럼은 그대로
        }
    }

    // 시간 측정 로그 확인용 — 로컬 환경 수치라 결과 검증은 하지 않음
    @Test
    fun `insert 비교 - saveAll vs batchInsert`() {
        val saveAllMs = measureTimeMillis {
            writerRepository.saveAll(writers(BENCHMARK_SIZE))
            em.flush()
        }
        val batchMs = measureTimeMillis { writerBatchService.batchInsert(writers(BENCHMARK_SIZE)) }

        println("[insert $BENCHMARK_SIZE 건] saveAll: ${saveAllMs}ms, batchInsert: ${batchMs}ms")
    }

    // 시간 측정 로그 확인용 — 더티 체킹 쪽은 엔티티 조회 시간까지 포함
    @Test
    fun `update 비교 - 더티 체킹 vs batchUpdate`() {
        writerBatchService.batchInsert(writers(BENCHMARK_SIZE * 2))
        val ids = writerRepository.findAll().map { it.id!! }
        em.clear()
        val dirtyCheckIds = ids.take(BENCHMARK_SIZE).toSet()
        val batchIds = ids.drop(BENCHMARK_SIZE)

        val dirtyCheckMs = measureTimeMillis {
            writerRepository.findAllById(dirtyCheckIds).forEach { it.changeName("dirty-${it.id}") }
            em.flush()
        }
        val batchMs = measureTimeMillis {
            writerBatchService.batchUpdate(batchIds.map { WriterUpdate(it, "batch-$it") })
        }

        println("[update $BENCHMARK_SIZE 건] 더티 체킹: ${dirtyCheckMs}ms, batchUpdate: ${batchMs}ms")
    }

    private fun writers(count: Int) =
        (1..count).map { Writer("writer-$it", "writer$it@example.com", 1, 1.0, true) }

    companion object {
        private const val BENCHMARK_SIZE = 10_000
    }
}
