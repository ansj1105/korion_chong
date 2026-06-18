# KORION Chong Backend API Implementation Plan

## 1. 현재 구현 상태

- 기준 저장소: `korion_chong`
- 프론트 연결 저장소: `kori_partners`
- 현재 백엔드 OpenAPI/Controller 구현 API
  - `POST /api/auth/login`
  - `GET /api/auth/availability`
  - `GET /api/auth/referral-codes/{code}/validate`
  - `POST /api/auth/email-verifications/send`
  - `POST /api/auth/email-verifications/confirm`
  - `POST /api/auth/wallet-links/verify`
  - `POST /api/auth/signup-applications`
  - `GET /api/leader/dashboard`
  - `GET /api/leader/partners`

## 2. 구현 원칙

- OpenAPI를 먼저 확정하고 Controller, DTO, Service, Repository, ContractTest 순서로 구현한다.
- 프론트가 이미 호출하는 경로를 백엔드 계약으로 맞춘다.
- 리더, 파트너, 가맹점은 각 역할별 헤더와 scope로 분리한다.
  - 리더: `X-Leader-Id`, `X-Country-Scopes`
  - 파트너: `X-Partner-Id`, `X-Country-Scopes`
  - 가맹점: `X-Merchant-Id`, `X-Country-Scopes`
- 하위 데이터 권한은 서버에서 반드시 검증한다.
  - 리더는 자기 국가 및 하위 조직만 조회한다.
  - 파트너는 본인 유치/소속 가맹점만 조회한다.
  - 가맹점은 본인 매장 데이터만 조회한다.
- 초기 구현은 프론트가 기대하는 JSON shape을 유지하되, 내부 쿼리는 실제 테이블 기준으로 교체 가능하게 만든다.
- 더미 데이터를 API 응답으로 복사하는 방식은 임시 개발 단계에서만 허용하고, 최종 구현은 DB Repository 기반으로 전환한다.

## 3. Step-by-Step 구현 체크리스트

### Step 1. 리더 API 확장

- [x] `GET /api/leader/dashboard`
- [x] `GET /api/leader/partners`
- [ ] `GET /api/leader/partner-applications`
  - 화면: `/leader/requests/partner`
  - 목적: 리더가 등록한 하위 파트너 후보/승인 요청 상태 조회
  - 권한: 리더 본인 국가 및 하위 조직 scope
- [ ] `GET /api/leader/merchant-applications`
  - 화면: `/leader/requests/merchant`
  - 목적: 리더 scope 내 가맹점 가입 요청 조회
  - 권한: 리더 본인 국가 및 하위 조직 scope
- [ ] `GET /api/leader/partner-sales`
  - 화면: `/leader/partners/sales`
  - 목적: 파트너별 매출, 하위 가맹점 매출, 수수료 요약
- [ ] `GET /api/leader/merchants`
  - 화면: `/leader/merchants`
  - 목적: 리더 국가/조직 내 가맹점 목록 조회
- [ ] `GET /api/leader/merchant-sales`
  - 화면: `/leader/merchants/sales`
  - 목적: 가맹점별 매출, 거래 건수, QR/NFC/BLE 사용 현황 조회
- [ ] `GET /api/leader/transactions`
  - 화면: `/leader/transactions`, `/leader/transactions/offline`, `/leader/transactions/failed`
  - Query: `variant=all|offline|failed`, `countryScope`, 기간/상태/가맹점/파트너 필터
  - 목적: 하위 조직 거래 로그 조회
- [ ] `GET /api/leader/settlements/request-summary`
  - 화면: `/leader/settlement/request`
  - 목적: 정산 가능 금액, 자동정산, 보류 거래, 신청 폼 기본값 조회
- [ ] `GET /api/leader/settlements`
  - 화면: `/leader/settlement/history`
  - 목적: 정산 요청, 승인, 지급, 보류/조정 내역 조회
