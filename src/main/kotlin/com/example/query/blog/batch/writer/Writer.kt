package com.example.query.blog.batch.writer

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "blog_writer")
class Writer(
    name: String,
    val email: String,
    val score: Int,
    val reputation: Double,
    val active: Boolean,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    var name: String = name
        protected set

    fun changeName(name: String) {
        this.name = name
    }
}