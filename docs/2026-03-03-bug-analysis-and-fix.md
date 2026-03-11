# 프로젝트 전수 분석 - 버그 목록 및 수정 현황

> **분석일**: 2026-03-03
> **분석 범위**: 백엔드 서비스/파서/컨트롤러/DTO, 프론트엔드(PC/모바일) 전체

---

## 수정 완료

### #3 날짜 종료 시점 나노초 구간 누락 (심각도: 높음)

- **문제**: `atTime(23, 59, 59)`로 종료 시간을 설정하여 `23:59:59.000000001` ~ `23:59:59.999999999` 구간의 거래가 모든 기간 집계에서 누락
- **영향**: CardLimitService, CardTransactionService, TagStatisticsService, CardUsageSummaryService의 모든 기간 집계
- **수정**: `atTime(23, 59, 59)` → `atTime(23, 59, 59, 999_999_999)` (15곳)
- **수정 파일**:
  - `CardLimitService.kt` (8곳)
  - `CardTransactionService.kt` (1곳)
  - `TagStatisticsService.kt` (4곳)
  - `CardUsageSummaryService.kt` (2곳)

### #4 중복 거래 검증에 cardLastFourDigits 미포함 (심각도: 높음)

- **문제**: 중복 체크 조건에 카드 끝4자리가 없어, 같은 카드사의 다른 카드에서 동일 금액/시간/가맹점 거래 시 두 번째 거래가 저장되지 않음
- **영향**: UniqueConstraint와 중복 체크 로직 모두 동일한 문제
- **수정**: Repository 메서드에 `cardLastFourDigits` 파라미터 추가, Service 호출부 수정, UniqueConstraint 변경
- **수정 파일**:
  - `CardTransactionRepository.kt` - 중복 체크 메서드 시그니처 변경
  - `CardTransactionService.kt` - 호출 시 `cardLastFourDigits` 전달
  - `CardTransaction.kt` - UniqueConstraint에 `cardLastFourDigits` 추가
  - `docs/sql/card-transaction-unique-constraint-migration.sql` - DDL 마이그레이션 SQL 생성
- **참고**: DB에 마이그레이션 SQL 수동 실행 필요

### #5 태그 통계 쿼리에 cancelled 조건 없음 (심각도: 높음)

- **문제**: `TransactionTagRepository`의 6개 집계 쿼리에 `cancelled = false` 조건이 없어 취소된 거래 금액이 태그 통계에 포함됨
- **영향**: 태그별 금액 요약, 월간 추이, 태그 ID별 합산 통계 전체
- **수정**: 6개 쿼리에 취소 거래 제외 조건 추가
- **수정 파일**:
  - `TransactionTagRepository.kt`
    - `sumAmountByTagAndPeriod` (JPQL): `AND tt.transaction.cancelled = false`
    - `sumAmountByTagAndPeriodAndType` (JPQL): `AND tt.transaction.cancelled = false`
    - `sumAmountByTagAndMonth` (Native): `AND ct.cancelled = false`
    - `sumAmountByTagAndMonthAndType` (Native): `AND ct.cancelled = false`
    - `sumAmountByTagIdsAndPeriod` (JPQL): `AND tt.transaction.cancelled = false`
    - `sumAmountByTagIdsAndMonth` (Native): `AND ct.cancelled = false`

### #6 글로벌 예외 핸들러 부재 (심각도: 높음)

- **문제**: `@RestControllerAdvice`가 없어 `IllegalArgumentException`, `NoSuchElementException` 등 비즈니스 예외가 500 에러로 응답
- **영향**: 클라이언트가 오류 원인을 파악할 수 없고 프론트엔드 에러 핸들링 불가
- **수정**: `GlobalExceptionHandler.kt` 신규 생성
- **수정 파일**:
  - `common/config/GlobalExceptionHandler.kt` (신규)
    - `IllegalArgumentException` → 400 Bad Request
    - `IllegalStateException` → 409 Conflict
    - `NoSuchElementException` → 404 Not Found
    - `MethodArgumentNotValidException` → 400 Bad Request (Bean Validation)
    - `DateTimeException` → 400 Bad Request
    - 기타 `Exception` → 500 Internal Server Error
  - 응답 형식: `{ "error": "메시지", "status": 코드 }`

### #7 프론트엔드 카드 필터 타입 불일치 (심각도: 높음)

- **문제**: `route.query.card`는 항상 문자열인데, `selectedCard`는 숫자 ID로 비교 (`===`)하여 대시보드→거래내역 카드 필터가 미작동
- **영향**: 대시보드에서 카드 클릭 후 거래 내역 페이지 이동 시 카드 필터 동작 안 함
- **수정**: `Number(route.query.card)`로 타입 변환
- **수정 파일**:
  - `front/src/views/TransactionListView.vue`
  - `front-mobile/src/views/TransactionListView.vue`

### #8 month 파라미터 범위 검증 없음 (심각도: 높음)

- **문제**: `month` 파라미터에 1~12 범위 검증이 없어 잘못된 값 입력 시 `DateTimeException` 발생 → 500 에러
- **영향**: `CardLimitController`, `CardMonthlyBillController`의 월별 조회 API
- **수정**: `require(month in 1..12)` 검증 추가 → 글로벌 예외 핸들러가 400 Bad Request로 응답
- **수정 파일**:
  - `CardLimitController.kt` - `getAll()` 메서드에 month 범위 검증 추가
  - `CardMonthlyBillController.kt` - `getMonthlyBills()` 메서드에 month 범위 검증 추가

---

## 대기 중 (미수정)

### 심각도: 높음

