package com.example.query.blog.batch.writer

import com.example.query.blog.batch.writer.QWriter.writer as qWriter
import com.example.query.blog.batch.writer.dto.WriterUpdate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.sql.Configuration
import com.querydsl.sql.MySQLTemplates
import com.querydsl.sql.RelationalPathBase
import com.querydsl.sql.SQLQueryFactory
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

@Service
class WriterBatchService(
    dataSource: DataSource,
) {
    private val sqlQueryFactory = SQLQueryFactory(
        Configuration(MySQLTemplates()),
        // SQLQueryFactory가 @Transactional이 잡아 둔 JPA 커넥션을 그대로 쓰게 하여 롤백, 원자성을 보장
        TransactionAwareDataSourceProxy(dataSource),
    )

    // JPA Q클래스는 EntityPathBase라 테이블로 쓸 수 없어 테이블 경로만 따로 생성
    private val writerTable = RelationalPathBase(Writer::class.java, "writer", null, "blog_writer")

    // WHERE 절의 컬럼 경로를 실제 테이블명(blog_writer)에 맞추기 위해 별도 경로 생성
    private val writerId = Expressions.numberPath(Long::class.java, writerTable, "id")

    @Transactional
    fun batchInsert(writers: List<Writer>) {
        // 배치가 비어 있으면 execute()가 값 없는 insert를 만들 수 있어 미리 반환
        if (writers.isEmpty()) return

        val insert = sqlQueryFactory.insert(writerTable)
        writers.forEach {
            insert.set(qWriter.name, it.name)
                .set(qWriter.email, it.email)
                .set(qWriter.score, it.score)
                .set(qWriter.reputation, it.reputation)
                .set(qWriter.active, it.active)
                .addBatch()
        }
        insert.execute()
    }

    @Transactional
    fun batchUpdate(updates: List<WriterUpdate>): Long {
        if (updates.isEmpty()) return 0

        val update = sqlQueryFactory.update(writerTable)
        updates.forEach {
            // where가 빠진 채 addBatch() 하면 테이블 전체가 갱신되므로 항상 id 조건 필요
            update.set(qWriter.name, it.name)
                .where(writerId.eq(it.id))
                .addBatch()
        }
        return update.execute()
    }
}

/* 로컬, 10,000건 특정 결과
*
* insert (saveAll) - JPA : 685ms , Querydsl-SQL 배치 : 66ms
* update (더티 체킹, 엔티티 조회 포함) - JPA : 1075ms , Querydsl-SQL 배치 : 234ms
* */