- [ ] `GET /api/leader/settlements/detail`
  - 화면: `/leader/settlement/history/detail`
  - 목적: 정산 상세, 가맹점/파트너/보류 거래 상세 조회
- [ ] `GET /api/leader/hq-notices`
  - 화면: `/leader/hq-notices`
  - 목적: 본사 공지 수신 목록 조회
- [ ] `GET /api/leader/notices/send-summary`
  - 화면: `/leader/notices/send`
  - 목적: 리더가 발송 가능한 대상 수와 공지 발송 KPI 조회
- [ ] `GET /api/leader/notices`
  - 화면: `/leader/notices/history`
  - 목적: 공지 발송 내역, 읽음률, 실패 대상 조회
- [ ] `GET /api/leader/profile`
  - 화면: `/leader/settings/profile`
  - 목적: 리더 프로필, Wallet, 국가 scope, 상태 조회
- [ ] `GET /api/leader/activity-logs`
  - 화면: `/leader/settings/activity-log`
  - 목적: 리더 계정 활동 로그 조회

### Step 2. 파트너 API 구현

- [ ] `GET /api/partner/dashboard`
  - 화면: `/partner/dashboard`
  - 목적: 본인 유치 가맹점 수, 월 거래량, 수수료, 정산가능금액 조회
- [ ] `GET /api/partner/merchant-applications`
  - 화면: `/partner/requests/merchant`
  - 목적: 파트너가 생성한 가맹점 등록 요청 목록 조회
- [ ] `GET /api/partner/merchant-applications/detail`
  - 화면: `/partner/requests/merchant/detail`
  - 목적: 가맹점 가입 요청 상세 및 검증자료 조회
- [ ] `GET /api/partner/merchants`
  - 화면: `/partner/merchants`
  - 목적: 본인이 유치한 가맹점만 조회
- [ ] `GET /api/partner/merchant-sales`
  - 화면: `/partner/merchants/sales`
  - 목적: 본인 하위 가맹점 매출/수수료 조회
- [ ] `GET /api/partner/settlements/request-summary`
  - 화면: `/partner/settlement/request`
  - 목적: 파트너 정산 가능 수수료, 보류 거래, 신청 기본값 조회
- [ ] `GET /api/partner/settlements`
  - 화면: `/partner/settlement/history`
  - 목적: 파트너 정산 신청/승인/지급 내역 조회
- [ ] `GET /api/partner/settlements/detail`
  - 화면: `/partner/settlement/history/detail`
  - 목적: 파트너 정산 상세 조회
- [ ] `GET /api/partner/hq-notices`
  - 화면: `/partner/hq-notices`
  - 목적: 본사/리더 공지 수신 목록 조회
- [ ] `GET /api/partner/notices/send-summary`
  - 화면: `/partner/notices/send`
  - 목적: 하위 가맹점 공지 발송 대상 수/KPI 조회
- [ ] `GET /api/partner/notices`
  - 화면: `/partner/notices/history`
  - 목적: 파트너 공지 발송 내역 조회
- [ ] `GET /api/partner/profile`
  - 화면: `/partner/settings/profile`
  - 목적: 파트너 프로필, Wallet, 소속 리더/본사 직접계약 여부 조회
- [ ] `GET /api/partner/activity-logs`
  - 화면: `/partner/settings/activity-log`
  - 목적: 파트너 활동 로그 조회

### Step 3. 가맹점 API 구현

- [ ] `GET /api/merchant/dashboard`
  - 화면: `/merchant/dashboard`
  - 목적: 본인 매장 KPI, 거래량, QR/NFC/BLE 상태, 최근 결제 조회
- [ ] `GET /api/merchant/transactions`
  - 화면: `/merchant/transactions`, `/merchant/transactions/refund`
  - Query: `variant=all|refund`, 기간/결제수단/상태 필터
  - 목적: 본인 매장 거래 및 환불 관련 거래 조회
