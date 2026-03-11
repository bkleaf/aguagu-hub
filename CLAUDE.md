# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**aguagu-hub**는 카드 결제 문자(SMS)를 수신·파싱하여 DB에 저장하고 조회하는 Spring Boot 애플리케이션이다.

- **언어**: Kotlin 2.2.21 (JSR-305 strict 모드)
- **프레임워크**: Spring Boot 4.0.1 (Web MVC, JPA, Validation)
- **빌드**: Gradle Kotlin DSL, Java 25 툴체인 (JVM 타겟 24)
- **데이터베이스**: PostgreSQL (Hibernate ddl-auto=validate, 스키마: aguagu)
- **API 문서**: Swagger UI (`/swagger-ui/index.html`) — springdoc-openapi 3.0.0-M1
- **패키지 루트**: `com.woo.server`
- **프론트엔드 (PC)**: Vue 3 + Vuetify 3 + Vite (`front/` 디렉토리, 빌드 결과물은 `src/main/resources/static/`)
- **프론트엔드 (모바일)**: Vue 3 + Vuetify 3 + Vite (`front-mobile/` 디렉토리, 빌드 결과물은 `src/main/resources/static-mobile/`)

## 개발 명령어

```bash
./gradlew build                    # 빌드
./gradlew bootRun                  # 실행 (port 8080)
./gradlew test                     # 전체 테스트
./gradlew test --tests "*Pattern*" # 특정 테스트
./gradlew clean build              # 클린 빌드
```

DB 연결은 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 오버라이드 가능.
Telegram 알림은 환경변수 `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`, `TELEGRAM_ENABLED`, `TELEGRAM_WEBHOOK_URL`로 설정.

## 아키텍처

도메인 기반 패키지 구조를 따른다. 현재 `card` 도메인과 `common` 공통 모듈이 존재.

```
com.woo.server
├── common/
│   ├── config/
│   │   ├── TelegramProperties              # Telegram 설정 속성 (@ConfigurationProperties)
│   │   ├── TelegramBotConfig               # Telegram 웹훅/명령어 초기화 (@PostConstruct)
│   │   ├── OpenApiConfig                   # Swagger/OpenAPI 설정
│   │   └── WebConfig                       # SPA(Vue Router) 폴백 설정
│   ├── enums/
│   │   ├── CardCompany                     # 카드사 열거형 (SAMSUNG, HYUNDAI, KB 등 10개)
│   │   ├── LimitType                       # 한도 유형 열거형 (MONTHLY, YEARLY, CUSTOM)
│   │   └── TagType                         # 태그 유형 열거형 (MAIN, DETAIL)
│   └── notification/
│       ├── TelegramNotificationService     # Telegram 거래 알림 서비스
│       ├── TelegramCommandService          # Telegram 봇 명령어 처리 (/limit, /card, /start)
│       └── TelegramWebhookController       # Telegram 웹훅 수신 컨트롤러
└── domain/card/
    ├── controller/
    │   ├── CardTransactionController        # 거래 REST API (/api/v1/card/transactions)
    │   ├── CardLimitController              # 한도 REST API (/api/v1/card/limits)
    │   ├── CreditCardController             # 신용카드 관리 REST API (/api/v1/card/credit-cards)
    │   └── AutoPaymentController            # 자동결제 관리 REST API (/api/v1/card/auto-payments)
    ├── service/
    │   ├── CardTransactionService           # 문자 처리, 중복 확인, 조회
    │   ├── CardLimitService                 # 한도 CRUD, 기간별 사용액 집계 (월간/연간/커스텀, 자동결제 포함)
    │   ├── CardUsageSummaryService          # 카드별 사용 현황 집계 (대시보드용, 한도 유형별 기간 기준)
    │   ├── CreditCardService                # 신용카드 등록, 조회, 삭제, 마스킹 매칭
    │   └── AutoPaymentService               # 자동결제 CRUD, 활성 금액 합산
    ├── dto/                                 # Request/Response DTO (Transaction, Limit, CreditCard, AutoPayment, UsageSummary)
    ├── entity/
    │   ├── CardTransaction                  # JPA 엔티티 (card_transactions 테이블, 파싱 성공 거래)
    │   ├── CardParseFailure                 # JPA 엔티티 (card_parse_failures 테이블, 파싱 실패 내역)
    │   ├── CardLimit                        # JPA 엔티티 (card_limits 테이블)
    │   ├── CreditCard                       # JPA 엔티티 (credit_cards 테이블)
    │   └── AutoPayment                      # JPA 엔티티 (auto_payments 테이블)
    ├── repository/
    │   ├── CardTransactionRepository        # Spring Data JPA
    │   ├── CardParseFailureRepository       # Spring Data JPA
    │   ├── CardLimitRepository              # Spring Data JPA
    │   ├── CreditCardRepository             # Spring Data JPA
    │   └── AutoPaymentRepository            # Spring Data JPA
    └── parser/
        ├── CardMessageParser                # 파서 인터페이스 + ParseResult
        ├── SamsungCardParser                # 삼성카드 문자 파싱 구현체
        ├── HanaCardParser                   # 하나카드 문자 파싱 구현체 (여러 줄 승인/한 줄 승인/취소, 마스킹 지원)
        ├── KbCardParser                     # KB카드 문자 파싱 구현체 (상세형/간략형/중간형)
        └── CardParserFactory                # 카드사별 파서 선택 팩토리
```

