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

## 개발 명령어

```bash
./gradlew build                    # 빌드
./gradlew bootRun                  # 실행 (port 8080)
./gradlew test                     # 전체 테스트
./gradlew test --tests "*Pattern*" # 특정 테스트
./gradlew clean build              # 클린 빌드
```

DB 연결은 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 오버라이드 가능.
Telegram 알림은 환경변수 `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`, `TELEGRAM_ENABLED`로 설정.

## 아키텍처

도메인 기반 패키지 구조를 따른다. 현재 `card` 도메인과 `common` 공통 모듈이 존재.

```
com.woo.server
├── common/
│   ├── config/
│   │   ├── TelegramProperties              # Telegram 설정 속성 (@ConfigurationProperties)
│   │   └── OpenApiConfig                   # Swagger/OpenAPI 설정
│   ├── enums/CardCompany                   # 카드사 열거형 (SAMSUNG, HYUNDAI, KB 등 10개)
│   └── notification/TelegramNotificationService  # Telegram 알림 서비스
└── domain/card/
    ├── controller/
    │   ├── CardTransactionController        # 거래 REST API (/api/v1/card/transactions)
    │   └── CardLimitController              # 한도 REST API (/api/v1/card/limits)
    ├── service/
    │   ├── CardTransactionService           # 문자 처리, 중복 확인, 조회
    │   └── CardLimitService                 # 한도 CRUD, 월간 사용액 집계
    ├── dto/                                 # CardMessageRequest, CardTransactionResponse, CardLimitDto
    ├── entity/
    │   ├── CardTransaction                  # JPA 엔티티 (card_transactions 테이블)
    │   └── CardLimit                        # JPA 엔티티 (card_limits 테이블)
    ├── repository/
    │   ├── CardTransactionRepository        # Spring Data JPA
    │   └── CardLimitRepository              # Spring Data JPA
    └── parser/
        ├── CardMessageParser                # 파서 인터페이스 + ParseResult
        ├── SamsungCardParser                # 삼성카드 문자 파싱 구현체
        └── CardParserFactory                # 카드사별 파서 선택 팩토리
```

### 핵심 흐름

1. SMS 문자가 `POST /api/v1/card/transactions`로 수신됨
2. `CardParserFactory`가 카드사를 식별하고 적절한 파서 선택
3. 파서가 금액, 거래일시, 사용처, 누적금액 등을 추출 (`ParseResult`)
4. `CardTransactionService`가 중복 확인 후 `CardTransaction` 엔티티로 저장
5. `TelegramNotificationService`가 Telegram Bot API를 통해 알림 전송

### 새 카드사 파서 추가 시

1. `CardCompany` 열거형에 카드사 추가
2. `CardMessageParser` 인터페이스를 구현하는 파서 클래스 작성
3. `CardParserFactory`에 해당 파서 등록

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

## 기록 문서

프로젝트 변경 이력은 `docs/` 디렉토리에 날짜별 마크다운으로 관리한다.

| 문서 | 내용 |
|------|------|
| `2025-01-28-http-client-설정-및-api-테스트-수정.md` | HTTP Client 환경설정, API 엔드포인트 수정 |
| `2025-01-29-phone-number-컬럼-마이그레이션-이슈.md` | phone_number 컬럼 추가, nullable 변경, ddl-auto=validate 전환 |
| `2025-01-30-telegram-알림-기능-구현.md` | Telegram Bot 실시간 알림 기능 |
| `2025-01-30-카드-한도-기능-구현-및-프로젝트-현황.md` | CardLimit 기능, 프로젝트 전체 현황 |
| `2025-01-30-이슈수정-swagger-테스트-구현.md` | HTTP 테스트 이슈 수정, Swagger 추가, JUnit 테스트 구현 |
