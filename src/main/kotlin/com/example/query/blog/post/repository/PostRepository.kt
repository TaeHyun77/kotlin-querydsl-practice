package com.example.query.blog.post.repository

import com.example.query.blog.post.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long>, PostCustomRepository
