package com.example.query.blog.paging.post.repository

import com.example.query.blog.paging.post.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long>, PostCustomRepository
