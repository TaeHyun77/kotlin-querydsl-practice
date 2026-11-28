package com.example.query.blog.batch.writer

import org.springframework.data.jpa.repository.JpaRepository

interface WriterRepository : JpaRepository<Writer, Long>