- [ ] `GET /api/merchant/settlements`
  - 화면: `/merchant/settlement/history`
  - 목적: 본인 매장 정산 내역 조회
- [ ] `GET /api/merchant/hq-notices`
  - 화면: `/merchant/hq-notices`
  - 목적: 본사/상위 조직 공지 수신 목록 조회
- [ ] `GET /api/merchant/profile`
  - 화면: `/merchant/settings/profile`
  - 목적: 매장 정보, 결제 설정, Wallet, 상품 상태 조회
- [ ] `GET /api/merchant/activity-logs`
  - 화면: `/merchant/settings/activity-log`
  - 목적: 가맹점 계정 활동 로그 조회

## 4. 권한 및 검증 공통 작업

- [ ] `LeaderAuthContextFactory`, `PartnerAuthContextFactory`, `MerchantAuthContextFactory` 분리 또는 공통화
- [ ] `403 Forbidden` 응답 표준화
  - 리더가 타 국가/타 조직 조회
  - 파트너가 타 파트너 가맹점 조회
  - 가맹점이 타 매장 조회
- [ ] 공통 응답 오류 schema OpenAPI 반영
- [ ] `countryScope` 필터 유효성 검증
- [ ] 기간 필터 검증
  - `YYYY-MM`
  - 날짜 range
  - 당일 거래 제외 정산 규칙
- [ ] ActivityLog/AuditLog 기록 대상 정의
  - 로그인
  - 승인 요청
  - 정산 신청
  - 공지 발송
  - 프로필/Wallet 변경

## 5. 구현 순서

1. OpenAPI paths/components 추가
   - 리더 확장 API
   - 파트너 API
   - 가맹점 API
2. DTO 정의
   - 프론트 JSON shape과 맞는 response record 추가
   - 공통 page/filter response component 정의
3. Controller 추가
   - `LeaderController` 확장
   - `PartnerController` 신규
   - `MerchantController` 신규
4. Service 계층 추가
   - role별 scope 검증 우선
   - 조회 조건 구성
   - DTO 조립
5. Repository 계층 추가
   - 현재 사용 가능한 테이블 우선 연결
   - 테이블 미정인 화면은 명시적으로 stub repository로 분리
6. ContractTest 추가
   - 정상 조회
   - 권한 밖 scope 403
   - 필터 검증 실패 400
7. 프론트 연동 검증
   - `kori_partners npm run build`
   - 백엔드 contract test
   - 로컬 API smoke test

## 6. 1차 구현 단위 제안

### 1차 PR 범위

- 리더 API 확장 중 조회만 필요한 화면부터 구현
  - `GET /api/leader/partner-applications`
  - `GET /api/leader/merchant-applications`
  - `GET /api/leader/merchants`
  - `GET /api/leader/profile`
  - `GET /api/leader/activity-logs`

### 2차 PR 범위

- 리더 거래/매출/정산 API
  - `GET /api/leader/partner-sales`
  - `GET /api/leader/merchant-sales`
  - `GET /api/leader/transactions`
  - `GET /api/leader/settlements/request-summary`
  - `GET /api/leader/settlements`
  - `GET /api/leader/settlements/detail`

### 3차 PR 범위

- 파트너 API 전체 골격
  - `PartnerController`
  - `PartnerAuthContext`
  - 파트너 dashboard, merchants, settlements, notices, profile, activity logs

### 4차 PR 범위

- 가맹점 API 전체 골격
  - `MerchantController`
  - `MerchantAuthContext`
  - merchant dashboard, transactions, settlements, notices, profile, activity logs

## 7. 완료 기준

- 모든 프론트 호출 경로가 OpenAPI에 존재한다.
- 각 path에 Controller와 ContractTest가 존재한다.
- 역할별 scope 위반은 서버에서 403으로 차단된다.
- `./gradlew test`가 통과한다.
- `kori_partners npm run build`가 통과한다.
