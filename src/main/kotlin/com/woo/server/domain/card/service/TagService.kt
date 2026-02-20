package com.woo.server.domain.card.service

import com.woo.server.domain.card.dto.TagRequest
import com.woo.server.domain.card.dto.TagResponse
import com.woo.server.domain.card.entity.Tag
import com.woo.server.domain.card.entity.TransactionTag
import com.woo.server.domain.card.repository.CardTransactionRepository
import com.woo.server.domain.card.repository.TagRepository
import com.woo.server.domain.card.repository.TransactionTagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 태그 서비스
 *
 * 태그의 CRUD 및 거래-태그 할당/해제 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class TagService(
    private val tagRepository: TagRepository,
    private val transactionTagRepository: TransactionTagRepository,
    private val cardTransactionRepository: CardTransactionRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 태그를 생성합니다.
     *
     * @param request 태그 생성 요청
     * @return 생성된 태그 응답
     * @throws IllegalArgumentException 이름이 중복된 경우
     */
    @Transactional
    fun create(request: TagRequest): TagResponse {
        if (tagRepository.existsByName(request.name)) {
            throw IllegalArgumentException("이미 존재하는 태그 이름입니다: ${request.name}")
        }
        val tag = Tag(
            name = request.name,
            color = request.color
        )
        val saved = tagRepository.save(tag)
        log.info("태그 생성 완료: ID=${saved.id}, name=${saved.name}")
        return TagResponse.from(saved)
    }

    /** 전체 태그를 조회합니다. */
    fun getAll(): List<TagResponse> {
        return tagRepository.findAll().map { TagResponse.from(it) }
    }

    /** 단건 태그를 조회합니다. */
    fun getById(id: Long): TagResponse? {
        return tagRepository.findById(id)
            .map { TagResponse.from(it) }
            .orElse(null)
    }

    /**
     * 태그를 수정합니다.
     *
     * @param id 태그 ID
     * @param request 태그 수정 요청
     * @return 수정된 태그 응답
     * @throws IllegalArgumentException 태그가 없거나 이름이 중복된 경우
     */
    @Transactional
    fun update(id: Long, request: TagRequest): TagResponse {
        val tag = tagRepository.findById(id)
            .orElseThrow { IllegalArgumentException("태그를 찾을 수 없습니다: ID=$id") }

        // 다른 태그와 이름이 중복되는지 확인
        val existing = tagRepository.findByName(request.name)
        if (existing != null && existing.id != id) {
            throw IllegalArgumentException("이미 존재하는 태그 이름입니다: ${request.name}")
        }

        tag.name = request.name
        tag.color = request.color
        tag.updatedAt = LocalDateTime.now()

        val saved = tagRepository.save(tag)
        log.info("태그 수정 완료: ID=${saved.id}, name=${saved.name}")
        return TagResponse.from(saved)
    }

    /**
     * 태그를 삭제합니다.
     * 연관된 거래-태그 매핑을 먼저 삭제한 후 태그를 삭제합니다.
     *
     * @param id 태그 ID
     * @return 삭제 성공 여부
     */
    @Transactional
    fun delete(id: Long): Boolean {
        if (!tagRepository.existsById(id)) {
            return false
        }
        transactionTagRepository.deleteByTagId(id)
        tagRepository.deleteById(id)
        log.info("태그 삭제 완료: ID=$id")
        return true
    }

    /**
     * 거래에 태그를 할당합니다.
     *
     * @param transactionId 거래 ID
     * @param tagId 태그 ID
     * @return 할당된 태그 응답
     * @throws IllegalArgumentException 거래 또는 태그가 없는 경우
     * @throws IllegalStateException 이미 할당된 경우
     */
    @Transactional
    fun addTagToTransaction(transactionId: Long, tagId: Long): TagResponse {
        val transaction = cardTransactionRepository.findById(transactionId)
            .orElseThrow { IllegalArgumentException("거래를 찾을 수 없습니다: ID=$transactionId") }
        val tag = tagRepository.findById(tagId)
            .orElseThrow { IllegalArgumentException("태그를 찾을 수 없습니다: ID=$tagId") }

        if (transactionTagRepository.existsByTransactionIdAndTagId(transactionId, tagId)) {
            throw IllegalStateException("이미 할당된 태그입니다")
        }

        val transactionTag = TransactionTag(
            transaction = transaction,
            tag = tag
        )
        transactionTagRepository.save(transactionTag)
        log.info("거래-태그 할당 완료: transactionId=$transactionId, tagId=$tagId")
        return TagResponse.from(tag)
    }

    /**
     * 거래에서 태그를 해제합니다.
     *
     * @param transactionId 거래 ID
     * @param tagId 태그 ID
     */
    @Transactional
    fun removeTagFromTransaction(transactionId: Long, tagId: Long) {
        transactionTagRepository.deleteByTransactionIdAndTagId(transactionId, tagId)
        log.info("거래-태그 해제 완료: transactionId=$transactionId, tagId=$tagId")
    }

    /**
     * 거래에 할당된 태그 목록을 조회합니다.
     *
     * @param transactionId 거래 ID
     * @return 태그 목록
     */
    fun getTagsByTransactionId(transactionId: Long): List<TagResponse> {
        return transactionTagRepository.findByTransactionIdWithTag(transactionId)
            .map { TagResponse.from(it.tag) }
    }
}