### 핵심 흐름

1. SMS 문자가 `POST /api/v1/card/transactions`로 수신됨
2. `CardParserFactory`가 카드사를 식별하고 적절한 파서 선택
3. 파서가 금액, 거래일시, 사용처, 누적금액 등을 추출 (`ParseResult`)
4. **파싱 실패 시**: `CardParseFailure` 엔티티로 저장, Telegram 실패 알림 전송
5. **파싱 성공 시**:
   - `CreditCardService`로 등록된 신용카드 매칭 (마스킹 카드번호 LIKE 역매칭 지원)
   - `CardTransactionService`가 중복 확인 후 `CardTransaction` 엔티티로 저장
   - `CardLimitService`가 한도 정보 조회 (월간/연간/커스텀, 활성 자동결제 금액 포함)
   - `TelegramNotificationService`가 정산기간 기준 누적 사용액 및 한도 잔여 정보 포함 알림 전송 (자동결제 포함 시 표시)

### 새 카드사 파서 추가 시

1. `CardCompany` 열거형에 카드사 추가
2. `CardMessageParser` 인터페이스를 구현하는 파서 클래스 작성
3. `CardParserFactory`에 해당 파서 등록

### Telegram 봇 연동

- **웹훅 방식**: 앱 시작 시 `TelegramBotConfig`가 웹훅 URL 등록 및 봇 명령어 메뉴 설정
- **명령어**: `/limit` (한도 조회), `/card` (등록 카드 조회), `/start` (도움말)
- **알림**: 거래 성공 시 정산기간 기준 누적 사용액 + 한도 잔여 정보 포함 (카드사 원본 누적금액이 아닌 자체 합산액 사용), 파싱 실패 시 실패 사유 전송

### 프론트엔드

경로 기반으로 PC / 모바일 프론트엔드를 분리하여 서빙한다.

| 경로 | 프로젝트 | 빌드 출력 | 설명 |
|------|----------|-----------|------|
| `/*` | `front/` | `src/main/resources/static/` | PC 전용 (사이드바 레이아웃, 테이블 기반) |
| `/m/*` | `front-mobile/` | `src/main/resources/static-mobile/` | 모바일 전용 (하단 네비게이션, 카드형 리스트) |

- 동일한 백엔드 API(`/api/v1/...`)를 공유
- `WebConfig`가 SPA 라우팅을 위해 `/m/**` → `static-mobile/index.html`, `/**` → `static/index.html`로 폴백

#### PC (`front/`)
- 페이지: 대시보드(카드별 사용 현황), 거래내역, 거래등록, 한도관리, 신용카드관리, 자동결제관리, 월별청구서
- 빌드: `cd front && npm run build`

#### 모바일 (`front-mobile/`)
- 페이지: 대시보드(카드별 사용 현황), 거래내역, 한도, 월별청구서, 더보기(자동결제/문자등록/카드관리)
- 하단 네비게이션: 대시보드 / 거래 / 한도 / 청구서 / 더보기
- 빌드: `cd front-mobile && npm run build`
- Vite base: `/m/`, Vue Router base: `/m/`
- UI: 하단 네비게이션(`v-bottom-navigation`), 카드형 리스트, 풀스크린 다이얼로그

### Kotlin/JPA 설정

