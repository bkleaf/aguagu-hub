package com.woo.server.domain.card.repository

import com.woo.server.domain.card.entity.TransactionTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * 거래-태그 매핑 리포지토리
 *
 * 거래와 태그의 다대다 관계를 관리합니다.
 */
@Repository
interface TransactionTagRepository : JpaRepository<TransactionTag, Long> {

    /** 거래 ID로 매핑 목록 조회 (태그를 JOIN FETCH하여 N+1 방지) */
    @Query("SELECT tt FROM TransactionTag tt JOIN FETCH tt.tag WHERE tt.transaction.id = :transactionId")
    fun findByTransactionIdWithTag(transactionId: Long): List<TransactionTag>

    /** 여러 거래 ID로 매핑 목록 일괄 조회 (태그를 JOIN FETCH하여 N+1 방지) */
    @Query("SELECT tt FROM TransactionTag tt JOIN FETCH tt.tag WHERE tt.transaction.id IN :transactionIds")
    fun findByTransactionIdInWithTag(transactionIds: List<Long>): List<TransactionTag>

    /** 거래 ID로 매핑 목록 조회 */
    fun findByTransactionId(transactionId: Long): List<TransactionTag>

    /** 태그 ID로 매핑 목록 조회 */
    fun findByTagId(tagId: Long): List<TransactionTag>

    /** 거래-태그 매핑 삭제 */
    @Modifying
    fun deleteByTransactionIdAndTagId(transactionId: Long, tagId: Long)

    /** 태그 ID로 매핑 전체 삭제 */
    @Modifying
    fun deleteByTagId(tagId: Long)

    /** 거래-태그 매핑 존재 여부 확인 */
    fun existsByTransactionIdAndTagId(transactionId: Long, tagId: Long): Boolean
}
