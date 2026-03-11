package com.woo.server.domain.card.controller

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.dto.CardMonthlyBillListResponse
import com.woo.server.domain.card.dto.CardMonthlyBillResponse
import com.woo.server.domain.card.service.CardMonthlyBillService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 월별 청구서 컨트롤러
 *
 * 월별 청구서 조회 API를 제공합니다.
 * 청구서 등록은 기존 POST /api/v1/card/transactions 엔드포인트에서
 * 발신번호 기반으로 자동 라우팅됩니다.
 */
@Tag(name = "월별 청구서", description = "카드사 월별 청구서 조회 API")
@RestController
@RequestMapping("/api/v1/card/monthly-bills")
class CardMonthlyBillController(
    private val cardMonthlyBillService: CardMonthlyBillService
) {

    /**
     * 월별 청구서를 조회합니다 (합계 포함).
     *
     * @param year 년도
     * @param month 월
     * @param cardCompany 카드사 (선택)
     * @return 청구서 목록 + 합계
     */
    @Operation(
        summary = "월별 청구서 조회",
        description = "년월 기준으로 청구서를 조회합니다. 카드사 필터링 가능. 합계 포함."
    )
    @GetMapping
    fun getMonthlyBills(
        @Parameter(description = "년도", example = "2026") @RequestParam year: Int,
        @Parameter(description = "월", example = "2") @RequestParam month: Int,
        @Parameter(description = "카드사 코드 (선택)", example = "SAMSUNG") @RequestParam(required = false) cardCompany: CardCompany?
    ): ResponseEntity<CardMonthlyBillListResponse> {
        // month 파라미터 범위 검증 (1~12)
        require(month in 1..12) { "월은 1~12 사이의 값이어야 합니다: $month" }
        val result = cardMonthlyBillService.getBillsByMonth(year, month, cardCompany)
        return ResponseEntity.ok(result)
    }

    /**
     * 전체 청구서를 조회합니다.
     *
     * @return 전체 청구서 목록
     */
    @Operation(summary = "전체 청구서 조회", description = "등록된 모든 청구서를 조회합니다.")
    @GetMapping("/all")
    fun getAllMonthlyBills(): ResponseEntity<List<CardMonthlyBillResponse>> {
        val bills = cardMonthlyBillService.getAllBills()
        return ResponseEntity.ok(bills)
    }
}