- **All-open 플러그인**: `@Entity`, `@MappedSuperclass`, `@Embeddable` 클래스를 자동으로 open 처리
- **No-arg 플러그인**: JPA 엔티티에 기본 생성자 자동 생성
- Lombok이 의존성에 포함되어 있으나, Kotlin data class 사용이 권장됨

## 테스트

```bash
./gradlew test                                          # 전체 테스트
./gradlew test --tests "*ControllerTest*"               # 컨트롤러 단위 테스트 (DB 불필요)
./gradlew test --tests "*CardTransactionIntegrationTest*" # 통합 테스트 (DB+Telegram 필요)
```

테스트 구조:

```
src/test/kotlin/com/woo/server/
├── domain/card/
│   ├── controller/
│   │   ├── CardTransactionControllerTest    # 거래 API 단위 테스트 (9개, @WebMvcTest)
│   │   └── CardLimitControllerTest          # 한도 API 단위 테스트 (6개, @WebMvcTest)
│   └── CardTransactionIntegrationTest       # 전체 흐름 통합 테스트 (@SpringBootTest)
└── AguaguHubApplicationTests                # 컨텍스트 로딩 테스트
```

> **Spring Boot 4.0 주의**: `@WebMvcTest`, `@AutoConfigureMockMvc`의 패키지가
> `o.s.boot.webmvc.test.autoconfigure`로 변경됨 (3.x: `o.s.boot.test.autoconfigure.web.servlet`).

## Docker 배포 (QNAP NAS)

```bash
# 로컬에서 이미지 빌드
docker build -t aguagu-hub:latest .

# 이미지를 tar로 저장 → NAS 전송 → 로드
docker save -o aguagu-hub.tar aguagu-hub:latest
scp aguagu-hub.tar admin@NAS_IP:/share/Container/aguagu-hub/
ssh admin@NAS_IP "cd /share/Container/aguagu-hub && docker load -i aguagu-hub.tar && docker compose up -d"

# 또는 자동 배포 스크립트 사용
deploy.bat
```

- Dockerfile: 3단계 멀티스테이지 빌드 (프론트엔드 → 백엔드 → 런타임)
- docker-compose.yml: 환경변수 `.env` 파일 참조, 재시작 정책 `unless-stopped`
- `.env.example`을 `.env`로 복사하여 실제 환경변수 설정 (DB, Telegram 등)
- 배포 상세 가이드: `docs/2026-02-12-qnap-nas-docker-배포-설정.md`

## 기록 문서

프로젝트 변경 이력은 `docs/` 디렉토리에 날짜별 마크다운으로 관리한다.

| 문서 | 내용 |
|------|------|
| `2025-01-28.md` | HTTP Client 환경설정, API 엔드포인트 수정 |
| `2025-01-29.md` | phone_number 컬럼 마이그레이션, nullable 변경, ddl-auto=validate 전환 |
| `2025-01-30.md` | Telegram 알림, 카드 한도 구현, Swagger/테스트, 어드민 프론트엔드 |
| `2025-01-31.md` | CreditCard 테이블 분리, 카드 한도 3종류(월간/연간/커스텀) 확장 |
| `2026-02-01.md` | 누적금액 정산기간 기준 변경, 마스킹 역매칭, 연도 판정 버그, 결제일 입력 |
| `2026-02-02.md` | 하나카드 한 줄 승인 형식 파싱 추가 |
| `2026-02-03.md` | 하나카드 정규식 수정, KB카드 중간형 형식 추가 |
| `2026-02-08.md` | 멀티파트 SMS 해결, 신용카드 active 필드, 삼성카드 후불교통, 파싱실패 테이블 분리 |
| `2026-02-12.md` | QNAP NAS Docker 배포 설정 (멀티스테이지 빌드, Compose, 배포 스크립트) |
| `2026-02-18.md` | 자동결제(AutoPayment) CRUD, 프론트엔드 관리 페이지 |
| `2026-02-18-mobile-frontend.md` | 모바일 프론트엔드 경로 기반 분리 (`/m/*`) |
| `2026-02-20.md` | 카드별 사용 현황 대시보드 (PC/모바일), 한도 기간 기준 잔여금액, 모바일 메뉴 변경 |
| `2026-02-22.md` | 태그 Main/Detail 분리 (TagType enum, 주요/세부 태그 구분, 통계 필터) |
| `2026-03-03-bug-analysis-and-fix.md` | 프로젝트 전수 분석 버그 33건 정리, 5건 수정 완료 (#3~#7), 28건 대기 |
