package com.example.query.blog.member

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// reserve_project DB를 공유하므로 기존 member 테이블과 충돌하지 않도록 접두어를 붙임
@Entity
@Table(name = "blog_member")
class Member(
    val name: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
