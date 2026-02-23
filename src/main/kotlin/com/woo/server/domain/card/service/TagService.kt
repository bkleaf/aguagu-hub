package com.woo.server.domain.card.service

import com.woo.server.common.enums.TagType
import com.woo.server.domain.card.dto.TagRequest
import com.woo.server.domain.card.dto.TagResponse
import com.woo.server.domain.card.dto.TransactionTagResponse
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
 * 태그 유형(MAIN/DETAIL)에 따라 주요 태그와 세부 태그를 관리합니다.
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
     * @param tagType 태그 유형 (MAIN/DETAIL, 기본값: DETAIL)
     * @return 할당된 태그 응답
     * @throws IllegalArgumentException 거래 또는 태그가 없는 경우
     * @throws IllegalStateException 이미 할당된 경우 또는 MAIN 태그가 이미 존재하는 경우
     */
    @Transactional
    fun addTagToTransaction(transactionId: Long, tagId: Long, tagType: TagType = TagType.DETAIL): TransactionTagResponse {
        val transaction = cardTransactionRepository.findById(transactionId)
            .orElseThrow { IllegalArgumentException("거래를 찾을 수 없습니다: ID=$transactionId") }
        val tag = tagRepository.findById(tagId)
            .orElseThrow { IllegalArgumentException("태그를 찾을 수 없습니다: ID=$tagId") }

        if (transactionTagRepository.existsByTransactionIdAndTagId(transactionId, tagId)) {
            throw IllegalStateException("이미 할당된 태그입니다")
        }

        // MAIN 태그 중복 방지
        if (tagType == TagType.MAIN && transactionTagRepository.existsByTransactionIdAndTagType(transactionId, TagType.MAIN)) {
            throw IllegalStateException("이미 주요 태그가 할당되어 있습니다. setMainTag를 사용해주세요.")
        }

        val transactionTag = TransactionTag(
            transaction = transaction,
            tag = tag,
            tagType = tagType
        )
        val saved = transactionTagRepository.save(transactionTag)
        log.info("거래-태그 할당 완료: transactionId=$transactionId, tagId=$tagId, tagType=$tagType")
        return TransactionTagResponse.from(saved)
    }

    /**
     * 거래의 주요 태그를 설정합니다.
     *
     * 기존 MAIN 태그가 있으면 DETAIL로 변경한 후, 지정된 태그를 MAIN으로 설정합니다.
     * 해당 태그가 아직 거래에 할당되지 않은 경우 새로 할당합니다.
     *
     * @param transactionId 거래 ID
     * @param tagId 주요 태그로 설정할 태그 ID
     * @return 설정된 주요 태그 응답
     */
    @Transactional
    fun setMainTag(transactionId: Long, tagId: Long): TransactionTagResponse {
        val transaction = cardTransactionRepository.findById(transactionId)
            .orElseThrow { IllegalArgumentException("거래를 찾을 수 없습니다: ID=$transactionId") }
        val tag = tagRepository.findById(tagId)
            .orElseThrow { IllegalArgumentException("태그를 찾을 수 없습니다: ID=$tagId") }

        // 기존 MAIN 태그를 DETAIL로 변경
        val existingMainTags = transactionTagRepository.findByTransactionIdAndTagType(transactionId, TagType.MAIN)
        existingMainTags.forEach { it.tagType = TagType.DETAIL }
        if (existingMainTags.isNotEmpty()) {
            transactionTagRepository.saveAll(existingMainTags)
        }

        // 해당 태그가 이미 거래에 할당되어 있는지 확인
        val existingTag = transactionTagRepository.findByTransactionIdWithTag(transactionId)
            .find { it.tag.id == tagId }

        val result = if (existingTag != null) {
            // 이미 할당된 경우 유형만 MAIN으로 변경
            existingTag.tagType = TagType.MAIN
            transactionTagRepository.save(existingTag)
        } else {
            // 새로 할당
            val transactionTag = TransactionTag(
                transaction = transaction,
                tag = tag,
                tagType = TagType.MAIN
            )
            transactionTagRepository.save(transactionTag)
        }

        log.info("주요 태그 설정 완료: transactionId=$transactionId, tagId=$tagId")
        return TransactionTagResponse.from(result)
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
     * 거래에 할당된 태그 목록을 조회합니다 (태그 유형 포함).
     *
     * @param transactionId 거래 ID
     * @return 태그 목록 (유형 포함)
     */
    fun getTagsByTransactionId(transactionId: Long): List<TransactionTagResponse> {
        return transactionTagRepository.findByTransactionIdWithTag(transactionId)
            .map { TransactionTagResponse.from(it) }
    }
}
