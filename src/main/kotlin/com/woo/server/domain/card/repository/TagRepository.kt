package com.woo.server.domain.card.repository

import com.woo.server.domain.card.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 태그 리포지토리
 *
 * 태그의 CRUD 및 이름 기반 조회를 제공합니다.
 */
@Repository
interface TagRepository : JpaRepository<Tag, Long> {

    /** 태그 이름으로 조회 */
    fun findByName(name: String): Tag?

    /** 태그 이름 존재 여부 확인 */
    fun existsByName(name: String): Boolean
}
