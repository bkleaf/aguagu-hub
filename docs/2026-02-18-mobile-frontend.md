# 2026-02-18 모바일 프론트엔드 경로 기반 분리

## 개요

기존 PC 전용 프론트엔드(`front/`)와 별도로 모바일 전용 프론트엔드(`front-mobile/`)를 생성하고, 경로 기반으로 분리하여 서빙하도록 구현했다.

- `/*` → PC 프론트엔드 (기존)
- `/m/*` → 모바일 프론트엔드 (신규)
- 동일한 백엔드 API(`/api/v1/...`)를 공유

## 변경 사항

### 1. 백엔드 — WebConfig.kt 수정

**파일**: `src/main/kotlin/com/woo/server/common/config/WebConfig.kt`

- `/m/**` 경로용 ResourceHandler 추가 (`classpath:/static-mobile/`)
- SPA 폴백: `/static-mobile/index.html`
- 핸들러 등록 순서: `/m/**` 먼저 (구체적 경로 우선), `/**` 나중에

### 2. 모바일 프론트엔드 프로젝트 — `front-mobile/`

기존 `front/`와 동일한 기술 스택 (Vue 3 + Vuetify 3 + Vite)으로 별도 프로젝트 생성.

#### 구조

```
front-mobile/
├── package.json            # 프로젝트 의존성 (aguagu-hub-mobile)
├── vite.config.js          # base: '/m/', outDir: static-mobile
├── index.html              # HTML 엔트리포인트
└── src/
    ├── main.js             # Vue 앱 초기화
    ├── App.vue             # 루트 컴포넌트
    ├── api/
    │   └── index.js        # API 함수 (기존 front와 동일)
    ├── router/
    │   └── index.js        # 라우터 (base: '/m/')
    ├── layouts/
    │   └── MobileLayout.vue # 모바일 레이아웃 (하단 네비게이션)
    └── views/
        ├── DashboardView.vue         # 대시보드 (요약 카드 + 최근 거래)
        ├── TransactionListView.vue   # 거래 내역 (카드형 리스트)
        ├── TransactionCreateView.vue # 문자 등록 폼
        ├── LimitListView.vue         # 한도 (프로그레스 바, 확장 상세)
        ├── CreditCardListView.vue    # 카드 관리 (카드 UI)
        ├── AutoPaymentListView.vue   # 자동결제 관리
        ├── MonthlyBillListView.vue   # 월별 청구서
        └── MoreView.vue              # 더보기 메뉴
```

#### 모바일 UI 특징

- 하단 네비게이션 바 (`v-bottom-navigation`) — 대시보드, 거래, 한도, 카드, 더보기
- 테이블 대신 `v-card` 리스트 사용
- 다이얼로그는 `fullscreen` 모드
- 한도 페이지에 프로그레스 바로 사용률 시각화
- 터치 친화적 레이아웃 (큰 터치 타겟, 세로 스크롤)

### 3. Dockerfile 수정

- 기존 `frontend-build` 스테이지 유지 (PC)
- `frontend-mobile-build` 스테이지 추가 (모바일)
- `backend-build` 스테이지에서 양쪽 빌드 결과물 복사

### 4. 주요 설정

| 설정 | PC (`front/`) | 모바일 (`front-mobile/`) |
|------|---------------|-------------------------|
| Vite base | `/` (기본) | `/m/` |
| Vue Router base | `/` (기본) | `/m/` |
| 빌드 출력 | `static/` | `static-mobile/` |
| Dev 서버 포트 | 3000 | 3001 |
| 레이아웃 | 사이드바 | 하단 네비게이션 |

## 빌드 명령

```bash
# 모바일 프론트엔드
cd front-mobile && npm install && npm run build

# PC 프론트엔드
cd front && npm install && npm run build

# 백엔드
./gradlew build -x test

# Docker
docker build -t aguagu-hub:latest .
```

## 검증

- [x] `cd front-mobile && npm install && npm run build` 성공
- [x] `./gradlew build -x test` 성공
- `http://localhost:8080/` → PC 화면
- `http://localhost:8080/m/` → 모바일 화면