| # | 분류 | 위치 | 내용 |
|---|------|------|------|
| 1 | 파서 버그 | `HanaCardParser.kt:217` | 취소 문자에서 `cancelled = true` 미설정 → 취소 거래가 일반 거래로 저장됨 |
| 2 | 파서 버그 | `HanaCardParser.kt:217` | 취소 시 `[취소]` 접두사가 merchantName에 추가되어 원본 거래 매칭 불가 → 취소 처리 완전 실패 |

### 심각도: 중간

| # | 분류 | 위치 | 내용 |
|---|------|------|------|
| 9 | 파서 버그 | `HanaCardParser.kt:106` | `message.contains("취소")`로 단순 판별 → 사용처명에 "취소" 포함 시 오분기 |
| 10 | 파서 버그 | `SamsungCardParser.kt:261` | 후불교통에서 `LocalDateTime.now()` 사용 → 월 정보 무시, 중복 체크 실패 |
| 11 | 파서 누락 | `KbCardParser.kt` | `canParse()`에 "승인" 필수 → KB카드 취소 문자 파싱 불가 |
| 12 | 비즈니스 로직 | `AutoPaymentService.kt:62-75` | `AutoPayment.creditCard`가 `val` → 자동결제 카드 변경 불가 |
| 13 | 비즈니스 로직 | `TagService.kt:173-205` | `setMainTag`에서 Tag 엔티티의 `tagType`과 TransactionTag의 `tagType` 불일치 가능 |
| 14 | 동시성 | `CardTransactionService.kt:104-114` | 중복 체크 Race Condition + UniqueConstraint 예외 미처리 → 500 에러 |
| 15 | 성능 | 서비스 전체 | 전체 거래 조회에 페이징 미적용 → 데이터 누적 시 성능 저하 |
| 16 | 성능 | `CardUsageSummaryService.kt` | MONTHLY 한도에서 동일 기간 DB 쿼리 2회 실행 (2N 문제) |
| 17 | 성능 | `CardLimitService.kt:80-89` | `getAll()`에서 N+1 쿼리 패턴 |
| 18 | SPA 라우팅 | `WebConfig.kt` | 존재하지 않는 API 경로(`/api/v1/...`) 요청 시 `index.html` 반환 (JSON 대신 HTML) |
| 19 | 프론트 일관성 | PC+모바일 `TransactionListView` | `addTag` 후 전체 목록 미갱신 (`changeMainTag`/`removeTag`와 패턴 불일치) |
| 20 | 프론트 버그 | PC+모바일 `TransactionListView` | `toISOString()` UTC 변환 → 한국 타임존(UTC+9)에서 월초 날짜 오류 가능 |
| 21 | 프론트 에러 | PC+모바일 `DashboardView` | `Promise.all` 사용 → 1개 API 실패 시 대시보드 전체 빈 상태 |
| 22 | 입력 검증 | `CreditCardRequest` | `lastFourDigits`에 `@Size`/`@Pattern` 없음 → 잘못된 형식 저장 가능 |
| 23 | 입력 검증 | `CardLimitController`, `CardTransactionController` | `year`만 입력 시 `month` 무시되고 전체 조회로 폴백 |
| 24 | 프론트 논리 | PC+모바일 `TagAmountSummaryView` | "태그 선택(MAIN)" UI인데 전체 태그(MAIN+DETAIL) 로드 |

### 심각도: 낮음

| # | 분류 | 위치 | 내용 |
|---|------|------|------|
| 25 | 코드 중복 | `CardLimitService` / `CreditCardService` | `buildCardDigitVariants` 동일 메서드 중복 |
| 26 | 엔티티 | `CardMonthlyBill.kt` | `updatedAt`이 `val` (불변) → 수정 기능 추가 시 문제 |
| 27 | API 설계 | `TagController` | URL에 `tags` 중복 (`/tags/transactions/.../tags`) |
| 28 | HTTP 상태코드 | `CardLimitController`, `CreditCardController` | `createOrUpdate` 수정 시에도 항상 201 반환 |
| 29 | 미사용 필드 | `TransactionTagRequest.tagType` | API 요청에 포함되나 완전히 무시됨 |
| 30 | 프론트 라우팅 | PC+모바일 `router` | 404 catch-all 라우트 없음 |
| 31 | 프론트 기능 불일치 | PC vs 모바일 `TagListView` | 모바일만 다이얼로그 내 태그 유형 변경 가능 |
| 32 | 프론트 에러 | PC+모바일 `CreditCardListView` | `onMounted`에서 `fetchSupportedCompanies` try-catch 누락 |
| 33 | 파서 코드 | 모든 파서 | `split("\n", "\r\n")` 순서 문제 (`\r\n`이 사실상 매칭 안 됨) |

---

## 진행 현황 요약

| 심각도 | 전체 | 수정 완료 | 대기 중 |
|--------|------|-----------|---------|
| 높음 | 8 | 6 (#3, #4, #5, #6, #7, #8) | 2 (#1, #2) |
| 중간 | 16 | 0 | 16 (#9~#24) |
| 낮음 | 9 | 0 | 9 (#25~#33) |
| **합계** | **33** | **6** | **27** |

---

## 수정 시 DDL 마이그레이션 필요 항목

| 파일 | 내용 | 상태 |
|------|------|------|
| `docs/sql/card-transaction-unique-constraint-migration.sql` | #4 UniqueConstraint에 `cardLastFourDigits` 추가 | SQL 생성 완료, DB 실행 필요 |

---

## 다음 우선 수정 추천

1. **#1, #2 하나카드 취소 처리** — 취소 기능이 완전히 동작하지 않는 상태
2. **#9, #11 파서 취소 감지 개선** — 카드사별 취소 문자 파싱 정확도 향상
3. **#14 중복 체크 Race Condition** — UniqueConstraint 예외 미처리
