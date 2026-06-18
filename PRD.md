# KORION 총판 시스템 PRD

## 1. 개요

KORION 총판 시스템은 본사 아래 리더, 파트너, 가맹점으로 이어지는 영업 조직의 거래 실적, 수수료 정산, 지급 상태를 관리하는 운영 시스템이다. 본 PRD의 1차 범위는 로그인, 정산, 지급 기능으로 제한한다.

## 2. 목표

- 리더, 파트너, 운영자가 안전하게 로그인하고 본인 권한 범위의 조직과 정산 데이터만 조회한다.
- 리더는 하위 파트너와 가맹점의 거래 실적, 본사 정산 금액, 본인 수수료를 확인한다.
- 파트너는 하위 가맹점의 거래 실적과 본사 및 리더 정산 금액을 확인한다.
- 운영자가 가맹점 거래에 따른 가맹점, 파트너, 리더, 본사 귀속 금액과 상위 정산 금액을 확정하고 지급 상태를 추적한다.

## 3. 사용자 역할

### 3.1 본사

- 전체 리더, 파트너, 가맹점 조직을 관리한다.
- 리더와 직접 계약할 수 있다.
- 파트너와 직접 계약할 수 있다.
- 전체 거래 실적과 본사 귀속 수수료를 확인한다.
- 정산 확정, 지급 승인, 지급 결과 등록을 수행한다.

### 3.2 리더

- 본인에게 배정된 파트너와 하위 가맹점 목록을 조회한다.
- 가맹점과 직접 계약할 수 있다.
- 하위 조직의 거래 실적, 정산 예정 금액, 지급 상태를 확인한다.
- 가맹점 거래에서 정해진 리더 수수료를 정산받는다.
- 리더 본인 귀속분 외 나머지 본사 귀속 금액은 본사에 정산되는 구조로 관리된다.
- 리더 계정 자체로는 스토어를 개설하거나 판매할 수 없다.
- 리더가 직접 장사하려면 별도 가맹점 엔티티를 생성해야 한다.

### 3.3 파트너

- 리더 하위에 소속되어 가맹점을 관리한다.
- 본사와 직접 계약하거나 리더 하위 계약으로 소속될 수 있다.
- 가맹점과 직접 계약할 수 있다.
- 본인에게 배정된 가맹점 목록, 거래 실적, 정산 상태를 조회한다.
- 파트너 정산은 리더 정산분과 본사 정산분으로 구분해 관리한다.
- 파트너는 하위 가맹점 운영 주체이며, 계약 경로에 따라 파트너 수수료 또는 상위 정산 금액의 주체가 될 수 있다.
- 파트너 계정 자체로는 스토어를 개설하거나 판매할 수 없다.
- 파트너가 직접 장사하려면 별도 가맹점 엔티티를 생성해야 한다.

### 3.4 가맹점

- 파트너 하위에 소속되는 실제 거래 발생 사업자이다.
- 리더와 직접 계약하거나 파트너와 직접 계약할 수 있다.
- 스토어 개설과 판매는 가맹점만 수행할 수 있다.
- 가맹점별 정해진 수수료율과 계약 경로에 따라 거래 금액이 가맹점, 파트너, 리더, 본사 귀속분으로 분배된다.
- 1차 범위에서는 별도 가맹점 포털을 제공하지 않고, 운영자와 상위 조직 화면에서 관리되는 대상으로 정의한다.

### 3.5 운영자

- 리더, 파트너, 가맹점 계정과 조직 배정을 관리한다.
- 정산 데이터를 검수하고 지급을 승인 또는 보류한다.
- 지급 결과를 등록하고 실패 건을 재처리한다.

## 4. 계약 및 영업 정책

### 4.1 계약 구조

- 본사는 리더와 직접 계약할 수 있다.
- 본사는 파트너와 직접 계약할 수 있다.
- 가맹점은 리더와 직접 계약할 수 있다.
- 가맹점은 파트너와 직접 계약할 수 있다.
- 파트너가 본사와 직접 계약된 경우에도 가맹점 거래 정산은 저장된 정산 경로 기준으로 가맹점, 파트너, 리더, 본사 귀속 금액을 분리한다.
- 가맹점의 계약 상대는 정산 정책과 권한 범위를 결정하는 기준 값으로 저장한다.

### 4.2 영업 주체 제한

- 실제 스토어 개설, 상품 판매, 결제 발생 주체는 가맹점만 가능하다.
- 리더 계정은 스토어를 직접 개설할 수 없다.
- 파트너 계정은 스토어를 직접 개설할 수 없다.
- 리더 또는 파트너가 직접 판매를 하려면 별도 가맹점 엔티티를 생성하고 해당 가맹점으로 스토어를 개설해야 한다.
- 정산과 매출 집계는 판매를 발생시킨 가맹점 기준으로 기록한다.

### 4.3 KORION WALLET 인증 정책

- 리더, 파트너, 가맹점은 KORION WALLET의 TRX 네트워크 주소를 계정 식별 및 인증 수단으로 등록할 수 있다.
- TRX 네트워크 주소는 리더, 파트너, 가맹점 엔티티별로 고유해야 한다.
- 동일한 TRX 네트워크 주소를 여러 리더, 파트너, 가맹점 계정에 중복 등록할 수 없다.
- TRX 네트워크 주소 인증은 지갑 소유자가 서명한 nonce 검증 방식으로 수행한다.
- KORION WALLET 인증은 KORION Wallet address 소유 증명 기반 인증으로 정의한다.
- 인증 기준값은 KORION WALLET TRX 네트워크 주소이며, 서버가 발급한 nonce에 대한 지갑 서명을 검증해 주소 소유를 확인한다.
- 인증용 nonce는 1회성으로 발급하고 만료 시간을 가진다.
- 주소 등록, 주소 변경, 주소 인증 성공, 주소 인증 실패는 감사 로그에 기록한다.
- KORION WALLET TRX 주소는 계정 소유 확인과 정산금 수취 주소로 사용할 수 있다.
- 인증 주소와 정산 수취 주소가 다른 경우 두 주소 모두 지갑 소유자 검증을 완료해야 한다.

## 5. 핵심 기능

## 5.1 로그인

### 목적

리더, 파트너, 운영자가 권한에 맞는 시스템 화면에 접근할 수 있도록 인증과 세션을 제공한다.

### 기능 요구사항

- 사용자는 아이디와 비밀번호로 로그인할 수 있다.
- 리더, 파트너, 가맹점은 등록된 KORION WALLET TRX 네트워크 주소로 인증할 수 있다.
- KORION WALLET 인증 시 서버가 발급한 nonce에 대한 지갑 서명을 검증한다.
- TRX 네트워크 주소가 등록되지 않았거나 비활성 상태이면 지갑 인증을 허용하지 않는다.
- 로그인 성공 시 사용자 역할을 판별해 접근 가능한 메뉴를 제한한다.
- 리더는 본인에게 배정된 파트너와 하위 가맹점 데이터만 조회할 수 있다.
- 파트너는 본인에게 배정된 가맹점 데이터만 조회할 수 있다.
- 운영자는 전체 리더, 파트너, 가맹점 정산 데이터를 조회할 수 있다.
- 비밀번호는 평문 저장을 금지한다.
- 로그인 실패 횟수가 일정 기준을 초과하면 계정을 임시 잠금 처리한다.
- 세션 만료 시 재로그인을 요구한다.

### 주요 화면

- 로그인 화면
- KORION WALLET 주소 인증 화면
- KORION WALLET 주소 등록 및 변경 화면
- 비밀번호 재설정 요청 화면
- 계정 잠금 안내 화면

### 수용 기준

- 정상 계정으로 로그인하면 역할별 대시보드로 이동한다.
- 등록된 TRX 네트워크 주소로 nonce 서명이 검증되면 역할별 대시보드로 이동한다.
- 등록되지 않은 TRX 네트워크 주소 또는 잘못된 서명은 인증 실패로 처리된다.
- 잘못된 비밀번호 입력 시 오류 메시지가 표시된다.
- 리더 계정으로 다른 리더의 파트너 또는 가맹점 데이터를 조회할 수 없다.
- 파트너 계정으로 다른 파트너의 가맹점 데이터를 조회할 수 없다.
- 리더 또는 파트너 계정으로 스토어 개설 화면에 접근할 수 없다.

## 5.2 정산

### 목적

운영자, 리더, 파트너가 가맹점 거래에서 발생한 정산 대상 금액을 계약 경로에 따라 본사, 리더, 파트너, 가맹점 귀속분과 상위 정산 금액으로 분리한다.

### 정산 기준

- 정산 단위는 일별 또는 월별로 설정할 수 있다.
- 정산 대상은 결제 완료 후 취소, 환불, 이상 거래가 제외된 거래로 한다.
- 정산 배치는 실행일 기준 전일 거래까지만 집계한다.
- 월 정산일이 지정된 경우 정산 대상 거래 기간은 해당 정산일 다음 날부터 다음 정산일 전날까지로 계산한다.
- 예: 정산일이 매월 1일이면 해당 월 2일부터 말일까지의 거래 내역을 다음 정산 주기에 집계한다.
- 정산일은 계약 구조에 따라 상위 주체가 결정하며, 한번 확정된 정산일은 권한 없는 하위 주체가 임의 변경할 수 없다.
- 리더는 본인 하위 파트너들의 정산 시기를 설정할 수 있다.
- 리더가 있는 파트너는 리더가 정한 정산일을 따른다.
- 리더가 없는 본사 직접 계약 파트너는 본사 정책 범위 안에서 직접 정산 시기를 설정할 수 있다.
- 가맹점은 계약 상대가 리더이든 파트너이든 직접 정산일을 변경할 수 없다.
- 가맹점 거래는 가맹점별 계약 경로와 수수료 정책에 따라 본사, 리더, 파트너, 가맹점 귀속 금액으로 분배한다.
- 정산 경로는 판매를 발생시킨 가맹점에서 시작하며, 파트너 또는 리더 계정 자체에서 시작하는 매출 정산 경로는 만들지 않는다.
- 파트너 정산은 파트너가 연결된 상위 리더 또는 본사에 정산해야 하는 금액을 구분해 관리한다.
- 리더 정산은 리더가 본사에 정산해야 하는 금액을 관리한다.
- 본사 정산은 전체 거래에서 본사 귀속 수수료, 리더의 본사 정산 금액, 파트너의 본사 정산 금액을 함께 관리한다.
- 정산 금액은 거래 금액에서 취소 금액, 환불 금액, 본사 수수료, 리더 수수료, 파트너 수수료, 가맹점 지급액, 기타 조정 금액을 반영해 계산한다.
- 정산 확정 이후에는 원본 정산 데이터를 임의 수정하지 않고 조정 내역으로 보정한다.

### 정산 경로 정책

- 정산 경로는 하드코딩하지 않고 계약 관계와 정산 경로 정책을 기준으로 동적으로 산출한다.
- `가맹점 -> 파트너 -> 리더 -> 본사`, `가맹점 -> 파트너 -> 본사`, `가맹점 -> 리더 -> 본사`, `가맹점 -> 본사`는 현재 예상 가능한 예시 경로이며 고정 목록이 아니다.
- 신규 계약 구조가 추가되면 코드 수정 없이 정산 경로 정책과 수수료 정책 데이터 변경으로 반영할 수 있어야 한다.
- 모든 정산 경로의 시작점은 가맹점 거래이며, 리더 또는 파트너가 직접 판매하려면 별도 가맹점 엔티티가 생성되어야 한다.
- 정산 배치는 거래 발생 가맹점의 계약 경로와 정책 계산 결과를 스냅샷으로 저장하고, 저장된 정산 경로 스냅샷 기준으로 귀속 금액을 계산한다.
- 본사는 가맹점 거래의 본사 귀속분, 파트너의 본사 정산분, 리더의 본사 정산분을 통합 관리한다.

### 정산일 변경 정책

- 본사는 리더의 정산일을 설정할 수 있다.
- 본사는 직접 계약 파트너의 정산일을 설정하거나, 해당 파트너에게 정산일 설정 권한을 부여할 수 있다.
- 리더는 본인 하위 파트너들의 정산 시기를 설정할 수 있다.
- 리더가 하위 파트너의 정산 시기를 설정하면 해당 파트너와 연결된 하위 가맹점 정산 기간도 그 기준을 따른다.
- 파트너는 리더 하위에 소속된 경우 정산일을 직접 바꿀 수 없다.
- 파트너는 리더 없이 본사와 직접 계약된 경우에만 본사 허용 범위 안에서 정산 시기를 직접 설정할 수 있다.
- 정산일 변경은 다음 미확정 정산 주기부터 적용하고, 이미 생성되었거나 확정된 정산 배치에는 소급 적용하지 않는다.
- 정산일 변경 이력은 변경 전 값, 변경 후 값, 변경자, 변경 사유, 적용 시작 주기를 기록한다.

### 기능 요구사항

- 운영자는 정산 기간을 선택해 정산 배치를 생성할 수 있다.
- 운영자는 정산일 기준으로 자동 산출된 거래 기간을 확인하고 정산 배치를 생성할 수 있다.
- 운영자는 정산 경로별, 리더별, 파트너별, 가맹점별 정산 예정 금액과 상위 정산 금액을 조회할 수 있다.
- 리더는 하위 파트너와 가맹점의 정산 예정 금액, 본인 귀속 수수료, 본사 정산 금액을 조회할 수 있다.
- 파트너는 하위 가맹점의 정산 예정 금액, 리더 정산 금액, 본사 정산 금액을 조회할 수 있다.
- 운영자는 정산 건을 검수 후 확정, 보류, 반려 처리할 수 있다.
- 정산 상태는 `예정`, `검수중`, `확정`, `보류`, `반려`, `지급요청`, `지급완료`로 관리한다.
- 정산 상세에는 정산 경로, 거래 건수, 총 거래 금액, 취소 금액, 본사 수수료, 리더 수수료, 파트너 수수료, 가맹점 지급액, 파트너의 리더 정산액, 파트너의 본사 정산액, 리더의 본사 정산액, 조정 금액, 최종 정산 금액을 표시한다.
- 정산 데이터는 엑셀 또는 CSV로 다운로드할 수 있다.

### 주요 화면

- 정산 대시보드
- 정산 배치 목록
- 리더별 정산 상세
- 파트너별 정산 상세
- 가맹점별 정산 상세
- 정산 조정 내역

### 수용 기준

- 운영자가 기간을 선택하면 해당 기간의 정산 대상 거래가 집계된다.
- 정산 배치는 실행일 기준 전일 거래까지만 포함한다.
- 정산일이 매월 1일인 경우 해당 월 2일부터 말일까지의 거래가 하나의 월 정산 구간으로 집계된다.
- 리더는 본인 하위 파트너들의 정산 시기를 설정할 수 있다.
- 리더가 있는 파트너는 정산일을 직접 변경할 수 없고, 리더가 설정한 정산 시기를 따른다.
- 리더가 없는 본사 직접 계약 파트너만 본사 정책 범위 안에서 정산 시기를 직접 설정할 수 있다.
- 가맹점 거래 정산은 계약 관계와 정산 경로 정책에 따라 산출된 경로로 분류된다.
- 문서에 기재된 정산 경로 예시는 고정 분기 조건으로 구현하지 않는다.
- 파트너 정산은 정산 경로에 따라 리더 정산분과 본사 정산분으로 분리된다.
- 리더 정산은 본사 정산분만 생성된다.
- 리더는 본인 하위 파트너와 가맹점의 정산 내역만 볼 수 있다.
- 파트너는 본인 하위 가맹점의 정산 내역만 볼 수 있다.
- 확정된 정산 건은 지급 요청 대상으로 전환할 수 있다.
- 보류 또는 반려된 정산 건은 지급 요청 대상에서 제외된다.

## 5.3 지급

### 목적

확정된 정산 금액을 계약 정책에 따라 해당 가맹점, 파트너, 리더의 KORION WALLET으로 지급하고, 파트너 및 리더가 상위 주체에 정산해야 하는 금액과 본사 귀속 금액을 추적한다.

### 지급 기준

- 지급 대상은 정산 상태가 `확정`인 건으로 제한한다.
- 지급 대상 유형은 가맹점, 파트너, 리더로 구분한다.
- 지급은 정산금 기준으로 산출된 각 지급 대상 금액을 해당 가맹점, 파트너, 리더의 KORION WALLET TRX 네트워크 주소로 입금하는 방식으로 처리한다.
- 지급 대상의 KORION WALLET TRX 주소가 등록되지 않았거나 인증되지 않은 경우 지급 요청을 생성할 수 없다.
- 본사 귀속 금액은 지급 대상이 아니라 본사 수익으로 집계한다.
- 파트너의 리더 및 본사 정산 금액은 상위 정산 채무로 관리하고, 파트너 수수료가 있는 경우에만 파트너 지급 대상으로 생성한다.
- 지급 전 수취 KORION WALLET 주소, 지갑 인증 상태, 네트워크(TRX)를 확인한다.
- 지급 실패 시 실패 사유를 기록하고 재지급 가능 상태로 전환한다.
- 지급 완료 후 지급 금액, 지급 일시, 처리자, KORION WALLET 주소, 네트워크, 외부 지급 참조값 또는 온체인 거래 식별자를 기록한다.

### 기능 요구사항

- 운영자는 확정 정산 건을 선택해 지급 요청을 생성할 수 있다.
- 운영자는 정산 건의 가맹점 지급액, 파트너 지급액, 리더 지급액을 각각 지급 요청으로 생성할 수 있다.
- 지급 요청은 승인 대기, 승인 완료, 지급 처리중, 지급 완료, 지급 실패, 지급 보류 상태를 가진다.
- 운영자는 지급 요청을 승인하거나 보류할 수 있다.
- 지급 결과는 수동 등록, 내부 지급 처리, 외부 지급 시스템 연동, 온체인 거래 식별자 등록으로 반영할 수 있다.
- 리더는 본인 지급 예정 금액과 하위 파트너 및 가맹점 지급 상태를 조회할 수 있다.
- 파트너는 본인 지급 예정 금액, 하위 가맹점 지급 상태, 리더 및 본사에 정산해야 하는 금액을 조회할 수 있다.
- 지급 실패 건은 실패 사유와 함께 재처리할 수 있다.

### 주요 화면

- 지급 요청 목록
- 지급 승인 화면
- 지급 상세 화면
- 지급 대상별 상세 화면
- 지급 실패 및 재처리 화면

### 수용 기준

- 확정되지 않은 정산 건은 지급 요청할 수 없다.
- 지급 대상의 인증된 KORION WALLET TRX 주소가 없으면 지급 요청할 수 없다.
- 본사 귀속 금액은 지급 요청으로 생성되지 않는다.
- 파트너의 리더 및 본사 정산 금액은 지급 요청으로 생성되지 않는다.
- 파트너 지급액은 정산 경로와 수수료 정책상 파트너 귀속분이 있는 경우에만 생성된다.
- 지급 완료 처리된 건은 중복 지급 요청할 수 없다.
- 지급 실패 건은 실패 사유가 없으면 저장할 수 없다.
- 지급 완료 건은 지급 대상 KORION WALLET 주소와 온체인 거래 식별자 또는 외부 지급 참조값을 기록해야 한다.
- 리더와 파트너는 본인 권한 범위의 지급 내역만 조회할 수 있다.

## 6. 데이터 모델 초안

### 6.1 리더

- 리더 ID
- KORION WALLET TRX 주소
- 지갑 인증 상태
- 로그인 ID
- 비밀번호 해시
- 이름
- 연락처
- 수수료 정책 ID
- 정산일
- 정산일 변경 잠금 여부
- 정산일 변경 권한 유형
- 정산 수취 KORION WALLET TRX 주소
- 상태
- 생성일
- 수정일

### 6.2 파트너

- 파트너 ID
- 리더 ID
- 계약 상대 유형
- 계약 상대 ID
- KORION WALLET TRX 주소
- 지갑 인증 상태
- 로그인 ID
- 비밀번호 해시
- 이름
- 연락처
- 수수료 정책 ID
- 정산일
- 정산일 변경 잠금 여부
- 정산일 변경 권한 유형
- 정산 수취 KORION WALLET TRX 주소
- 상태
- 생성일
- 수정일

### 6.3 가맹점

- 가맹점 ID
- 리더 ID
- 파트너 ID
- 계약 상대 유형
- 계약 상대 ID
- KORION WALLET TRX 주소
- 지갑 인증 상태
- 상호명
- 사업자등록번호
- 대표자명
- 연락처
- 수수료 정책 ID
- 정산일
- 정산일 소유 주체 유형
- 정산일 소유 주체 ID
- 정산 수취 KORION WALLET TRX 주소
- 스토어 개설 가능 여부
- 상태
- 생성일
- 수정일

### 6.4 계약

- 계약 ID
- 계약 주체 유형
- 계약 주체 ID
- 계약 상대 유형
- 계약 상대 ID
- 계약 시작일
- 계약 종료일
- 정산일
- 정산일 변경 권한 유형
- 상태
- 생성일
- 수정일

### 6.5 수수료 정책

- 수수료 정책 ID
- 정책명
- 적용 대상 유형
- 정산 경로 정책 ID
- 본사 수수료율
- 리더 수수료율
- 파트너 수수료율
- 가맹점 지급률
- 지급 대상 정책
- 적용 시작일
- 적용 종료일
- 상태
- 생성일
- 수정일

### 6.6 정산 경로 정책

- 정산 경로 정책 ID
- 정책명
- 시작 주체 유형
- 경로 산출 조건
- 경로 참여 주체 규칙
- 수수료 귀속 규칙
- 지급 대상 산출 규칙
- 상위 정산 채무 산출 규칙
- 적용 시작일
- 적용 종료일
- 상태
- 생성일
- 수정일

### 6.7 정산

- 정산 ID
- 정산 기간 시작일
- 정산 기간 종료일
- 리더 ID
- 파트너 ID
- 가맹점 ID
- 수수료 정책 ID
- 정산 경로 정책 ID
- 정산 경로 스냅샷
- 정산 기준일
- 거래 집계 시작일
- 거래 집계 종료일
- 거래 건수
- 총 거래 금액
- 취소 금액
- 본사 수수료 금액
- 리더 수수료 금액
- 파트너 수수료 금액
- 가맹점 지급 금액
- 파트너 리더 정산 금액
- 파트너 본사 정산 금액
- 리더 본사 정산 금액
- 조정 금액
- 최종 정산 금액
- 정산 상태
- 확정일
- 생성일
- 수정일

### 6.8 지급

- 지급 ID
- 정산 ID
- 지급 대상 유형
- 지급 대상 ID
- 지급 금액
- 지급 상태
- 지급 네트워크
- 지급 KORION WALLET TRX 주소
- 온체인 거래 식별자
- 외부 지급 참조값
- 실패 사유
- 지급 완료일
- 생성일
- 수정일

### 6.9 지갑 인증 이력

- 인증 이력 ID
- 대상 유형
- 대상 ID
- TRX 네트워크 주소
- nonce
- 서명값
- 인증 결과
- 실패 사유
- 요청 IP
- 생성일
- 만료일

### 6.10 정산일 변경 이력

- 변경 이력 ID
- 대상 유형
- 대상 ID
- 변경 전 정산일
- 변경 후 정산일
- 변경 권한 유형
- 적용 시작 주기
- 변경 사유
- 변경자 ID
- 생성일

## 7. 권한 정책

- 리더는 본인 계정과 연결된 파트너, 가맹점, 정산, 지급 데이터만 조회할 수 있다.
- 파트너는 본인 계정과 연결된 가맹점, 정산, 지급 데이터만 조회할 수 있다.
- 리더, 파트너, 가맹점의 KORION WALLET TRX 주소는 해당 엔티티 인증과 권한 확인에만 사용한다.
- TRX 네트워크 주소 인증이 성공해도 계약 관계와 역할 권한을 우회할 수 없다.
- 리더와 파트너는 정산 또는 지급 상태를 직접 확정하거나 완료 처리할 수 없다.
- 리더는 본인 하위 파트너들의 정산 시기를 설정할 수 있다.
- 리더 하위 파트너는 정산일을 직접 변경할 수 없다.
- 본사 직접 계약 파트너만 본사가 허용한 범위에서 정산 시기를 직접 설정할 수 있다.
- 리더와 파트너는 직접 스토어를 개설하거나 판매 주체가 될 수 없다.
- 스토어 개설 권한은 가맹점 엔티티에만 부여한다.
- 운영자는 전체 데이터를 조회하고 정산 확정, 보류, 반려, 지급 승인, 지급 결과 등록을 수행할 수 있다.
- 모든 정산 확정, 지급 승인, 지급 완료, 지급 실패 처리에는 처리자와 처리 시간이 기록되어야 한다.

## 8. 상태 정의

### 8.1 정산 상태

- `예정`: 정산 집계 전 또는 집계 준비 상태
- `검수중`: 운영자가 정산 내역을 확인 중인 상태
- `확정`: 지급 요청이 가능한 상태
- `보류`: 추가 확인이 필요해 지급 요청이 제한된 상태
- `반려`: 정산 대상에서 제외되거나 재생성이 필요한 상태
- `지급요청`: 지급 프로세스로 넘어간 상태
- `지급완료`: 연결된 지급이 완료된 상태

### 8.2 지급 상태

- `승인대기`: 운영자 승인 전 상태
- `승인완료`: 지급 실행 가능한 상태
- `지급처리중`: 외부 지급 또는 내부 처리 진행 상태
- `지급완료`: 지급이 정상 완료된 상태
- `지급실패`: 지급 시도 실패 상태
- `지급보류`: 운영자가 지급을 일시 중단한 상태

## 9. 비기능 요구사항

- 권한 검증은 서버에서 최종 수행한다.
- 계약 관계 검증은 서버에서 최종 수행한다.
- KORION WALLET TRX 주소 서명 검증은 서버에서 최종 수행한다.
- 지급 대상 KORION WALLET 주소와 인증 상태 검증은 서버에서 최종 수행한다.
- 로그인, 정산 확정, 지급 승인, 지급 완료, 지급 실패는 감사 로그를 남긴다.
- 정산 계산은 정산 시점의 수수료 정책 스냅샷을 기준으로 수행한다.
- 정산 기간 계산은 정산일, 계약 관계, 정산일 변경 이력을 기준으로 서버에서 결정한다.
- 정산 경로 계산은 거래 발생 시점의 가맹점 계약 관계와 정산 경로 정책 스냅샷을 기준으로 서버에서 결정한다.
- 정산 경로, 지급 대상, 수수료 귀속 규칙은 코드에 하드코딩하지 않고 정책 데이터로 관리한다.
- 문서의 정산 경로 사례는 구현 예시이며 고정된 enum 또는 if 분기 목록으로 사용하지 않는다.
- 금액 계산은 정수 단위 또는 고정 소수점 기반으로 처리한다.
- 목록 화면은 기간, 리더, 파트너, 가맹점, 상태 기준 필터를 제공한다.
- 주요 목록은 페이지네이션을 제공한다.
- 개인정보와 KORION WALLET 주소는 접근 권한과 마스킹 정책을 적용한다.
- KORION WALLET 주소는 화면과 다운로드 파일에서 권한에 따라 전체 표시 또는 마스킹 표시를 적용한다.

## 10. 제외 범위

- 가맹점 전용 로그인 포털
- 외부 법정화폐 계좌 검증 연동
- 외부 지급 API 실시간 연동
- 세금계산서 발행
- 리더 상위의 추가 다단계 조직 구조
- 모바일 앱

## 11. 추천 기술 스택

### 11.1 프론트엔드

- 기본 프레임워크: React + TypeScript + Vite
- 라우팅: React Router
- 서버 상태 관리: TanStack Query
- 폼 관리: React Hook Form + Zod
- UI: Tailwind CSS + shadcn/ui 또는 Radix UI 기반 컴포넌트
- 차트/대시보드: Recharts
- 인증 연동: KORION WALLET 서명 요청 모듈을 별도 adapter로 분리
- 선택 이유: 운영 화면 중심의 SPA에 적합하고, 정산/지급 목록의 필터, 페이지네이션, 재조회, 캐시 무효화 처리가 단순하다.

### 11.2 백엔드

- 기본 프레임워크: Spring Boot + Java
- 빌드: Gradle
- API: REST JSON API 우선, 외부 연동이 늘어날 경우 OpenAPI 문서화 필수
- 인증/인가: Spring Security + JWT 세션 + KORION WALLET nonce 서명 검증
- 데이터 접근: Spring Data JDBC 또는 JPA
- 배치/스케줄: Spring Scheduler 또는 Spring Batch
- 정책 엔진: 정산 경로 정책, 수수료 정책, 지급 대상 정책은 DB 정책 테이블 기반으로 계산하고 코드에 하드코딩하지 않는다.
- 선택 이유: 정산, 지급, 감사 로그처럼 트랜잭션 무결성이 중요한 업무에 적합하고 기존 KORION Java 운영 방식과 맞다.

### 11.3 데이터베이스 및 마이그레이션

- 주 데이터베이스: PostgreSQL
- 마이그레이션: Flyway
- 캐시/락/임시 상태: Redis
- 주요 저장 원칙:
  - 정산 배치는 정책 계산 결과를 스냅샷으로 저장한다.
  - 지급 요청은 idempotency key를 가져야 한다.
  - KORION WALLET 주소 변경, 정산일 변경, 지급 상태 변경은 이력 테이블을 둔다.
  - 금액은 floating point를 쓰지 않고 정수 최소 단위 또는 fixed decimal로 저장한다.

### 11.4 이벤트 관리

- 1차 구현: Spring ApplicationEvent + TransactionalEventListener
- 비동기/재시도 필요 영역: DB outbox table + worker poller
- 대용량 또는 다중 서비스 확장 시: Apache Kafka
- Redis Streams 사용 가능 영역: 단일 서비스 내부의 가벼운 비동기 작업, 알림, 재시도 큐
- 이벤트 발행 원칙:
  - 정산/지급의 원장 상태 변경은 DB 트랜잭션이 먼저 성공해야 한다.
  - 외부 지급, 알림, 웹훅, 감사 로그 확장은 트랜잭션 이후 이벤트 리스너에서 처리한다.
  - 외부 지급 요청은 outbox 기반으로 발행해 중복 실행을 방지한다.
  - 이벤트 payload에는 정책 계산 결과 스냅샷 ID와 idempotency key를 포함한다.

### 11.5 주요 이벤트

- `LeaderCreated`
- `PartnerCreated`
- `MerchantCreated`
- `WalletAddressRegistered`
- `WalletAddressVerified`
- `SettlementPolicyChanged`
- `SettlementScheduleChanged`
- `SettlementBatchCreated`
- `SettlementConfirmed`
- `SettlementRejected`
- `PayoutRequested`
- `PayoutApproved`
- `PayoutSubmitted`
- `PayoutSucceeded`
- `PayoutFailed`

### 11.6 월렛 및 지급 처리

- KORION WALLET 인증: TRX 네트워크 주소 + nonce 서명 검증
- 지급 처리: 정산금 기준으로 가맹점, 파트너, 리더의 정산 수취 KORION WALLET TRX 주소로 입금
- 월렛 연동은 `WalletGateway` 인터페이스 뒤에 둔다.
- 지급 실행은 `PayoutProcessor` worker에서 처리한다.
- 온체인 거래 식별자 또는 외부 지급 참조값을 반드시 저장한다.
- 지급 요청은 대상, 정산 ID, 금액, 네트워크, 수취 주소 기준으로 중복 생성과 중복 지급을 막는다.

### 11.7 운영 및 배포

- 컨테이너: Docker
- 로컬/단일 서버 배포: Docker Compose
- 웹 서버/프록시: Nginx
- 관측성: Spring Boot Actuator + Prometheus + Grafana
- 로그: JSON structured logging
- 알림: 지급 실패, 정산 배치 실패, wallet 검증 실패율 증가 시 Telegram 또는 Slack 알림
- 비밀값: 환경변수 또는 서버 secret 파일로 관리하고 Git에 저장하지 않는다.

### 11.8 테스트

- 백엔드 단위 테스트: JUnit 5
- 백엔드 통합 테스트: Testcontainers + PostgreSQL + Redis
- API 계약 테스트: OpenAPI 기반 contract test
- 프론트 테스트: Vitest + Testing Library
- E2E 테스트: Playwright
- 필수 테스트 케이스:
  - 리더/파트너/가맹점 권한 격리
  - KORION WALLET nonce 서명 성공/실패
  - 정산 경로 정책 동적 계산
  - 정산일 변경 권한
  - 지급 idempotency
  - 지급 실패 후 재처리

### 11.9 우선 구현 순서

1. Spring Boot API + PostgreSQL + Flyway 기반 골격
2. React 운영 화면 + 인증/권한 라우팅
3. KORION WALLET 주소 등록과 nonce 서명 인증
4. 계약, 정산 경로 정책, 수수료 정책 테이블
5. 정산 배치 생성과 스냅샷 저장
6. 지급 요청과 KORION WALLET 지급 worker
7. outbox 이벤트와 이벤트 리스너
8. 운영 대시보드, 알림, 감사 로그

## 12. 작업큐 및 협업 체크리스트

이 섹션은 개발 착수 전 팀이 함께 확인할 작업큐이다. 아래 항목은 PRD 반영 대상이며, 구현은 담당자/범위/검수 기준이 확정된 뒤 진행한다.

### 12.1 공통 진행 원칙

- [ ] PRD 변경 사항을 먼저 리뷰하고, 확정된 항목만 개발 이슈로 분리한다.
- [ ] 정산 경로, 지급 대상, 수수료 귀속 규칙은 정책 데이터 기반으로 설계하고 하드코딩하지 않는다.
- [ ] 리더, 파트너, 가맹점 권한 경계는 API와 UI 양쪽에서 검증한다.
- [ ] KORION WALLET TRX 주소는 인증 주소와 정산 수취 주소의 관계를 명확히 정의한 뒤 구현한다.
- [ ] 개발 착수 전 API 계약, 화면 범위, mock 데이터 범위, 실제 연동 범위를 구분한다.
- [ ] 커밋/푸시/배포는 최신 요청에서 명시된 경우에만 수행한다.

### 12.2 `kori_partners` 참고 및 연동 범위

- [x] `kori_partners` 프로젝트를 참고 프로젝트로 사용한다.
- [x] workspace에 없으면 `https://github.com/ansj1105/kori_partners.git`에서 클론한다.
- [x] 기존 프로젝트의 라우팅, i18n, 컴포넌트 구조, mock JSON hook 패턴을 먼저 파악한다.
- [x] 기존 메뉴/IA를 임의 재구성하지 않고, 요청된 국가 리더 대시보드 범위 안에서만 변경한다.
- [x] `kori_partners`에서 진행된 인증/대시보드 연동 변경은 리뷰 후 커밋/푸시 완료 상태로 관리한다.

### 12.2.1 로그인/회원가입 진입 요구사항

- [x] Purpose: 권한별 로그인과 파트너/가맹점 회원가입 진입을 제공한다.
- [x] Permission: 전체 사용자가 접근 가능하다.
- [x] Main Features: 리더/파트너/가맹점 로그인 카드, 파트너/가맹점 가입 선택, 보안 안내.
- [x] Input Fields: 없음 또는 진입 선택값.
- [x] Actions: 로그인 화면 이동, 회원가입 화면 이동.
- [x] Status: 권한별 라우팅 필요.
- [x] Validation: 선택한 로그인 카드와 실제 인증 후 역할이 일치해야 한다.
- [x] Validation: 리더 회원가입 진입은 일반 사용자에게 노출하지 않고 본사 또는 운영 생성 플로우로 제한한다.
- [x] Validation: 파트너/가맹점 회원가입 진입은 승인 요청 플로우로 연결하고 즉시 권한을 부여하지 않는다.
- [x] Data Notes: Auth routing, role selection, ActivityLog 시작점으로 사용한다.
- [ ] Data Notes: 로그인 성공/실패, 역할 불일치, 회원가입 진입 선택은 ActivityLog 또는 AuthActivityLog에 기록한다.
- [ ] Data Notes: 로그인 카드별 목적지는 `leader`, `partner`, `merchant` role code로 관리하고 화면 문구에 하드코딩하지 않는다.

### 12.2.2 리더 로그인 요구사항

- [ ] Purpose: 본사에서 발급/승인된 국가 리더가 Portal에 접속한다.
- [ ] Permission: 국가 리더만 로그인 성공 후 리더 Portal에 접근 가능하다.
- [ ] Main Features: 아이디/비밀번호, 2FA, 로그인 유지, 비밀번호 찾기.
- [ ] Input Fields: 아이디, 비밀번호, 2FA 코드.
- [ ] Actions: 로그인, 비밀번호 찾기.
- [ ] Status: `계정 활성`, `계정 정지`, `2FA 검증`.
- [x] Validation: 본사에서 발급 또는 승인된 국가 리더 계정만 `/leader/dashboard`로 진입할 수 있다.
- [x] Validation: 계정 정지, 비활성, 승인 전 상태는 로그인 성공 처리하지 않는다.
- [ ] Validation: 2FA가 활성화된 리더 계정은 2FA 코드 검증 전 세션을 발급하지 않는다.
- [ ] Validation: 로그인 유지 옵션은 보안 정책의 만료 시간과 기기 신뢰 정책을 따른다.
- [x] Data Notes: 로그인 성공 시 `/leader/dashboard`로 이동하고 자기 국가 scope를 적용한다.
- [ ] Data Notes: 리더 로그인 성공/실패, 2FA 성공/실패, 비밀번호 찾기 요청은 ActivityLog 또는 AuthActivityLog에 기록한다.
- [ ] Data Notes: 로그인 응답은 leaderId, country scope, 2FA 필요 여부, 세션 만료 정보를 포함한다.

### 12.2.3 파트너 로그인 요구사항

- [ ] Purpose: 세일즈 파트너가 본인 하위 가맹점과 정산 화면에 접근한다.
- [ ] Permission: 세일즈 파트너만 로그인 성공 후 파트너 Portal에 접근 가능하다.
- [ ] Main Features: 로그인, 2FA, 세션 유지.
- [ ] Input Fields: 아이디, 비밀번호, 2FA 코드.
- [ ] Actions: 로그인, 비밀번호 찾기.
- [ ] Status: `활성`, `비활성`, `정지`.
- [x] Validation: 활성 상태의 세일즈 파트너만 `/partner/dashboard`로 진입할 수 있다.
- [x] Validation: 비활성 또는 정지 상태의 파트너는 하위 가맹점과 정산 화면 접근을 차단한다.
- [ ] Validation: 2FA가 활성화된 파트너 계정은 2FA 코드 검증 전 세션을 발급하지 않는다.
- [ ] Validation: 로그인 후 모든 파트너 API는 own merchants scope를 서버에서 적용한다.
- [x] Data Notes: 로그인 성공 시 `/partner/dashboard`로 이동하고 own merchants scope를 적용한다.
- [ ] Data Notes: 파트너 로그인 성공/실패, 2FA 성공/실패, 비밀번호 찾기 요청은 ActivityLog 또는 AuthActivityLog에 기록한다.
- [ ] Data Notes: 로그인 응답은 partnerId, role, own merchant scope, 2FA 필요 여부, 세션 만료 정보를 포함한다.

### 12.2.4 가맹점 로그인 요구사항

- [ ] Purpose: 가맹점 운영자가 자기 매장 데이터에 접근한다.
- [ ] Permission: 가맹점만 로그인 성공 후 가맹점 Portal에 접근 가능하다.
- [ ] Main Features: 로그인, 2FA, 매장 단위 권한 검증.
- [ ] Input Fields: 아이디, 비밀번호, 2FA 코드.
- [ ] Actions: 로그인, 비밀번호 찾기.
- [ ] Status: `매장 활성`, `결제 가능`, `결제 제한`, `비활성`.
- [x] Validation: 활성 매장에 연결된 가맹점 운영자만 `/merchant/dashboard`로 진입할 수 있다.
- [ ] Validation: 매장 비활성 또는 결제 제한 상태에서는 결제/정산/매장 운영 기능 접근을 정책에 따라 제한한다.
- [ ] Validation: 2FA가 활성화된 가맹점 계정은 2FA 코드 검증 전 세션을 발급하지 않는다.
- [ ] Validation: 로그인 후 모든 가맹점 API는 own store scope를 서버에서 적용한다.
- [x] Data Notes: 로그인 성공 시 `/merchant/dashboard`로 이동하고 own store scope를 적용한다.
- [ ] Data Notes: 가맹점 로그인 성공/실패, 2FA 성공/실패, 비밀번호 찾기 요청은 ActivityLog 또는 AuthActivityLog에 기록한다.
- [ ] Data Notes: 로그인 응답은 merchantId, storeId, role, own store scope, 결제 상태, 2FA 필요 여부, 세션 만료 정보를 포함한다.

### 12.2.5 파트너 회원가입 요구사항

- [ ] Purpose: 리더 코드 또는 본사 직접 계약으로 파트너 가입을 신청한다.
- [ ] Permission: 파트너 신청자가 접근 가능하다.
- [ ] Main Features: 계정정보, 소속정보, 영업역량, KORION WALLET, 동의/제출.
- [ ] Input Fields: 아이디, 이메일, 인증번호, 리더코드, KORION WALLET, 활동정보.
- [ ] Actions: 중복확인, 인증발송, 가입신청.
- [ ] Status: `승인요청`, `대기`, `비활성`.
- [x] Validation: 이메일 인증은 가입신청 제출 전 필수이다.
- [x] Validation: 아이디와 이메일은 중복 확인을 통과해야 한다.
- [x] Validation: 리더코드가 있으면 유효한 국가 리더 또는 상위 조직 코드인지 검증한다.
- [x] Validation: 리더코드가 없으면 본사 직접 계약 신청으로 분류한다.
- [ ] Validation: KORION WALLET TRX 주소는 형식, 중복, nonce 서명 인증 정책을 따른다.
- [x] Data Notes: 최종 승인과 권한 활성화는 본사 또는 상위 검토 후 처리한다.
- [x] Data Notes: 가입신청은 즉시 파트너 권한을 부여하지 않고 파트너 신청/검토 레코드로 저장한다.
- [ ] Data Notes: 중복확인, 인증발송, 이메일 인증 성공/실패, 가입신청 제출은 ActivityLog 또는 AuthActivityLog에 기록한다.

### 12.2.6 가맹점 회원가입 요구사항

- [ ] Purpose: 리더 코드, 파트너 코드, 본사 직접 계약 중 하나로 가입한다.
- [ ] Permission: 가맹점 신청자가 접근 가능하다.
- [ ] Main Features: 매장정보, 소속코드, 결제설정, KORION WALLET, 검증자료.
- [ ] Input Fields: 아이디, 이메일, 매장명, 주소, 업종, KORION WALLET, 증빙자료.
- [ ] Actions: 코드확인, 중복확인, 가입신청.
- [ ] Status: `승인요청`, `매장대기`, `결제비활성`.
- [x] Validation: 리더 코드, 파트너 코드, 본사 직접 계약 중 하나의 계약 경로가 필요하다.
- [x] Validation: 리더/파트너 코드는 유효한 상위 조직과 계약 가능 상태인지 검증한다.
- [ ] Validation: 매장명, 주소, 업종, 증빙자료는 중복 가맹점 검토 대상이다.
- [ ] Validation: KORION WALLET TRX 주소는 형식, 중복, nonce 서명 인증 정책을 따른다.
- [x] Data Notes: 승인 전 결제 기능은 비활성 상태로 유지한다.
- [ ] Data Notes: 중복 가맹점 의심은 RiskFlag 또는 검토 사유로 기록한다.
- [x] Data Notes: 가입신청은 즉시 가맹점 권한과 결제 권한을 부여하지 않고 가맹점 신청/검토 레코드로 저장한다.
- [ ] Data Notes: 코드확인, 중복확인, 가입신청 제출, 증빙자료 업로드는 ActivityLog 또는 AuthActivityLog에 기록한다.

### 12.2.7 확인 모달/토스트 요구사항

- [x] Purpose: 가입, 인증, 중복확인, 제출 결과를 사용자에게 피드백한다.
- [x] Permission: 전체 사용자가 사용할 수 있다.
- [x] Main Features: 확인 모달, 오류 모달, 성공 토스트.
- [ ] Input Fields: 해당 없음.
- [ ] Actions: 확인, 취소, 재시도.
- [x] Status: `성공`, `실패`, `중복`, `인증완료`.
- [x] Validation: 중복확인, 이메일 인증, Wallet 인증, 가입신청 제출 등 중요 액션은 결과 상태를 명확히 표시한다.
- [ ] Validation: 실패 또는 중복 상태는 재시도 가능 여부와 다음 액션을 함께 표시한다.
- [ ] Validation: 보안상 민감한 실패 사유는 사용자에게 과도하게 노출하지 않는다.
- [ ] Data Notes: 모든 중요 액션은 ActivityLog 또는 AuditLog에 기록한다.
- [ ] Data Notes: 모달/토스트 문구와 상태 코드는 프론트 하드코딩을 최소화하고 공통 메시지 정책으로 관리한다.
- [ ] Data Notes: 동일 액션 재시도는 requestId 또는 idempotency key로 추적 가능해야 한다.

### 12.2.8 로그인/회원가입 화면 매트릭스

- [ ] 공통 Main Features: 권한별 진입, 로그인, 회원가입, 이메일 인증, 중복 확인, KORION WALLET 연동.
- [x] 공통 Input Fields: 아이디, 비밀번호, 이메일, 인증번호, 휴대폰/WhatsApp, Telegram, 코드, KORION WALLET 주소.
- [ ] 공통 Actions: 로그인, 회원가입 시작, 중복 확인, 인증번호 발송, 인증 확인, Wallet 연동, 가입 신청.
- [x] 공통 Status: `승인요청`, `검토중`, `자료요청`, `보류`, `승인`, `거절`, `대기`, `비활성`, `Wallet 미연동`, `Wallet 연동 완료`.
- [x] 공통 Validation: 필수값, 이메일 인증, 아이디/이메일/Telegram/WhatsApp/Wallet 중복 확인, 코드 유효성.
- [x] 공통 Modal / Toast: 가입 확인, 중복 경고, 인증 완료 토스트, 가입 신청 완료 토스트.
- [x] 공통 Data Notes: `users`는 기존 인증 주체, `partner_applications`는 승인 전 가입신청, `ReferralCode`, `WalletLink`, `ApprovalWorkflow`는 총판 도메인 보조 모델로 사용한다.
- [x] 공통 Dev Notes: 승인 전 권한은 비활성 상태로 유지한다.
- [x] 공통 Dev Notes: 개인키, 시드문구, PIN 입력은 금지한다.
- [x] 공통 Dev Notes: role 기반 라우팅은 필수이다.
- [x] 화면 1. 로그인/회원가입 메인: 가입 신청자와 전체 사용자를 대상으로 권한별 로그인/회원가입 진입 목적과 기능을 정의한다.
- [x] 화면 2. 리더 로그인: 국가 리더가 본사 발급/승인 계정으로 로그인하고 성공 시 `/leader/dashboard`로 이동한다.
- [x] 화면 3. 파트너 로그인: 세일즈 파트너가 로그인하고 성공 시 `/partner/dashboard`와 own merchants scope를 적용받는다.
- [x] 화면 4. 가맹점 로그인: 가맹점 운영자가 로그인하고 성공 시 `/merchant/dashboard`와 own store scope를 적용받는다.
- [x] 화면 5. 파트너 회원가입: 가입 신청자가 리더 코드 또는 본사 직접 계약 기준으로 파트너 가입을 신청한다.
- [x] 화면 6. 가맹점 회원가입: 가입 신청자가 리더 코드, 파트너 코드, 본사 직접 계약 중 하나로 가맹점 가입을 신청한다.
- [ ] 화면 7. KORION Wallet 연동: 가입 신청자와 전체 사용자가 KORION WALLET 주소를 연동하고 Wallet 미연동/연동 완료 상태를 확인한다.
- [x] 화면 8. 이메일 인증/중복 확인: 가입 신청자와 전체 사용자가 이메일 인증과 아이디/이메일/Telegram/WhatsApp/Wallet 중복 확인을 수행한다.
- [x] 화면 9. 확인 모달/토스트: 가입, 인증, 중복 확인, Wallet 연동, 제출 결과를 공통 모달/토스트 정책으로 피드백한다.
- [x] API 후보: `POST /api/auth/login`
- [x] API 후보: `POST /api/auth/signup-applications`
- [x] API 후보: `POST /api/auth/email-verifications/send`
- [x] API 후보: `POST /api/auth/email-verifications/confirm`
- [x] API 후보: `GET /api/auth/availability`
- [x] API 후보: `POST /api/auth/wallet-links/verify`
- [x] API 후보: `GET /api/auth/referral-codes/{code}/validate`

### 12.2.9 총판 가입신청 저장소 및 상태 전이 확정안

- [x] `users` 테이블은 기존 일반 사용자/지갑 계정의 인증 주체로 유지하고, 총판 회원가입/조직/정산 도메인 상태를 직접 소유하지 않는다.
- [x] 공개 파트너/가맹점 가입신청은 `partner_applications`에 저장한다.
- [x] `partner_applications`는 승인 전 신청 레코드이며, 생성 시점에 `users`, `partners`, `merchants` 권한을 생성하거나 활성화하지 않는다.
- [x] 가입신청의 비밀번호는 원문 저장을 금지하고 `partner_applications.password_hash`에 해시로만 저장한다.
- [x] 최종 승인 후 본사/상위 검토 API가 필요한 `users`, `partners`, `merchants`, `distributor_contracts`, `distributor_wallet_addresses`를 생성 또는 연결한다.
- [x] `partner_applications`는 기존 마케팅/제휴 신청 호환성을 유지하면서 총판 포털 회원가입의 1차 저장소로 사용한다.
- [x] 표준 상태 코드는 프론트/백엔드/API에서 동일하게 사용한다.
- [ ] 신청 상태 전이: `REQUESTED -> REVIEWING -> NEED_MORE_INFO/HOLD/APPROVED/REJECTED/CANCELLED`.
- [ ] 자료요청 보완 후 상태 전이: `NEED_MORE_INFO -> REVIEWING`.
- [ ] 보류 재검토 상태 전이: `HOLD -> REVIEWING/REJECTED`.
- [x] 승인 후 권한 활성화는 신청 상태가 아니라 승인 API의 후속 생성/연결 결과로 처리한다.
- [x] 화면 라벨 매핑: `REQUESTED=승인요청`, `REVIEWING=검토중`, `NEED_MORE_INFO=자료요청`, `HOLD=보류`, `APPROVED=승인`, `REJECTED=거절`, `CANCELLED=취소`.
- [x] 승인 전 `대기`, `매장대기`, `결제비활성` 같은 화면 상태는 `partner_applications.status`와 승인 후 `partners/merchants` 상태를 조합해 표시한다.
- [x] 최종 승인과 권한 활성화는 분리한다. 신청 `APPROVED`만으로 로그인 권한, 파트너 권한, 가맹점 결제 권한을 발급하지 않는다.
- [ ] 상태 전이는 `ApprovalWorkflow` 또는 상태 이력 테이블에 actor, reason, requested materials, createdAt을 남긴다.

### 12.2.10 WalletLink nonce 인증 확정안

- [x] Wallet 연동은 KORION Wallet address 기반 인증으로 확정한다.
- [x] 인증 대상 주소는 KORION WALLET TRX 네트워크 주소이다.
- [ ] WalletLink 생성 전 아이디/이메일/Telegram/WhatsApp 기본 중복확인을 완료해야 한다.
- [ ] `Wallet 미연동` 상태는 Wallet 주소가 없거나 nonce 서명 검증이 완료되지 않은 상태이다.
- [ ] `Wallet 연동 완료` 상태는 서버 발급 nonce에 대한 KORION Wallet address 서명이 검증된 상태이다.
- [ ] nonce는 1회성, 짧은 만료 시간, requestId와 연결된 challenge로 발급한다.
- [x] 서버는 서명 원문, 개인키, 시드문구, PIN을 저장하거나 요구하지 않는다.
- [x] Wallet 주소 중복 확인은 승인 완료 계정뿐 아니라 `REQUESTED`, `REVIEWING`, `NEED_MORE_INFO`, `HOLD`, `APPROVED` 상태의 신청 건까지 포함한다.
- [x] Wallet 인증 성공/실패 결과는 ActivityLog 또는 AuditLog에 기록한다.
- [ ] Wallet nonce 발급과 재시도 이력은 nonce 발급 API 구현 후 ActivityLog 또는 AuditLog에 기록한다.

### 12.3 국가 리더 API/대시보드 작업큐

- [x] Purpose: 국가 리더의 담당 국가 운영 현황을 요약한다.
- [x] Permission: 국가 리더만 접근 가능하다.
- [x] Validation: 국가 리더는 본인에게 배정된 국가 scope 데이터만 조회할 수 있다.
- [x] Input Fields: 기간, 국가 scope.
- [x] Main Features: KPI, 하부 조직, 월 거래량, 수수료, 리스크 알림.
- [ ] Actions: 상세 이동, 리포트 이동.
- [x] Data Notes: `LeaderProfile`, `Partner summary`, `Merchant summary`를 기본 데이터 단위로 사용한다.
- [x] API는 국가 scope 권한 검증 후 응답해야 하며, 프론트 필터만 믿지 않는다.
- [x] 대시보드 데이터는 정산/거래 원천 데이터를 직접 계산하지 않고, API가 제공하는 summary 응답을 표시한다.
- [ ] 상세 이동 대상은 파트너 목록, 가맹점 목록, 거래 로그, 정산 리포트 중 어떤 화면으로 갈지 확정한다.
- [ ] 리스크 알림 기준은 실패 거래, 월렛 미인증, 정산일 변경, 비정상 거래량 등으로 후보를 두고 운영 기준을 확정한다.

### 12.4 국가 리더 대시보드 API 후보

- [x] `GET /api/leader/dashboard`
- [x] Query: `period`, `countryScope`.
- [x] Auth context: `leaderId`, 허용 `countryScopes`.
- [x] Response: `leaderProfile`, `kpis`, `organizationSummary`, `monthlyVolume`, `feeSummary`, `riskAlerts`.
- [x] Error: 요청한 `countryScope`가 리더 권한 범위 밖이면 `403` 또는 표준 권한 오류를 반환한다.
- [x] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.5 세일즈파트너 등록 요청관리 작업큐

- [ ] Purpose: 리더가 하위 파트너 후보를 등록하고 본사 승인 요청을 보낸다.
- [ ] Permission: 국가 리더만 접근 가능하다.
- [ ] Main Features: 파트너 후보 등록, 상태 확인, 자료요청 대응.
- [ ] Input Fields: 파트너명, 연락처, 이메일, 지역, KORION WALLET, 실행계획.
- [ ] Actions: 임시저장, 본사 승인 요청.
- [ ] Status: `임시저장`, `승인요청`, `검토중`, `자료요청`, `보류`, `승인`, `거절`.
- [ ] Validation: 리더는 본인 국가 scope 안의 파트너 후보만 등록할 수 있다.
- [ ] Validation: KORION WALLET TRX 주소는 형식과 중복 여부를 검증한다.
- [ ] Validation: 본사 승인 요청 시 필수 입력값과 실행계획 첨부/내용을 검증한다.
- [ ] Data Notes: 리더는 최종 승인할 수 없고, 본사 승인 요청까지만 수행한다.
- [ ] Data Notes: 본사 최종 승인 전까지 파트너 후보는 정식 파트너로 집계하지 않는다.
- [ ] Data Notes: 자료요청 상태에서는 리더가 요청된 자료를 보완해 재제출할 수 있다.
- [ ] Audit: 임시저장, 승인요청, 자료보완, 본사 상태 변경 이력을 남긴다.

### 12.6 세일즈파트너 등록 요청 API 후보

- [ ] `GET /api/leader/partner-requests`
- [ ] Query: `status`, `period`, `countryScope`, `keyword`.
- [ ] Response: 파트너 후보 목록, 상태, 마지막 변경일, 자료요청 여부.
- [ ] `POST /api/leader/partner-requests/drafts`
- [ ] Body: 파트너명, 연락처, 이메일, 지역, KORION WALLET, 실행계획.
- [ ] Action: 임시저장.
- [ ] `POST /api/leader/partner-requests/{requestId}/submit`
- [ ] Action: 본사 승인 요청.
- [ ] `POST /api/leader/partner-requests/{requestId}/materials`
- [ ] Action: 자료요청 대응 및 보완 제출.
- [ ] Error: 리더 국가 scope 밖 지역이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 이미 본사 검토가 완료된 요청은 리더가 수정할 수 없다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.7 파트너 관리 작업큐

- [x] Purpose: 승인된 하부 파트너 목록과 성과를 관리한다.
- [x] Permission: 국가 리더만 접근 가능하다.
- [ ] Main Features: 파트너 목록, 파트너 상세, 활동 메모, 정지 요청, 권한 변경 요청.
- [ ] Input Fields: 검색, 상태, 지역, 메모.
- [ ] Actions: 상세보기, 본사 정지 요청.
- [ ] Status: `활성`, `비활성`, `정지요청`.
- [x] Validation: 국가 리더는 본인 국가 scope와 하위 조직에 속한 승인 파트너만 조회할 수 있다.
- [x] Validation: 정식 승인되지 않은 파트너 후보는 파트너 관리 목록에 포함하지 않는다.
- [ ] Validation: 리더는 파트너를 직접 정지 처리할 수 없고 본사 정지 요청만 생성할 수 있다.
- [ ] Data Notes: 상위 조직 변경은 본사 권한이다.
- [ ] Data Notes: 활동 메모는 파트너 단위 이력으로 저장하고 작성자, 작성일, 수정일을 남긴다.
- [ ] Data Notes: 권한 변경 요청은 리더가 요청하고 본사가 승인/거절한다.
- [ ] Audit: 상세 조회, 메모 작성/수정, 정지 요청, 권한 변경 요청 이력을 남긴다.

### 12.8 파트너 관리 API 후보

- [x] `GET /api/leader/partners`
- [x] Query: `keyword`, `status`, `region`, `countryScope`, `page`, `size`.
- [x] Response: 승인된 하부 파트너 목록, 성과 요약, 상태, 최근 활동일.
- [ ] `GET /api/leader/partners/{partnerId}`
- [ ] Response: 파트너 상세, 성과, 하위 가맹점 요약, 활동 메모, 상태 이력.
- [ ] `POST /api/leader/partners/{partnerId}/notes`
- [ ] Body: 메모.
- [ ] Action: 활동 메모 추가.
- [ ] `POST /api/leader/partners/{partnerId}/suspension-requests`
- [ ] Body: 정지 요청 사유, 근거 메모.
- [ ] Action: 본사 정지 요청 생성.
- [ ] `POST /api/leader/partners/{partnerId}/permission-change-requests`
- [ ] Body: 요청 권한, 요청 사유.
- [ ] Action: 본사 권한 변경 요청 생성.
- [ ] Error: 리더 국가 scope 밖 파트너이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 상위 조직 변경 요청은 리더 API에서 허용하지 않는다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.9 가맹점 관리 작업큐

- [ ] Purpose: 자기 국가/조직 내 가맹점을 관리한다.
- [ ] Permission: 국가 리더만 접근 가능하다.
- [ ] Main Features: 가맹점 목록, 승인대기, 문제 가맹점, 매장 상태.
- [ ] Input Fields: 검색, 도시, 상태, 업종.
- [ ] Actions: 상세보기, 재검토 요청.
- [ ] Status: `대기`, `활성`, `정지`, `문제`.
- [ ] Validation: 국가 리더는 본인 국가 scope와 하위 조직에 속한 가맹점만 조회할 수 있다.
- [ ] Validation: 리더는 가맹점 상태를 직접 최종 승인/정지 처리하지 않고, 재검토 요청 또는 본사/정책 플로우를 따른다.
- [ ] Data Notes: 허위 가맹점 의심은 `RiskFlag`로 처리한다.
- [ ] Data Notes: 문제 가맹점은 `RiskFlag` 상태, 사유, 탐지 기준, 처리 상태를 함께 표시한다.
- [ ] Data Notes: 매장 상태는 영업중, 일시중지, 폐점요청, 검수필요 등 운영 상태와 분리해 정의한다.
- [ ] Audit: 상세 조회, 재검토 요청, RiskFlag 생성/변경, 매장 상태 변경 이력을 남긴다.

### 12.10 가맹점 관리 API 후보

- [ ] `GET /api/leader/merchants`
- [ ] Query: `keyword`, `city`, `status`, `industry`, `countryScope`, `page`, `size`.
- [ ] Response: 가맹점 목록, 승인 상태, 매장 상태, 담당 파트너/리더, RiskFlag 요약.
- [ ] `GET /api/leader/merchants/{merchantId}`
- [ ] Response: 가맹점 상세, 계약 경로, 매장 상태, 거래 요약, 정산 요약, RiskFlag 이력.
- [ ] `POST /api/leader/merchants/{merchantId}/review-requests`
- [ ] Body: 재검토 사유, 요청 유형, 근거 메모.
- [ ] Action: 재검토 요청 생성.
- [ ] `GET /api/leader/merchants/risk-flags`
- [ ] Query: `severity`, `status`, `city`, `industry`.
- [ ] Response: 허위 의심 또는 문제 가맹점 RiskFlag 목록.
- [ ] Error: 리더 국가 scope 밖 가맹점이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 리더가 직접 승인/정지 최종 상태를 변경하는 API는 제공하지 않는다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.11 거래로그 작업큐

- [ ] Purpose: 하부 조직 거래와 오프라인 결제 상태를 확인한다.
- [ ] Permission: 국가 리더만 접근 가능하다.
- [ ] Main Features: 전체 거래 로그, 오프라인 거래 로그, 실패 거래 로그, 취소 거래 로그, 환불 거래 로그.
- [ ] Input Fields: 기간, 가맹점, 파트너, 결제상태.
- [ ] Actions: 상세보기, Excel 다운로드.
- [ ] Status: `성공`, `실패`, `환불`, `취소`, `Sync 대기`.
- [ ] Validation: 국가 리더는 본인 국가 scope와 하위 조직 거래만 조회할 수 있다.
- [ ] Validation: Excel 다운로드도 조회 권한과 동일한 국가 scope/조직 필터를 적용해야 한다.
- [ ] Data Notes: Sync 실패 또는 장기 Sync 대기는 정산 보류 후보로 표시한다.
- [ ] Data Notes: 오프라인 결제는 로컬 완료, 서버 Sync, 정산 반영 상태를 분리해 표시한다.
- [ ] Data Notes: 실패/취소/환불 거래는 정산 대상 제외 또는 조정 후보로 분리한다.
- [ ] Audit: 거래 상세 조회와 Excel 다운로드 이력을 남긴다.

### 12.12 거래로그 API 후보

- [ ] `GET /api/leader/transactions`
- [ ] Query: `periodFrom`, `periodTo`, `merchantId`, `partnerId`, `paymentStatus`, `channel`, `countryScope`, `page`, `size`.
- [ ] Response: 거래 목록, 결제 상태, 오프라인 Sync 상태, 정산 반영 상태, 가맹점/파트너 요약.
- [ ] `GET /api/leader/transactions/{transactionId}`
- [ ] Response: 거래 상세, 결제 증빙, 오프라인 Sync 이력, 정산 영향, 실패/취소/환불 사유.
- [ ] `GET /api/leader/transactions/export`
- [ ] Query: 목록 조회와 동일한 필터.
- [ ] Action: 권한 범위 내 Excel 다운로드.
- [ ] Error: 리더 국가 scope 밖 거래이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: Excel 다운로드 요청이 허용 건수 또는 기간 제한을 초과하면 표준 제한 오류를 반환한다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.13 수수료 정산신청 작업큐

- [ ] Purpose: 확정 수수료를 기준으로 본사에 정산을 요청한다.
- [ ] Permission: 국가 리더만 접근 가능하다.
- [ ] Main Features: 정산 가능금액 계산, 파트너 자동정산, 보류 제외.
- [ ] Input Fields: 정산기간, KORION WALLET, 신청금액, 메모.
- [ ] Actions: 임시저장, 본사 정산 요청.
- [ ] Status: `본사 검토중`, `승인`, `지급완료`, `보류`.
- [ ] Validation: 당일 거래는 정산 신청 대상에서 제외한다.
- [ ] Validation: 마지막 정산일 다음날부터 신청일 전일까지의 확정 거래만 정산 대상이다.
- [ ] Validation: 실패, 취소, 환불, Sync 대기, Sync 실패, RiskFlag 보류 거래는 정산 가능금액에서 제외한다.
- [ ] Validation: 신청금액은 정산 가능금액을 초과할 수 없다.
- [ ] Validation: 지급 대상 KORION WALLET TRX 주소가 인증되지 않았으면 본사 정산 요청을 생성할 수 없다.
- [ ] Data Notes: 파트너 자동정산 대상은 정산 경로 정책과 지급 대상 정책 기준으로 계산한다.
- [ ] Data Notes: 보류 제외 금액과 제외 사유를 정산 신청 상세에 표시한다.
- [ ] Data Notes: 임시저장 상태의 정산 신청은 확정 거래 재계산 결과와 차이가 날 수 있으므로 제출 시 재검증한다.
- [ ] Audit: 임시저장, 본사 정산 요청, 금액 재계산, 보류 제외, 본사 상태 변경 이력을 남긴다.

### 12.14 수수료 정산신청 API 후보

- [ ] `GET /api/leader/settlement-requests/available`
- [ ] Query: `settlementPeriodFrom`, `settlementPeriodTo`, `countryScope`.
- [ ] Response: 정산 가능금액, 보류 제외 금액, 제외 사유, 마지막 정산일, 대상 거래 기간.
- [ ] `POST /api/leader/settlement-requests/drafts`
- [ ] Body: 정산기간, KORION WALLET, 신청금액, 메모.
- [ ] Action: 임시저장.
- [x] `POST /api/leader/settlement-requests`
- [x] Body: 정산기간, KORION WALLET, 신청금액, 메모.
- [x] Action: 본사 정산 요청.
- [x] `POST /api/leader/settlement-requests/{settlementRequestId}/approve`
- [x] `POST /api/leader/settlement-requests/{settlementRequestId}/reject`
- [x] `POST /api/leader/settlement-requests/{settlementRequestId}/mark-paid`
- [ ] `GET /api/leader/settlement-requests`
- [ ] Query: `status`, `period`, `countryScope`, `page`, `size`.
- [ ] Response: 정산 신청 목록, 상태, 신청금액, 지급 상태, 보류 제외 금액.
- [ ] Error: 당일 거래가 포함된 기간이면 표준 검증 오류를 반환한다.
- [ ] Error: 마지막 정산일 이전 거래가 포함되면 표준 검증 오류를 반환한다.
- [x] Error: 정산 가능금액 초과 신청이면 표준 검증 오류를 반환한다.
- [x] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.15 정산내역 작업큐

- [x] Purpose: 정산 요청, 승인, 지급, 보류/조정 내역을 확인한다.
- [x] Permission: 국가 리더만 접근 가능하다.
- [x] Main Features: 정산 테이블, 상태 필터, 상세 패널.
- [ ] Input Fields: 검색, 기간, 상태.
- [ ] Actions: 상세보기, 다운로드.
- [ ] Status: `지급완료`, `보류`, `조정필요`.
- [x] Validation: 국가 리더는 본인 국가 scope와 하위 조직 관련 정산내역만 조회할 수 있다.
- [ ] Validation: 다운로드도 조회 권한과 동일한 국가 scope/조직 필터를 적용해야 한다.
- [x] Data Notes: `SettlementRequest`와 `Commission`을 연결해 요청금액, 승인금액, 지급금액, 보류/조정 금액을 표시한다.
- [x] Data Notes: 보류/조정 내역은 원 거래, 보류 사유, 조정 사유, 처리 상태를 함께 표시한다.
- [ ] Data Notes: 지급완료 내역은 KORION WALLET 지급 주소와 온체인 거래 식별자 또는 외부 지급 참조값을 연결한다.
- [ ] Audit: 상세 조회와 다운로드 이력을 남긴다.

### 12.16 정산내역 API 후보

- [x] `GET /api/leader/settlement-history`
- [ ] Query: `keyword`, `periodFrom`, `periodTo`, `status`, `countryScope`, `page`, `size`.
- [x] Response: 정산내역 테이블, `SettlementRequest` 요약, `Commission` 요약, 지급 상태, 보류/조정 상태.
- [x] `GET /api/leader/settlement-history/{settlementRequestId}`
- [x] Response: 정산 요청 상세, 승인/보류/조정 이력, 연결된 Commission 목록, 지급 내역.
- [ ] `GET /api/leader/settlement-history/export`
- [ ] Query: 목록 조회와 동일한 필터.
- [ ] Action: 권한 범위 내 다운로드.
- [ ] Error: 리더 국가 scope 밖 정산내역이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 다운로드 요청이 허용 건수 또는 기간 제한을 초과하면 표준 제한 오류를 반환한다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.17 본사 소식지 작업큐

- [ ] Purpose: 본사에서 리더에게 보낸 공지와 정책을 확인한다.
- [ ] Permission: 국가 리더만 접근 가능하다.
- [ ] Main Features: 공지 목록, 상세보기, 읽음 상태.
- [ ] Input Fields: 검색, 공지유형.
- [ ] Actions: 상세보기, 읽음 처리.
- [ ] Status: `읽음`, `미확인`.
- [ ] Validation: 국가 리더는 본인 국가 scope 또는 본인에게 발송된 본사 공지만 조회할 수 있다.
- [ ] Validation: 리더는 본사 공지를 작성, 수정, 삭제할 수 없다.
- [ ] Data Notes: 본사 공지는 리더가 수신만 가능하다.
- [ ] Data Notes: 공지유형은 정책, 정산, 장애, 리스크, 일반 공지 등 정책 데이터로 관리한다.
- [ ] Data Notes: 읽음 상태는 리더별 수신자 기준으로 저장한다.
- [ ] Audit: 상세 조회, 읽음 처리 이력을 남긴다.

### 12.18 본사 소식지 API 후보

- [ ] `GET /api/leader/hq-notices`
- [ ] Query: `keyword`, `noticeType`, `readStatus`, `countryScope`, `page`, `size`.
- [ ] Response: 본사 공지 목록, 공지유형, 제목, 발송일, 읽음 상태.
- [ ] `GET /api/leader/hq-notices/{noticeId}`
- [ ] Response: 본사 공지 상세, 첨부/정책 링크, 읽음 상태.
- [ ] `POST /api/leader/hq-notices/{noticeId}/read`
- [ ] Action: 읽음 처리.
- [ ] Error: 리더 수신 대상이 아닌 공지이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 리더 API에서는 본사 공지 작성/수정/삭제를 제공하지 않는다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.19 공지보내기 작업큐

- [x] Purpose: 하위 파트너와 가맹점에게 공지를 발송한다.
- [x] Permission: 국가 리더만 접근 가능하다.
- [ ] Main Features: 대상 선택, 채널 선택, 즉시/예약 발송, 읽음확인.
- [x] Input Fields: 제목, 유형, 대상, 내용, 채널, 예약시간.
- [ ] Actions: 임시저장, 미리보기, 발송.
- [ ] Status: `예약대기`, `발송중`, `완료`, `실패`.
- [x] Validation: 타 국가 또는 타 조직 대상에게 발송할 수 없다.
- [x] Validation: 발송 대상은 리더의 국가 scope와 하위 파트너/가맹점 관계로 서버에서 검증한다.
- [x] Validation: 예약시간은 현재 시각 이후여야 한다.
- [x] Validation: 발송 전 제목, 유형, 대상, 내용, 채널 필수값을 검증한다.
- [x] Data Notes: 리더 공지는 하위 파트너와 가맹점 수신자 단위로 발송/읽음 상태를 저장한다.
- [ ] Data Notes: 채널은 포털 알림, 이메일, Telegram 등 정책 데이터로 관리한다.
- [ ] Data Notes: 발송 실패는 수신자별 실패 사유와 재시도 가능 여부를 저장한다.
- [x] Audit: 발송과 예약 발송 이력을 남긴다.

### 12.20 공지보내기 API 후보

- [ ] `GET /api/leader/notices/recipients`
- [ ] Query: `recipientType`, `countryScope`, `partnerId`, `merchantStatus`, `keyword`.
- [ ] Response: 리더 권한 범위 내 발송 가능 수신자 목록과 수신자 수.
- [ ] `POST /api/leader/notices/drafts`
- [ ] Body: 제목, 유형, 대상, 내용, 채널, 예약시간.
- [ ] Action: 임시저장.
- [ ] `POST /api/leader/notices/preview`
- [ ] Body: 제목, 유형, 대상, 내용, 채널.
- [ ] Action: 발송 미리보기와 수신자 검증.
- [ ] `POST /api/leader/notices/{noticeId}/send`
- [ ] Action: 즉시 발송 또는 예약 발송 확정.
- [x] `POST /api/leader/notices/send`
- [x] Action: draft 없이 즉시 발송 또는 예약 발송 내역을 저장한다.
- [ ] `GET /api/leader/notices/{noticeId}/read-receipts`
- [ ] Response: 수신자별 발송 상태, 읽음 상태, 실패 사유.
- [x] Error: 타 국가/타 조직 수신자가 포함되면 `403` 또는 표준 권한 오류를 반환한다.
- [x] Error: 예약시간이 과거이면 표준 검증 오류를 반환한다.
- [x] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.21 발송내역 작업큐

- [ ] Purpose: 공지 발송 결과와 읽음률을 확인한다.
- [ ] Permission: 국가 리더만 접근 가능하다.
- [ ] Main Features: 발송 테이블, 읽음/실패 대상, 재발송.
- [ ] Input Fields: 검색, 유형, 상태, 기간.
- [ ] Actions: 상세보기, 재발송.
- [ ] Status: `임시저장`, `예약대기`, `발송완료`, `일부실패`.
- [ ] Validation: 국가 리더는 본인이 발송했거나 본인 국가 scope/하위 조직 대상인 발송내역만 조회할 수 있다.
- [ ] Validation: 재발송은 실패 대상 또는 미확인 대상 등 허용된 대상 범위 안에서만 수행한다.
- [ ] Validation: 타 국가/타 조직 수신자를 재발송 대상으로 포함할 수 없다.
- [ ] Data Notes: `Notice`, `NoticeRecipient`, `ActivityLog`를 연결해 발송 결과와 읽음률을 계산한다.
- [ ] Data Notes: 읽음률은 `읽음 수 / 발송 성공 수` 기준으로 계산한다.
- [ ] Data Notes: 일부실패는 수신자별 실패 사유와 재발송 가능 여부를 함께 표시한다.
- [ ] Audit: 상세 조회, 실패 대상 확인, 재발송 요청 이력을 `ActivityLog`에 남긴다.

### 12.22 발송내역 API 후보

- [ ] `GET /api/leader/notices/history`
- [ ] Query: `keyword`, `noticeType`, `status`, `periodFrom`, `periodTo`, `countryScope`, `page`, `size`.
- [ ] Response: 발송 테이블, 발송 상태, 대상 수, 성공/실패 수, 읽음률.
- [ ] `GET /api/leader/notices/history/{noticeId}`
- [ ] Response: Notice 상세, NoticeRecipient 목록, 읽음/실패 대상, ActivityLog 요약.
- [ ] `POST /api/leader/notices/history/{noticeId}/resend`
- [ ] Body: 재발송 대상 조건, 채널, 메모.
- [ ] Action: 실패 대상 또는 허용된 대상에게 재발송.
- [ ] Error: 타 국가/타 조직 수신자가 포함되면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 재발송 불가 상태의 공지이면 표준 검증 오류를 반환한다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.23 파트너 어드민 대시보드 작업큐

- [ ] Purpose: 본인 유치 가맹점과 실적, 수수료를 요약한다.
- [ ] Permission: 세일즈 파트너만 접근 가능하다.
- [ ] Main Features: 하위 가맹점 수, 월 거래량, 수수료, 정산가능금액.
- [ ] Input Fields: 기간 필터.
- [ ] Actions: 상세 이동, 정산 이동.
- [ ] Status: `활성`, `정산가능`, `보류`.
- [ ] Validation: 세일즈 파트너는 own merchant scope 안의 가맹점 데이터만 조회할 수 있다.
- [ ] Validation: 정산가능금액은 확정 거래와 보류 제외 정책 기준으로 계산한다.
- [ ] Data Notes: own merchant scope 검증은 필수이며 프론트 필터만 믿지 않는다.
- [ ] Data Notes: 파트너 대시보드는 리더/본사 전체 데이터를 노출하지 않는다.
- [ ] Data Notes: 수수료와 정산가능금액은 정산 경로 정책과 지급 대상 정책 스냅샷 기준으로 계산한다.

### 12.24 파트너 어드민 대시보드 API 후보

- [ ] `GET /api/partner/dashboard`
- [ ] Query: `period`.
- [ ] Auth context: `partnerId`, own merchant scope.
- [ ] Response: 파트너 프로필, KPI, 하위 가맹점 요약, 월 거래량, 수수료 요약, 정산가능금액.
- [ ] Error: own merchant scope 밖 데이터 요청이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.25 가맹점 요청 관리 작업큐

- [ ] Purpose: 파트너가 신규 가맹점 등록 요청을 생성하고 상태를 확인한다.
- [ ] Permission: 세일즈 파트너만 접근 가능하다.
- [ ] Main Features: 가맹점 신청, 자료 업로드, 상태 추적.
- [ ] Input Fields: 매장명, 대표자, 주소, KORION WALLET, 증빙.
- [ ] Actions: 임시저장, 등록요청.
- [ ] Status: `승인요청`, `검토중`, `자료요청`, `승인`, `거절`.
- [ ] Validation: 파트너는 own merchant scope로 생성될 가맹점 요청만 등록할 수 있다.
- [ ] Validation: KORION WALLET TRX 주소 형식과 중복 여부를 검증한다.
- [ ] Validation: 등록요청 시 매장명, 대표자, 주소, KORION WALLET, 필수 증빙을 검증한다.
- [ ] Data Notes: 최종 승인은 리더 또는 본사 권한이다.
- [ ] Data Notes: 파트너는 임시저장, 등록요청, 자료요청 대응만 수행할 수 있다.
- [ ] Data Notes: 승인 전 가맹점 요청은 정식 가맹점 목록과 정산 대상에 포함하지 않는다.
- [ ] Audit: 임시저장, 등록요청, 자료 업로드, 자료요청 대응, 승인/거절 상태 변경 이력을 남긴다.

### 12.26 가맹점 요청 관리 API 후보

- [ ] `GET /api/partner/merchant-requests`
- [ ] Query: `status`, `period`, `keyword`, `page`, `size`.
- [ ] Response: 가맹점 등록 요청 목록, 상태, 자료요청 여부, 마지막 변경일.
- [ ] `POST /api/partner/merchant-requests/drafts`
- [ ] Body: 매장명, 대표자, 주소, KORION WALLET, 증빙, 메모.
- [ ] Action: 임시저장.
- [ ] `POST /api/partner/merchant-requests/{requestId}/submit`
- [ ] Action: 등록요청.
- [ ] `POST /api/partner/merchant-requests/{requestId}/materials`
- [ ] Body: 추가 증빙, 보완 메모.
- [ ] Action: 자료요청 대응.
- [ ] Error: 파트너 own merchant scope 밖 요청이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 이미 승인/거절된 요청은 파트너가 수정할 수 없다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.27 파트너 가맹점 관리 작업큐

- [ ] Purpose: 본인이 유치한 가맹점만 관리한다.
- [ ] Permission: 세일즈 파트너만 접근 가능하다.
- [ ] Main Features: 가맹점 목록, 거래확인, 정보수정 요청.
- [ ] Input Fields: 검색, 상태, 도시.
- [ ] Actions: 상세보기, 수정요청.
- [ ] Status: `운영중`, `승인대기`, `정지`.
- [ ] Validation: 파트너는 본인이 유치한 own merchant scope 가맹점만 조회할 수 있다.
- [ ] Validation: 타 파트너 가맹점은 목록, 상세, 거래확인, 수정요청 모두 조회/요청할 수 없다.
- [ ] Validation: 파트너는 가맹점 정보를 직접 수정하지 않고 수정요청만 생성할 수 있다.
- [ ] Data Notes: 타 파트너 가맹점 조회는 서버에서 차단한다.
- [ ] Data Notes: 거래확인은 해당 가맹점의 거래 요약과 최근 거래만 제공하고 리더/본사 전체 거래는 노출하지 않는다.
- [ ] Data Notes: 수정요청은 리더 또는 본사 검토 플로우로 전달한다.
- [ ] Audit: 상세 조회, 거래확인, 수정요청 이력을 남긴다.

### 12.28 파트너 가맹점 관리 API 후보

- [ ] `GET /api/partner/merchants`
- [ ] Query: `keyword`, `status`, `city`, `page`, `size`.
- [ ] Response: own merchant scope 가맹점 목록, 운영 상태, 거래 요약, 최근 거래일.
- [ ] `GET /api/partner/merchants/{merchantId}`
- [ ] Response: 가맹점 상세, 거래 요약, 정산 요약, 운영 상태.
- [ ] `GET /api/partner/merchants/{merchantId}/transactions`
- [ ] Query: `periodFrom`, `periodTo`, `paymentStatus`, `page`, `size`.
- [ ] Response: 해당 가맹점 거래 목록과 상태 요약.
- [ ] `POST /api/partner/merchants/{merchantId}/change-requests`
- [ ] Body: 수정 요청 항목, 요청 사유, 증빙.
- [ ] Action: 정보수정 요청 생성.
- [ ] Error: own merchant scope 밖 가맹점이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 정지 상태 가맹점의 수정요청 허용 범위는 정책에 따라 검증한다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.29 파트너 수수료 정산 작업큐

- [ ] Purpose: 파트너 본인 수수료와 정산 신청을 관리한다.
- [ ] Permission: 세일즈 파트너만 접근 가능하다.
- [ ] Main Features: 정산가능수수료, 보류거래, 지급내역.
- [ ] Input Fields: 기간, KORION WALLET, 신청금액.
- [ ] Actions: 정산신청, 상세보기.
- [ ] Status: `REQUESTED`, `APPROVED`, `PAID`, `REJECTED`.
- [ ] Validation: 파트너는 본인 귀속 수수료만 조회하고 정산 신청할 수 있다.
- [ ] Validation: Sync 실패, 환불, 취소, 리스크 거래는 정산가능수수료에서 제외한다.
- [ ] Validation: 신청금액은 정산가능수수료를 초과할 수 없다.
- [ ] Validation: 인증된 정산 수취 KORION WALLET TRX 주소가 없으면 정산신청할 수 없다.
- [ ] Data Notes: Sync 실패/환불/리스크 거래는 제외한다.
- [ ] Data Notes: 보류거래는 제외 사유, 원 거래, 해소 가능 여부를 함께 표시한다.
- [ ] Data Notes: 지급내역은 KORION WALLET 지급 주소와 온체인 거래 식별자 또는 외부 지급 참조값을 연결한다.
- [ ] Audit: 정산신청, 상세조회, 보류거래 확인, 지급내역 조회 이력을 남긴다.

### 12.30 파트너 수수료 정산 API 후보

- [ ] `GET /api/partner/settlements/available`
- [ ] Query: `periodFrom`, `periodTo`.
- [ ] Response: 정산가능수수료, 보류거래 금액, 제외 사유, 대상 거래 기간.
- [x] `POST /api/partner/settlements/requests`
- [x] Body: 기간, KORION WALLET, 신청금액.
- [x] Action: 정산신청.
- [ ] `GET /api/partner/settlements/requests`
- [ ] Query: `status`, `periodFrom`, `periodTo`, `page`, `size`.
- [ ] Response: 정산 신청 목록, 상태, 신청금액, 승인금액, 지급 상태.
- [ ] `GET /api/partner/settlements/requests/{requestId}`
- [ ] Response: 정산 신청 상세, 보류거래, 지급내역.
- [x] `GET /api/partner/settlements`
- [x] Response: 실제 정산 신청 목록, 상태, 신청금액, 지급 상태.
- [x] `GET /api/partner/settlements/detail`
- [x] Response: 최신 정산 신청 상세, 보류거래, 지급내역.
- [ ] Error: partnerId 기준 본인 수수료 범위 밖이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 정산가능수수료 초과 신청이면 표준 검증 오류를 반환한다.
- [x] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.31 파트너 본사소식지/웰컴 공지 작업큐

- [ ] Purpose: 본사/리더 공지를 수신하고 하위 가맹점 공지를 발송한다.
- [ ] Permission: 세일즈 파트너만 접근 가능하다.
- [ ] Main Features: 공지 수신, 공지 발송, 읽음 상태.
- [ ] Input Fields: 제목, 대상, 내용, 채널.
- [ ] Actions: 발송, 상세보기.
- [ ] Status: `읽음`, `미확인`, `발송완료`.
- [ ] Validation: 파트너는 본인에게 발송된 본사/리더 공지만 조회할 수 있다.
- [ ] Validation: 파트너는 본인 하위 가맹점에게만 공지를 발송할 수 있다.
- [ ] Validation: 타 파트너 가맹점, 리더, 본사, 타 국가/타 조직 대상에게 발송할 수 없다.
- [ ] Data Notes: 하위 가맹점에게만 발송 가능하다.
- [ ] Data Notes: 수신 공지와 발송 공지는 Notice 방향(`INBOUND`, `OUTBOUND`)으로 구분한다.
- [ ] Data Notes: 웰컴 공지는 신규 승인 가맹점 대상 기본 템플릿으로 관리할 수 있다.
- [ ] Audit: 공지 상세 조회, 읽음 처리, 발송, 발송 실패 이력을 남긴다.

### 12.32 파트너 본사소식지/웰컴 공지 API 후보

- [ ] `GET /api/partner/notices/inbox`
- [ ] Query: `keyword`, `noticeType`, `readStatus`, `page`, `size`.
- [ ] Response: 본사/리더 수신 공지 목록, 공지유형, 제목, 발송자, 읽음 상태.
- [ ] `GET /api/partner/notices/inbox/{noticeId}`
- [ ] Response: 수신 공지 상세, 첨부/정책 링크, 읽음 상태.
- [ ] `POST /api/partner/notices/inbox/{noticeId}/read`
- [ ] Action: 읽음 처리.
- [x] `POST /api/partner/notices/outbox`
- [x] Body: 제목, 유형, 대상, 내용, 채널, 예약시간.
- [x] Action: 하위 가맹점 공지 즉시/예약 발송 내역 저장.
- [ ] `GET /api/partner/notices/outbox/{noticeId}`
- [ ] Response: 발송 공지 상세, 하위 가맹점 수신자, 읽음/미확인/실패 상태.
- [ ] Error: 파트너 수신 대상이 아닌 공지이면 `403` 또는 표준 권한 오류를 반환한다.
- [x] Error: own merchant scope 밖 수신자가 포함되면 `403` 또는 표준 권한 오류를 반환한다.
- [x] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.33 파트너 자료실/영업도구 작업큐

- [ ] Purpose: 영업 및 온보딩 자료를 확인한다.
- [ ] Permission: 세일즈 파트너만 접근 가능하다.
- [ ] Main Features: KORION PAY 소개, QR/NFC/BLE 사용법, 가맹점 안내자료.
- [ ] Input Fields: 검색, 카테고리.
- [ ] Actions: 다운로드, 공유.
- [ ] Status: `활성 자료`, `만료 자료`.
- [ ] Validation: 파트너는 본인 권한과 국가/언어 scope에 배포된 자료만 조회할 수 있다.
- [ ] Validation: 만료 자료는 기본 목록에서 숨기거나 만료 표시 후 다운로드 제한 정책을 적용한다.
- [ ] Validation: 공유 링크는 권한 범위와 만료 시간을 가져야 한다.
- [ ] Data Notes: 자료 버전 관리가 필요하다.
- [ ] Data Notes: 자료는 카테고리, 언어, 국가 scope, 버전, 만료일, 파일 해시를 가진다.
- [ ] Data Notes: QR/NFC/BLE 사용법은 제품 버전 또는 앱 버전과 연결할 수 있어야 한다.
- [ ] Audit: 다운로드, 공유 링크 생성, 만료 자료 접근 시도를 기록한다.

### 12.34 파트너 자료실/영업도구 API 후보

- [ ] `GET /api/partner/resources`
- [ ] Query: `keyword`, `category`, `status`, `language`, `page`, `size`.
- [ ] Response: 자료 목록, 카테고리, 버전, 상태, 만료일, 다운로드 가능 여부.
- [ ] `GET /api/partner/resources/{resourceId}`
- [ ] Response: 자료 상세, 버전 이력, 파일 메타데이터, 공유 가능 여부.
- [ ] `GET /api/partner/resources/{resourceId}/download`
- [ ] Action: 권한 범위 내 자료 다운로드.
- [ ] `POST /api/partner/resources/{resourceId}/share-links`
- [ ] Body: 만료 시간, 공유 대상 메모.
- [ ] Action: 공유 링크 생성.
- [ ] Error: 권한 scope 밖 자료이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 만료 자료 다운로드가 제한된 경우 표준 검증 오류를 반환한다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.35 파트너 활동 로그 작업큐

- [ ] Purpose: 파트너 계정의 실행 기록을 확인한다.
- [ ] Permission: 세일즈 파트너만 접근 가능하다.
- [ ] Main Features: 로그인, 가맹점 요청, 정산 신청, 공지 발송 기록.
- [ ] Input Fields: 기간, 유형, 상태.
- [ ] Actions: 상세보기.
- [ ] Status: `성공`, `실패`, `검토중`.
- [ ] Validation: 파트너는 본인 계정 또는 본인 조직 범위의 활동 로그만 조회할 수 있다.
- [ ] Validation: 민감한 payload, 토큰, KORION WALLET 서명값은 로그 상세에 노출하지 않는다.
- [ ] Data Notes: `ActivityLog`는 필수이다.
- [ ] Data Notes: 로그인, 가맹점 요청, 정산 신청, 공지 발송, 자료 다운로드/공유, 지갑 인증 이벤트를 ActivityLog에 기록한다.
- [ ] Data Notes: 실패 로그는 실패 사유, 요청 ID, 대상 엔티티를 함께 저장한다.
- [ ] Audit: 활동 로그 상세 조회도 감사 이벤트로 남긴다.

### 12.36 파트너 활동 로그 API 후보

- [ ] `GET /api/partner/activity-logs`
- [ ] Query: `periodFrom`, `periodTo`, `type`, `status`, `page`, `size`.
- [ ] Response: 활동 로그 목록, 유형, 상태, 발생 시각, 대상 엔티티, 요청 ID.
- [ ] `GET /api/partner/activity-logs/{activityLogId}`
- [ ] Response: 활동 로그 상세, 실패 사유, 대상 엔티티, 안전하게 마스킹된 메타데이터.
- [ ] Error: partnerId 범위 밖 활동 로그이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 민감 payload 요청은 응답하지 않는다.
- [ ] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.37 가맹점 대시보드 API 작업큐

- [x] Purpose: 매장 운영자가 본인 매장의 운영/결제 상태를 확인한다.
- [x] Permission: 가맹점만 접근 가능하다.
- [x] Main Features: KPI, 거래량, QR/NFC/BLE 상태, 최근 결제.
- [ ] Input Fields: 기간, 결제수단.
- [ ] Actions: 거래 상세보기.
- [ ] Status: `결제완료`, `Sync대기`, `Sync실패`.
- [x] Validation: 가맹점은 본인 매장 데이터만 조회할 수 있다.
- [x] Validation: 매장 운영자 계정과 merchant/store scope를 서버에서 검증한다.
- [x] Data Notes: QR/NFC/BLE 결제수단별 상태와 최근 결제 목록은 동일한 권한 scope를 적용한다.
- [ ] Data Notes: Sync대기/Sync실패 거래는 정산 대상 포함 여부와 보류 사유를 함께 표시할 수 있어야 한다.
- [ ] Audit: 거래 상세 조회와 Sync실패 상세 조회는 ActivityLog 또는 MerchantActivityLog에 기록한다.

### 12.38 가맹점 대시보드 API 후보

- [x] `GET /api/merchant/dashboard`
- [ ] Query: `periodFrom`, `periodTo`, `paymentMethod`.
- [x] Response: KPI, 거래량, QR/NFC/BLE 상태, 최근 결제, Sync 상태 요약.
- [x] `GET /api/merchant/payments`
- [ ] Query: `periodFrom`, `periodTo`, `paymentMethod`, `status`, `page`, `size`.
- [x] Response: 본인 매장 결제 목록, 결제수단, 결제상태, Sync 상태, 거래금액, 발생 시각.
- [ ] `GET /api/merchant/payments/{paymentId}`
- [ ] Response: 거래 상세, 결제수단, Sync 이력, 정산 반영 상태, 실패 사유.
- [ ] Error: 본인 매장 범위 밖 paymentId이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: Sync실패 상세 조회 시 민감한 내부 payload는 마스킹한다.
- [x] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.39 가맹점 관리/매장정보 작업큐

- [x] Purpose: 매장 정보, 결제 설정, 상품 상태를 확인하고 수정 요청한다.
- [x] Permission: 가맹점만 접근 가능하다.
- [x] Main Features: 매장기본정보, 주소, 업종, KORION WALLET, 상품상태.
- [ ] Input Fields: 매장명, 주소, 연락처, 업종, 상품정보.
- [ ] Actions: 저장, 수정요청, Wallet 확인.
- [ ] Status: `검토중`, `활성`, `비활성`.
- [x] Validation: 가맹점은 본인 매장 정보만 조회하고 수정 요청할 수 있다.
- [ ] Validation: 중요 정보 변경은 본사 또는 상위 조직 검토가 필요하다.
- [ ] Validation: KORION WALLET 확인은 TRX 네트워크 주소와 nonce 서명 기준으로 검증한다.
- [ ] Data Notes: 중요 정보 변경은 즉시 반영하지 않고 변경 요청 레코드로 관리한다.
- [ ] Data Notes: 상품상태는 매장 운영 가능 여부와 결제 노출 가능 여부를 분리해 표현할 수 있어야 한다.
- [ ] Audit: 저장, 수정요청, Wallet 확인, 검토 결과 반영은 ActivityLog 또는 MerchantActivityLog에 기록한다.

### 12.40 가맹점 관리/매장정보 API 후보

- [x] `GET /api/merchant/store`
- [x] Response: 매장기본정보, 주소, 연락처, 업종, KORION WALLET 상태, 상품상태, 검토 상태.
- [ ] `PATCH /api/merchant/store/draft`
- [ ] Body: 매장명, 주소, 연락처, 업종, 상품정보.
- [ ] Action: 임시 저장 또는 중요 정보가 아닌 허용 필드 저장.
- [ ] `POST /api/merchant/store/change-requests`
- [ ] Body: 매장명, 주소, 연락처, 업종, 상품정보, 변경 사유.
- [ ] Action: 본사 또는 상위 조직 검토 대상 수정요청 생성.
- [ ] `POST /api/merchant/store/wallet/verify`
- [ ] Body: KORION WALLET TRX 주소, nonce, signature.
- [ ] Action: Wallet 확인.
- [ ] Error: 본인 매장 범위 밖 요청이면 `403` 또는 표준 권한 오류를 반환한다.
- [ ] Error: 중요 정보 변경을 직접 저장하려는 경우 수정요청을 요구한다.
- [x] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.41 가맹점 본사 소식지/계정 로그 작업큐

- [x] Purpose: 본사/상위 조직 공지와 계정 활동 로그를 확인한다.
- [x] Permission: 가맹점만 접근 가능하다.
- [x] Main Features: 공지수신, 읽음상태, 프로필, 활동로그.
- [ ] Input Fields: 검색, 기간, 유형.
- [ ] Actions: 상세보기, 프로필 저장.
- [ ] Status: `읽음`, `미확인`, `성공`, `실패`.
- [x] Validation: 가맹점은 본인 매장 또는 본인 계정으로 수신된 공지만 조회할 수 있다.
- [x] Validation: 계정 활동 로그는 본인 계정과 본인 매장 scope 안에서만 조회할 수 있다.
- [ ] Validation: 프로필 저장 시 변경 가능한 필드와 검토 대상 필드를 분리한다.
- [ ] Data Notes: 공지 수신과 계정 변경은 ActivityLog에 기록한다.
- [ ] Data Notes: 본사/리더/파트너 공지는 발신 조직과 수신 범위를 함께 저장한다.
- [ ] Audit: 공지 상세보기, 읽음 처리, 프로필 저장, 활동로그 상세 조회를 기록한다.

### 12.42 가맹점 본사 소식지/계정 로그 API 후보

- [x] `GET /api/merchant/notices`
- [ ] Query: `keyword`, `periodFrom`, `periodTo`, `type`, `readStatus`, `page`, `size`.
- [x] Response: 본사/상위 조직 공지 목록, 공지 유형, 발신 조직, 읽음 상태, 발송 시각.
- [ ] `GET /api/merchant/notices/{noticeId}`
- [ ] Response: 공지 상세, 발신 조직, 읽음 상태, 첨부/정책 링크.
- [ ] `POST /api/merchant/notices/{noticeId}/read`
- [ ] Action: 읽음 처리.
- [x] `GET /api/merchant/profile`
- [x] Response: 계정 프로필, 연락처, 알림 설정, 변경 가능 필드.
- [ ] `PATCH /api/merchant/profile`
- [ ] Body: 프로필 변경 가능 필드.
- [ ] Action: 프로필 저장.
- [x] `GET /api/merchant/activity-logs`
- [ ] Query: `periodFrom`, `periodTo`, `type`, `status`, `page`, `size`.
- [x] Response: 활동 로그 목록, 유형, 상태, 발생 시각, 대상 엔티티.
- [ ] `GET /api/merchant/activity-logs/{activityLogId}`
- [ ] Response: 활동 로그 상세, 실패 사유, 대상 엔티티, 마스킹된 메타데이터.
- [ ] Error: 본인 매장 또는 계정 범위 밖 공지/로그이면 `403` 또는 표준 권한 오류를 반환한다.
- [x] OpenAPI 문서와 contract test를 같이 작성한다.

### 12.43 프론트 화면 체크리스트

- [ ] `/auth` 또는 로그인 진입 화면에서 리더/파트너/가맹점 로그인 카드를 제공한다.
- [ ] 파트너/가맹점 회원가입 선택 진입을 제공하고, 리더 회원가입은 일반 공개 진입으로 노출하지 않는다.
- [ ] 로그인 카드 선택 후 각 권한별 로그인 화면 또는 공통 로그인 화면에 role selection 값을 전달한다.
- [ ] 인증 성공 후 실제 사용자 권한에 맞는 대시보드로 라우팅한다.
- [ ] 선택한 로그인 카드와 인증된 역할이 다르면 보안 안내와 함께 권한별 올바른 진입으로 유도한다.
- [ ] 로그인/회원가입 진입 화면에는 KORION WALLET 서명 요청이나 민감 정보 입력을 요구하지 않는다.
- [ ] `/leader/login` 또는 리더 role selection 로그인 화면에서 아이디, 비밀번호, 2FA 코드 입력을 제공한다.
- [ ] 리더 로그인 화면에는 로그인 유지와 비밀번호 찾기 액션을 제공한다.
- [ ] 리더 계정이 2FA 필요 상태이면 비밀번호 검증 후 2FA 코드 입력 단계로 전환한다.
- [ ] 리더 로그인 성공 시 `/leader/dashboard`로 이동하고 선택 가능한 국가 scope를 API 응답 기준으로 초기화한다.
- [ ] 리더 계정 정지/비활성/2FA 실패 상태는 보안 안내를 표시하되 계정 존재 여부를 과도하게 노출하지 않는다.
- [ ] `/partner/login` 또는 파트너 role selection 로그인 화면에서 아이디, 비밀번호, 2FA 코드 입력을 제공한다.
- [ ] 파트너 로그인 화면에는 세션 유지와 비밀번호 찾기 액션을 제공한다.
- [ ] 파트너 계정이 2FA 필요 상태이면 비밀번호 검증 후 2FA 코드 입력 단계로 전환한다.
- [ ] 파트너 로그인 성공 시 `/partner/dashboard`로 이동하고 own merchants scope를 API 응답 기준으로 초기화한다.
- [ ] 파트너 계정 비활성/정지/2FA 실패 상태는 보안 안내를 표시하되 계정 존재 여부를 과도하게 노출하지 않는다.
- [ ] `/merchant/login` 또는 가맹점 role selection 로그인 화면에서 아이디, 비밀번호, 2FA 코드 입력을 제공한다.
- [ ] 가맹점 로그인 화면에는 비밀번호 찾기 액션을 제공한다.
- [ ] 가맹점 계정이 2FA 필요 상태이면 비밀번호 검증 후 2FA 코드 입력 단계로 전환한다.
- [ ] 가맹점 로그인 성공 시 `/merchant/dashboard`로 이동하고 own store scope를 API 응답 기준으로 초기화한다.
- [ ] 매장 비활성/결제 제한/2FA 실패 상태는 보안 안내를 표시하되 계정 존재 여부를 과도하게 노출하지 않는다.
- [ ] `/partner/signup` 또는 파트너 가입신청 화면에서 계정정보, 소속정보, 영업역량, KORION WALLET, 동의/제출 단계를 제공한다.
- [ ] 파트너 가입신청 입력 필드는 아이디, 이메일, 인증번호, 리더코드, KORION WALLET, 활동정보를 제공한다.
- [ ] 파트너 가입신청 화면에는 아이디/이메일 중복확인, 이메일 인증발송, 가입신청 액션을 제공한다.
- [ ] 리더코드가 없으면 본사 직접 계약 신청으로 표시하고, 리더코드가 있으면 상위 리더 정보를 확인 표시한다.
- [ ] 이메일 인증 완료 전에는 가입신청 제출 버튼을 비활성화한다.
- [ ] 가입신청 제출 후 상태는 승인요청/대기/비활성 중 현재 검토 상태를 표시한다.
- [ ] `/merchant/signup` 또는 가맹점 가입신청 화면에서 매장정보, 소속코드, 결제설정, KORION WALLET, 검증자료 단계를 제공한다.
- [ ] 가맹점 가입신청 입력 필드는 아이디, 이메일, 매장명, 주소, 업종, KORION WALLET, 증빙자료를 제공한다.
- [ ] 가맹점 가입신청 화면에는 리더 코드/파트너 코드 확인, 아이디/이메일/매장 중복확인, 가입신청 액션을 제공한다.
- [ ] 리더 코드, 파트너 코드, 본사 직접 계약 중 선택한 계약 경로를 명확히 표시한다.
- [ ] 승인 전 상태는 승인요청/매장대기/결제비활성으로 표시하고 결제 기능을 활성화하지 않는다.
- [ ] 중복 가맹점 의심 결과가 있으면 검토 필요 안내를 표시하고 즉시 승인으로 이어지지 않게 한다.
- [ ] 가입, 인증, 중복확인, 제출 액션에는 재사용 가능한 확인 모달, 오류 모달, 성공 토스트를 적용한다.
- [ ] 성공/실패/중복/인증완료 상태별 메시지와 버튼은 공통 컴포넌트 정책을 따른다.
- [ ] 확인 모달은 확인/취소 액션을 제공하고, 오류 모달은 재시도 가능 여부를 명확히 표시한다.
- [ ] 성공 토스트는 다음 화면 이동 또는 현재 상태 갱신과 충돌하지 않도록 자동 닫힘 시간을 정책화한다.
- [ ] 보안상 민감한 실패 사유는 화면에 원문 노출하지 않고 일반화된 안내로 표시한다.
- [ ] 로그인/회원가입 메인 화면은 9개 화면 진입을 role 기반으로 제공하되, 리더 회원가입 공개 진입은 노출하지 않는다.
- [ ] 회원가입 계열 화면은 휴대폰/WhatsApp, Telegram 입력과 중복 확인 결과를 표시한다.
- [ ] KORION Wallet 연동 화면은 Wallet 미연동/연동 완료 상태를 명확히 표시하고 개인키/시드문구/PIN 입력을 요구하지 않는다.
- [ ] 이메일 인증/중복 확인 화면은 인증번호 발송, 인증 확인, 아이디/이메일/Telegram/WhatsApp/Wallet 중복 확인 결과를 분리 표시한다.
- [ ] 가입 신청 완료 후 승인요청/검토중/자료요청/보류/승인/거절/대기/비활성 상태를 조회 가능한 경로로 안내한다.
- [ ] `/leader/dashboard` 화면에 기간 필터와 국가 scope 필터를 둔다.
- [ ] 선택 가능한 국가 scope는 API의 `LeaderProfile.countryScopes`만 노출한다.
- [ ] KPI는 국가 scope와 기간 기준의 summary 값을 표시한다.
- [ ] 하부 조직 패널은 파트너/가맹점 요약을 표시한다.
- [ ] 월 거래량 패널은 기간별 거래액과 거래 건수를 표시한다.
- [ ] 수수료 패널은 리더/파트너/가맹점 귀속 또는 지급 예정 summary를 표시한다.
- [ ] 리스크 알림 패널은 severity, 대상, 메시지를 표시한다.
- [ ] 상세 이동/리포트 이동 버튼은 확정된 기존 라우트로 연결한다.
- [ ] 접근성: 필터 label, 버튼 포커스, alert role, 테이블/목록 읽기 순서를 확인한다.
- [ ] 모바일/좁은 화면에서 카드와 패널 텍스트가 겹치지 않는지 확인한다.
- [ ] `/leader/requests/partner` 화면에서 파트너 후보 등록 폼과 상태 목록을 제공한다.
- [ ] 파트너 후보 등록 폼에는 파트너명, 연락처, 이메일, 지역, KORION WALLET, 실행계획 입력을 둔다.
- [ ] 임시저장과 본사 승인 요청 버튼은 상태별로 활성/비활성을 분리한다.
- [ ] 자료요청 상태에서는 요청 사유와 보완 제출 UI를 제공한다.
- [ ] 리더에게 최종 승인/거절 버튼을 노출하지 않는다.
- [ ] `/leader/partners` 화면에서 승인된 하부 파트너 목록과 성과 요약을 제공한다.
- [ ] 파트너 목록 필터는 검색, 상태, 지역을 제공한다.
- [ ] 파트너 상세 화면은 기본정보, 성과, 하위 가맹점 요약, 활동 메모, 상태 이력을 표시한다.
- [ ] 활동 메모 입력/저장 UI를 제공한다.
- [ ] 리더에게 직접 정지 버튼을 노출하지 않고 `본사 정지 요청` 액션만 제공한다.
- [ ] 상위 조직 변경 UI는 리더 화면에 노출하지 않는다.
- [ ] `/leader/merchants` 화면에서 자기 국가/조직 내 가맹점 목록을 제공한다.
- [ ] 가맹점 목록 필터는 검색, 도시, 상태, 업종을 제공한다.
- [ ] 가맹점 목록에서 승인대기, 활성, 정지, 문제 상태를 구분 표시한다.
- [ ] 문제 가맹점은 RiskFlag severity와 사유를 함께 표시한다.
- [ ] 가맹점 상세 화면은 기본정보, 계약 경로, 매장 상태, 거래/정산 요약, RiskFlag 이력을 표시한다.
- [ ] 리더에게 직접 승인/정지 최종 처리 버튼을 노출하지 않고 `재검토 요청` 액션을 제공한다.
- [ ] `/leader/transactions` 화면에서 전체 거래 로그를 제공한다.
- [ ] `/leader/transactions/offline` 화면에서 오프라인 거래와 Sync 상태를 제공한다.
- [ ] `/leader/transactions/failed` 화면에서 실패/취소/환불 거래 로그를 제공한다.
- [ ] 거래 로그 필터는 기간, 가맹점, 파트너, 결제상태를 제공한다.
- [ ] 거래 상세 화면은 오프라인 Sync 이력과 정산 반영 상태를 표시한다.
- [ ] Excel 다운로드 버튼은 현재 필터와 권한 범위를 그대로 적용한다.
- [ ] Sync 대기/Sync 실패 거래는 정산 보류 후보 배지로 표시한다.
- [ ] `/leader/settlement/request` 화면에서 정산 가능금액, 보류 제외 금액, 대상 거래 기간을 표시한다.
- [ ] 정산 신청 입력 필드는 정산기간, KORION WALLET, 신청금액, 메모를 제공한다.
- [ ] 정산기간 선택 시 당일 거래 제외와 마지막 정산일 다음날 기준을 안내한다.
- [ ] 임시저장과 본사 정산 요청 버튼을 제공한다.
- [ ] 본사 정산 요청 전 정산 가능금액을 재계산하고 차이가 있으면 사용자에게 표시한다.
- [ ] 파트너 자동정산 대상과 보류 제외 사유를 별도 영역에 표시한다.
- [ ] `/leader/settlement/history` 화면에서 정산 요청, 승인, 지급, 보류/조정 내역 테이블을 제공한다.
- [ ] 정산내역 필터는 검색, 기간, 상태를 제공한다.
- [ ] 상태 필터는 지급완료, 보류, 조정필요를 제공한다.
- [ ] 상세 패널은 SettlementRequest, Commission, 지급 내역, 보류/조정 사유를 함께 표시한다.
- [ ] 다운로드 버튼은 현재 필터와 권한 범위를 그대로 적용한다.
- [ ] `/leader/hq-notices` 화면에서 본사 공지 목록과 읽음 상태를 제공한다.
- [ ] 본사 소식지 필터는 검색, 공지유형을 제공한다.
- [ ] 본사 공지 상세보기 화면 또는 패널을 제공한다.
- [ ] 미확인 공지는 목록에서 명확히 구분 표시한다.
- [ ] 상세보기 진입 또는 읽음 처리 버튼으로 읽음 상태를 갱신한다.
- [ ] 리더 화면에는 본사 공지 작성/수정/삭제 액션을 노출하지 않는다.
- [ ] `/leader/notices/send` 화면에서 하위 파트너/가맹점 대상 공지 작성 폼을 제공한다.
- [ ] 대상 선택 UI는 파트너, 가맹점, 지역, 상태 기준으로 필터링할 수 있어야 한다.
- [ ] 채널 선택 UI는 정책 데이터로 받은 채널 목록을 표시한다.
- [ ] 즉시 발송과 예약 발송을 분리하고, 예약시간 입력을 제공한다.
- [ ] 미리보기 화면은 제목, 유형, 대상 수, 내용, 채널, 예약시간을 표시한다.
- [ ] 발송 후 읽음확인 화면 또는 패널에서 수신자별 읽음/미확인/실패 상태를 표시한다.
- [ ] 타 국가/타 조직 수신자는 UI 선택 목록에 노출하지 않는다.
- [ ] `/leader/notices/history` 화면에서 발송 테이블과 읽음률을 제공한다.
- [ ] 발송내역 필터는 검색, 유형, 상태, 기간을 제공한다.
- [ ] 발송 상세 화면은 Notice 본문, NoticeRecipient별 읽음/실패 상태, ActivityLog를 표시한다.
- [ ] 일부실패 상태는 실패 대상 수와 실패 사유를 요약 표시한다.
- [ ] 재발송 버튼은 허용된 실패/미확인 대상이 있는 경우에만 활성화한다.
- [ ] 재발송 전 대상 수, 채널, 메모를 확인하는 미리보기 또는 확인 단계를 제공한다.
- [ ] `/partner/dashboard` 화면에 기간 필터를 제공한다.
- [ ] 파트너 대시보드 KPI는 하위 가맹점 수, 월 거래량, 수수료, 정산가능금액을 표시한다.
- [ ] 하위 가맹점 요약은 own merchant scope 안의 데이터만 표시한다.
- [ ] 상세 이동은 하위 가맹점 목록 또는 거래/매출 상세로 연결한다.
- [ ] 정산 이동은 파트너 정산 신청 또는 정산내역 화면으로 연결한다.
- [ ] 보류 상태의 정산가능금액은 사유와 함께 표시한다.
- [x] `/partner/requests/merchant` 화면에서 가맹점 등록 요청 목록과 상태를 제공한다.
- [ ] 가맹점 등록 요청 폼에는 매장명, 대표자, 주소, KORION WALLET, 증빙 업로드를 제공한다.
- [ ] 임시저장과 등록요청 버튼은 상태별로 활성/비활성을 분리한다.
- [ ] 자료요청 상태에서는 요청 사유와 추가 증빙 업로드 UI를 제공한다.
- [ ] 파트너 화면에는 최종 승인/거절 버튼을 노출하지 않는다.
- [ ] `/partner/merchants` 화면에서 본인이 유치한 가맹점 목록만 제공한다.
- [ ] 파트너 가맹점 목록 필터는 검색, 상태, 도시를 제공한다.
- [ ] 상태는 운영중, 승인대기, 정지를 구분 표시한다.
- [ ] 가맹점 상세 화면은 기본정보, 거래 요약, 정산 요약, 운영 상태를 표시한다.
- [ ] 거래확인 영역은 해당 가맹점의 거래만 표시한다.
- [ ] 정보수정 요청 버튼과 요청 폼을 제공한다.
- [ ] 타 파트너 가맹점 검색/상세 진입 경로를 UI에 노출하지 않는다.
- [ ] `/partner/settlement/request` 화면에서 파트너 정산가능수수료와 보류거래를 표시한다.
- [ ] 파트너 수수료 정산 입력 필드는 기간, KORION WALLET, 신청금액을 제공한다.
- [ ] 정산신청 버튼은 인증된 KORION WALLET과 정산가능수수료가 있을 때만 활성화한다.
- [ ] `/partner/settlement/history` 화면에서 REQUESTED/APPROVED/PAID/REJECTED 상태별 지급내역을 표시한다.
- [ ] 보류거래 상세보기는 Sync 실패, 환불, 취소, 리스크 제외 사유를 표시한다.
- [ ] `/partner/hq-notices` 또는 `/partner/notices/inbox` 화면에서 본사/리더 수신 공지 목록을 제공한다.
- [ ] 수신 공지는 읽음/미확인 상태를 구분 표시한다.
- [ ] `/partner/notices/send` 화면에서 하위 가맹점 대상 공지 발송 폼을 제공한다.
- [ ] 공지 발송 입력 필드는 제목, 대상, 내용, 채널을 제공한다.
- [ ] 대상 선택 UI는 own merchant scope 안의 하위 가맹점만 노출한다.
- [ ] 웰컴 공지 템플릿 선택 또는 기본 내용 삽입 UI를 제공한다.
- [ ] 파트너 화면에는 본사/리더/타 파트너 대상 발송 액션을 노출하지 않는다.
- [ ] `/partner/resources` 화면에서 영업 및 온보딩 자료 목록을 제공한다.
- [ ] 자료실 필터는 검색, 카테고리, 상태를 제공한다.
- [ ] KORION PAY 소개, QR/NFC/BLE 사용법, 가맹점 안내자료 카테고리를 구분 표시한다.
- [ ] 자료 상세 화면은 버전, 만료일, 파일 정보, 다운로드/공유 액션을 제공한다.
- [ ] 만료 자료는 상태를 명확히 표시하고 다운로드 제한 정책을 반영한다.
- [ ] 공유 액션은 만료 시간이 있는 공유 링크 생성 흐름을 제공한다.
- [ ] `/partner/settings/activity-log` 또는 `/partner/activity-logs` 화면에서 파트너 활동 로그를 제공한다.
- [ ] 활동 로그 필터는 기간, 유형, 상태를 제공한다.
- [ ] 활동 로그 목록은 로그인, 가맹점 요청, 정산 신청, 공지 발송 기록을 구분 표시한다.
- [ ] 활동 로그 상세는 실패 사유와 대상 엔티티를 표시하되 민감 payload는 마스킹한다.
- [ ] `/merchant/dashboard` 화면에서 매장 운영 KPI, 거래량, QR/NFC/BLE 상태, 최근 결제를 제공한다.
- [ ] 가맹점 대시보드 필터는 기간과 결제수단을 제공한다.
- [ ] 결제 상태는 결제완료, Sync대기, Sync실패를 구분 표시한다.
- [ ] 최근 결제 목록의 거래 상세보기는 본인 매장 거래만 열 수 있어야 한다.
- [ ] QR/NFC/BLE 상태 영역은 사용 가능, 점검 필요, Sync 지연 등 운영 상태를 명확히 표시한다.
- [ ] Sync실패 거래 상세는 실패 사유와 정산 보류 여부를 표시하되 민감 payload는 마스킹한다.
- [ ] `/merchant/store` 또는 `/merchant/settings/store` 화면에서 매장기본정보, 주소, 업종, KORION WALLET, 상품상태를 제공한다.
- [ ] 가맹점 매장정보 입력 필드는 매장명, 주소, 연락처, 업종, 상품정보를 제공한다.
- [ ] 중요 정보 변경은 즉시 저장 버튼이 아니라 수정요청 흐름으로 연결한다.
- [ ] Wallet 확인 버튼은 KORION WALLET TRX 주소 인증 상태와 실패 사유를 표시한다.
- [ ] 검토중/활성/비활성 상태를 매장 정보와 상품상태 영역에서 구분 표시한다.
- [ ] 상품상태가 비활성인 경우 결제 노출 또는 판매 가능 여부를 명확히 표시한다.
- [ ] `/merchant/notices` 화면에서 본사/상위 조직 공지 목록과 읽음 상태를 제공한다.
- [ ] 가맹점 공지 필터는 검색, 기간, 유형, 읽음 상태를 제공한다.
- [ ] 공지 상세보기 진입 시 읽음 처리 정책에 따라 읽음 상태를 갱신한다.
- [ ] `/merchant/profile` 화면에서 가맹점 계정 프로필과 변경 가능 필드를 제공한다.
- [ ] 프로필 저장 버튼은 변경 가능 필드만 저장하고 중요 정보는 매장정보 수정요청 흐름으로 연결한다.
- [ ] `/merchant/activity-logs` 화면에서 계정 활동 로그를 제공한다.
- [ ] 활동 로그 필터는 기간, 유형, 상태를 제공한다.
- [ ] 활동 로그 목록은 공지 수신/읽음, 프로필 저장, 로그인, Wallet 확인, 매장정보 수정요청 기록을 구분 표시한다.
- [ ] 실패 상태 활동 로그 상세는 실패 사유를 표시하되 민감 payload는 마스킹한다.

### 12.44 백엔드 체크리스트

- [x] Auth routing API 또는 로그인 성공 응답은 사용자 role, partnerId, merchantId, country scope를 명확히 반환한다.
- [x] 로그인 성공 후 role selection과 실제 계정 권한이 불일치하면 권한 오류 또는 재라우팅 응답을 반환한다.
- [x] 파트너/가맹점 회원가입 진입은 신청 레코드 생성 플로우로 연결하고 즉시 권한을 부여하지 않는다.
- [x] 리더 계정 생성은 본사 승인/생성 플로우로 제한하고 공개 회원가입 API에서 생성하지 않는다.
- [x] 로그인 성공/실패, role selection, 역할 불일치, 회원가입 진입 선택은 ActivityLog 또는 AuthActivityLog에 기록한다.
- [x] 로그인/회원가입 진입에서 민감 payload, 토큰, KORION WALLET 서명 원문은 로그에 저장하지 않는다.
- [x] 리더 로그인 API는 아이디/비밀번호 검증 후 계정 상태와 국가 리더 승인 상태를 확인한다.
- [ ] 리더 로그인 API는 2FA 활성 계정에 대해 2FA 검증 완료 전 최종 세션을 발급하지 않는다.
- [ ] 로그인 유지 토큰은 만료 시간, 기기 식별자, 폐기 가능 상태를 저장한다.
- [ ] 비밀번호 찾기 요청은 계정 존재 여부를 노출하지 않는 응답을 반환하고 요청 이력을 기록한다.
- [x] 리더 로그인 성공 응답에는 leaderId, role, countryScopes, redirectPath=`/leader/dashboard`를 포함한다.
- [ ] 리더 로그인 성공/실패, 2FA 성공/실패, 비밀번호 찾기 요청은 ActivityLog 또는 AuthActivityLog에 기록한다.
- [x] 파트너 로그인 API는 아이디/비밀번호 검증 후 계정 상태와 세일즈 파트너 승인 상태를 확인한다.
- [ ] 파트너 로그인 API는 2FA 활성 계정에 대해 2FA 검증 완료 전 최종 세션을 발급하지 않는다.
- [ ] 파트너 비활성/정지 상태에서는 하위 가맹점 목록, 정산, 공지 발송 권한을 발급하지 않는다.
- [x] 파트너 로그인 성공 응답에는 partnerId, role, ownMerchantScope, redirectPath=`/partner/dashboard`를 포함한다.
- [ ] 파트너 로그인 성공/실패, 2FA 성공/실패, 비밀번호 찾기 요청은 ActivityLog 또는 AuthActivityLog에 기록한다.
- [ ] 파트너 세션으로 호출되는 API는 own merchants scope를 서버에서 재검증한다.
- [x] 가맹점 로그인 API는 아이디/비밀번호 검증 후 계정 상태와 가맹점 승인 상태를 확인한다.
- [ ] 가맹점 로그인 API는 2FA 활성 계정에 대해 2FA 검증 완료 전 최종 세션을 발급하지 않는다.
- [ ] 매장 비활성 또는 결제 제한 상태에서는 결제/정산/매장 운영 권한을 정책에 따라 제한한다.
- [x] 가맹점 로그인 성공 응답에는 merchantId, role, ownStoreScope, redirectPath=`/merchant/dashboard`를 포함한다.
- [ ] 가맹점 로그인 성공/실패, 2FA 성공/실패, 비밀번호 찾기 요청은 ActivityLog 또는 AuthActivityLog에 기록한다.
- [ ] 가맹점 세션으로 호출되는 API는 own store scope를 서버에서 재검증한다.
- [x] 파트너 회원가입 API는 아이디/이메일 중복확인과 이메일 인증번호 발송/검증을 제공한다.
- [x] 파트너 가입신청 제출 API는 이메일 인증 완료 여부를 서버에서 검증한다.
- [x] 리더코드가 있는 가입신청은 유효한 리더 또는 상위 조직 코드와 연결하고, 없으면 본사 직접 계약 신청으로 저장한다.
- [x] 파트너 가입신청은 `승인요청` 또는 `대기` 상태로 저장하고 로그인 권한/파트너 권한을 즉시 활성화하지 않는다.
- [ ] 최종 승인과 권한 활성화는 본사 또는 상위 검토 API에서만 처리한다.
- [x] 파트너 가입신청의 KORION WALLET은 TRX 주소 형식, 중복, nonce 서명 인증 정책을 적용한다.
- [x] 중복확인, 인증발송, 이메일 인증 성공/실패, 가입신청 제출은 ActivityLog 또는 AuthActivityLog에 기록한다.
- [x] 가맹점 회원가입 API는 리더 코드/파트너 코드 확인과 본사 직접 계약 선택을 검증한다.
- [x] 가맹점 가입신청 제출 API는 아이디/이메일 중복을 검증하고, 매장명/주소/증빙자료는 검토 메모로 저장한다.
- [x] 가맹점 가입신청은 `승인요청`, `매장대기`, `결제비활성` 상태로 저장하고 승인 전 결제 기능을 활성화하지 않는다.
- [ ] 중복 가맹점 의심은 RiskFlag 또는 검토 사유로 저장하고 본사 또는 상위 조직 검토 대상에 포함한다.
- [x] 가맹점 가입신청의 KORION WALLET은 TRX 주소 형식, 중복, nonce 서명 인증 정책을 적용한다.
- [ ] 가맹점 최종 승인과 결제 기능 활성화는 본사 또는 상위 검토 API에서만 처리한다.
- [ ] 코드확인, 중복확인, 가입신청 제출, 증빙자료 업로드는 ActivityLog 또는 AuthActivityLog에 기록한다.
- [x] 가입, 인증, 중복확인, 제출 API는 프론트 공통 모달/토스트가 사용할 수 있는 표준 결과 코드와 사용자 표시용 메시지 키를 반환한다.
- [x] 중요 액션은 requestId 또는 idempotency key를 받아 중복 제출과 재시도를 추적한다.
- [x] 성공/실패/중복/인증완료 결과는 ActivityLog 또는 AuditLog에 기록한다.
- [x] 보안상 민감한 오류 원문은 API 응답과 로그 metadata에서 마스킹한다.
- [x] 재시도 가능한 오류와 재시도 불가능한 검증 오류를 구분해 응답한다.
- [x] 회원가입 신청은 `partner_applications`를 기준으로 저장하고 승인 전 실제 권한을 활성화하지 않는다.
- [x] `partner_applications` 상태는 `REQUESTED`, `REVIEWING`, `NEED_MORE_INFO`, `HOLD`, `APPROVED`, `REJECTED`, `CANCELLED` 전이를 기준으로 처리한다.
- [x] `partner_applications`는 기존 마케팅/제휴 신청 호환성을 유지하면서 총판 포털 회원가입 상태의 기준으로 사용한다.
- [x] 승인과 권한 활성화는 분리하고, `partner_applications.APPROVED`만으로 로그인 권한과 role 권한을 발급하지 않는다.
- [x] 회원가입 API는 비밀번호 원문을 로그/DB에 저장하지 않고 `password_hash`만 저장한다.
- [x] role 기반 라우팅은 `users`, `partners`, `merchants`, `distributor_contracts`의 승인/활성 상태를 함께 검증한다.
- [x] 코드 유효성 검증은 `ReferralCode`와 계약 경로 정책을 기준으로 수행한다.
- [x] Wallet 연동은 `WalletLink`로 저장하고 개인키/시드문구/PIN은 입력받거나 저장하지 않는다.
- [x] Wallet 연동은 KORION Wallet address 기반 인증이며, TRX 주소와 nonce 서명 검증 결과를 `WalletLink`에 저장한다.
- [x] Wallet 주소 중복 검증은 활성 계정뿐 아니라 진행 중인 신청 상태까지 포함한다.
- [ ] 승인 상태 전이는 `ApprovalWorkflow`에 기록하고 자료요청/보류/승인/거절 이력을 추적한다.
- [ ] 휴대폰/WhatsApp, Telegram, Wallet 중복 확인은 서버에서 표준 availability API로 처리한다.
- [ ] `LeaderProfile` 조회에서 리더의 국가 scope를 반환한다.
- [ ] dashboard API에서 `countryScope` 권한을 서버에서 검증한다.
- [ ] `Partner/Merchant summary` 조회는 리더의 국가 scope와 계약 관계 기준으로 필터링한다.
- [ ] 월 거래량은 거래 확정/취소/환불 상태 기준을 명확히 정의한다.
- [ ] 수수료 summary는 정산 경로 정책과 수수료 정책 스냅샷 기준으로 계산한다.
- [ ] 리스크 알림은 운영 기준을 정책화하고 하드코딩하지 않는다.
- [ ] API 응답에는 민감한 KORION WALLET 주소 전체값을 기본 포함하지 않는다.
- [ ] 파트너 후보 등록 API는 리더 국가 scope와 지역을 서버에서 검증한다.
- [ ] 파트너 후보 상태 전이는 `임시저장 -> 승인요청 -> 검토중 -> 자료요청/보류/승인/거절` 기준으로 관리한다.
- [ ] 리더는 `임시저장`, `승인요청`, `자료요청 대응`만 수행할 수 있다.
- [ ] 본사만 `검토중`, `보류`, `승인`, `거절` 상태 변경을 수행할 수 있다.
- [ ] 승인된 파트너만 정식 파트너 목록과 정산/수수료 계산 대상에 포함한다.
- [ ] 파트너 관리 API는 승인된 파트너만 반환한다.
- [ ] 파트너 목록/상세 API는 리더 국가 scope와 하위 조직 관계를 서버에서 검증한다.
- [ ] 활동 메모는 작성자와 변경 이력을 저장한다.
- [ ] 정지 요청과 권한 변경 요청은 본사 검토 상태로 생성한다.
- [ ] 상위 조직 변경은 리더 API에서 처리하지 않고 본사 API로만 허용한다.
- [ ] 가맹점 관리 API는 리더 국가 scope와 하위 조직 관계를 서버에서 검증한다.
- [ ] 가맹점 상태와 매장 상태를 분리해 저장한다.
- [ ] 허위 가맹점 의심은 RiskFlag 엔티티로 생성하고, 원인/탐지 기준/처리 상태를 저장한다.
- [ ] 리더의 재검토 요청은 본사 또는 담당 검수 플로우의 대기 상태로 생성한다.
- [ ] 리더 API는 가맹점 최종 승인/정지 상태를 직접 변경하지 않는다.
- [ ] 거래로그 API는 리더 국가 scope와 하위 조직 관계를 서버에서 검증한다.
- [ ] 결제 상태와 오프라인 Sync 상태, 정산 반영 상태를 분리해 저장/응답한다.
- [ ] Sync 실패 또는 장기 Sync 대기는 정산 보류 후보로 계산한다.
- [ ] 실패/취소/환불 거래는 정산 제외 또는 조정 후보로 분리한다.
- [ ] Excel 다운로드는 동일 필터/권한 검증을 재사용하고 다운로드 이력을 저장한다.
- [ ] 대량 다운로드 제한 기간과 최대 건수를 정책화한다.
- [ ] 정산 가능금액 계산은 마지막 정산일 다음날부터 신청일 전일까지의 확정 거래만 포함한다.
- [ ] 당일 거래는 정산 가능금액 계산에서 항상 제외한다.
- [ ] Sync 대기/실패, 실패, 취소, 환불, RiskFlag 보류 거래는 정산 가능금액에서 제외한다.
- [ ] 파트너 자동정산 금액은 정산 경로 정책과 지급 대상 정책 기준으로 계산한다.
- [x] 정산 신청 제출 시 임시저장 금액을 신뢰하지 않고 서버에서 재계산한다.
- [x] 본사 검토중/승인/지급완료/보류 상태 전이를 서버에서 관리한다.
- [ ] 정산내역 API는 SettlementRequest와 Commission을 연결해 응답한다.
- [ ] 정산내역 API는 리더 국가 scope와 하위 조직 관계를 서버에서 검증한다.
- [ ] 보류/조정필요 상태는 보류 사유, 조정 사유, 원 거래 또는 Commission 참조를 함께 저장한다.
- [ ] 지급완료 상태는 지급 대상 KORION WALLET 주소와 온체인 거래 식별자 또는 외부 지급 참조값을 연결한다.
- [ ] 정산내역 다운로드는 동일 필터/권한 검증을 재사용하고 다운로드 이력을 저장한다.
- [ ] 본사 소식지 API는 리더별 수신 대상과 국가 scope를 서버에서 검증한다.
- [ ] 본사 공지 읽음 상태는 리더 수신자별로 저장한다.
- [ ] 본사 공지 작성/수정/삭제는 본사 권한 API에서만 허용한다.
- [ ] 공지유형은 정책 데이터 또는 코드 테이블로 관리한다.
- [x] 리더 공지 발송 API는 수신자의 국가 scope와 하위 조직 관계를 서버에서 검증한다.
- [x] 공지 발송은 notice 본문과 recipient delivery 상태를 분리 저장한다.
- [ ] 예약 발송은 scheduler 또는 outbox worker에서 처리한다.
- [ ] 채널별 발송 실패는 수신자별 실패 사유와 재시도 상태를 저장한다.
- [ ] 읽음확인은 수신자별 read receipt로 저장한다.
- [x] 타 국가/타 조직 대상이 하나라도 포함되면 발송 요청 전체를 거절한다.
- [ ] 발송내역 API는 Notice, NoticeRecipient, ActivityLog를 연결해 응답한다.
- [ ] 읽음률은 발송 성공 수 기준으로 계산하고 실패 대상은 분모에서 제외한다.
- [ ] 일부실패 상태는 NoticeRecipient 실패 상태가 하나 이상 있을 때 산출한다.
- [ ] 재발송은 기존 Notice를 복제하거나 재발송 이벤트로 연결하되, 원본 Notice와 추적 가능해야 한다.
- [ ] 재발송 대상도 리더 국가 scope와 하위 조직 관계를 서버에서 재검증한다.
- [x] 파트너 대시보드 API는 partnerId 기준 own merchant scope를 서버에서 검증한다.
- [ ] 파트너 대시보드 API는 리더/본사 전체 데이터를 응답하지 않는다.
- [ ] 하위 가맹점 수와 월 거래량은 파트너가 유치한 활성/보류 가맹점 범위로 계산한다.
- [ ] 파트너 수수료와 정산가능금액은 정산 경로 정책과 지급 대상 정책 스냅샷 기준으로 계산한다.
- [ ] 정산가능/보류 상태는 정산 신청 가능 여부와 보류 사유를 함께 반환한다.
- [x] 가맹점 요청 관리 API는 partnerId 기준 own merchant scope를 서버에서 검증한다.
- [ ] 가맹점 요청 상태 전이는 `임시저장 -> 승인요청 -> 검토중 -> 자료요청/승인/거절` 기준으로 관리한다.
- [ ] 파트너는 `임시저장`, `등록요청`, `자료요청 대응`만 수행할 수 있다.
- [ ] 리더 또는 본사만 최종 승인/거절 상태 변경을 수행할 수 있다.
- [x] 승인된 가맹점 요청만 정식 가맹점 목록과 정산/거래 대상에 포함한다.
- [ ] 증빙 파일은 접근 권한과 보존 정책을 적용한다.
- [ ] 파트너 가맹점 관리 API는 partnerId 기준 own merchant scope를 서버에서 검증한다.
- [ ] 타 파트너 가맹점은 목록/상세/거래/수정요청 API 모두에서 차단한다.
- [ ] 파트너 정보수정 요청은 원본 가맹점 정보를 직접 변경하지 않고 변경 요청 엔티티로 저장한다.
- [ ] 파트너 거래확인 API는 해당 가맹점 거래만 응답하고 리더/본사 전체 거래는 응답하지 않는다.
- [ ] 정지 상태 가맹점의 수정요청 가능 항목은 정책 데이터로 관리한다.
- [ ] 파트너 수수료 정산 API는 partnerId 기준 본인 귀속 수수료만 계산한다.
- [ ] Sync 실패, 환불, 취소, 리스크 거래는 정산가능수수료에서 제외한다.
- [x] 파트너 정산 신청 상태 전이는 `REQUESTED -> APPROVED/REJECTED -> PAID` 기준으로 관리한다.
- [ ] 파트너 지급내역은 지급 대상 KORION WALLET 주소와 온체인 거래 식별자 또는 외부 지급 참조값을 연결한다.
- [x] 파트너 정산 신청 제출 시 정산가능수수료를 서버에서 재계산한다.
- [ ] 파트너 공지 수신 API는 partnerId 기준 수신 대상 여부를 서버에서 검증한다.
- [ ] 파트너 공지 발송 API는 own merchant scope 안의 하위 가맹점 수신자만 허용한다.
- [ ] 수신 공지와 발송 공지는 Notice 방향과 수신자 타입으로 구분 저장한다.
- [ ] 하위 가맹점 공지 발송 결과와 읽음 상태는 NoticeRecipient 기준으로 저장한다.
- [ ] 웰컴 공지 템플릿은 정책 데이터로 관리하고 코드에 하드코딩하지 않는다.
- [ ] 자료실 API는 파트너 권한, 국가 scope, 언어 scope를 서버에서 검증한다.
- [ ] 자료 버전은 immutable record로 저장하고 최신 버전 포인터를 별도 관리한다.
- [ ] 만료 자료는 다운로드 가능 여부를 정책으로 결정한다.
- [ ] 공유 링크는 만료 시간, 서명 토큰, 접근 범위를 가져야 한다.
- [ ] 다운로드와 공유 링크 생성은 ActivityLog 또는 ResourceAccessLog에 기록한다.
- [ ] 파트너 활동 로그 API는 partnerId 기준 조회 권한을 서버에서 검증한다.
- [ ] ActivityLog는 로그인, 가맹점 요청, 정산 신청, 공지 발송, 자료 다운로드/공유, 지갑 인증 이벤트에서 필수 생성한다.
- [ ] ActivityLog에는 actor, action type, status, target entity, request ID, createdAt을 저장한다.
- [ ] 민감 payload, 토큰, 개인키, 서명 원문은 저장하지 않거나 마스킹한다.
- [ ] 실패 ActivityLog는 실패 사유와 재시도 가능 여부를 저장한다.
- [ ] 가맹점 대시보드 API는 merchantId/storeId 기준 본인 매장 scope를 서버에서 검증한다.
- [ ] 가맹점 KPI와 거래량은 결제완료, Sync대기, Sync실패 상태 기준을 분리 계산한다.
- [ ] QR/NFC/BLE 상태는 하드코딩하지 않고 단말/결제수단 설정과 최근 Sync 상태를 기준으로 산출한다.
- [ ] 최근 결제 목록과 거래 상세 API는 본인 매장 거래만 반환한다.
- [ ] Sync실패 거래 상세에는 실패 사유와 정산 보류 여부를 포함하되 내부 payload는 마스킹한다.
- [ ] 가맹점 거래 상세 조회는 ActivityLog 또는 MerchantActivityLog에 기록한다.
- [ ] 가맹점 매장정보 API는 merchantId/storeId 기준 본인 매장 scope를 서버에서 검증한다.
- [ ] 중요 정보 변경은 직접 업데이트하지 않고 변경 요청 상태 `검토중`으로 생성한다.
- [ ] 본사 또는 상위 조직 검토 승인 후에만 중요 정보 변경을 실제 매장 정보에 반영한다.
- [ ] KORION WALLET 확인 API는 TRX 네트워크 주소와 nonce 서명을 검증하고 결과를 저장한다.
- [ ] 상품상태 변경 가능 권한과 매장 활성/비활성 상태 변경 권한을 분리한다.
- [ ] 매장정보 저장, 수정요청, Wallet 확인, 검토 결과 반영은 ActivityLog 또는 MerchantActivityLog에 기록한다.
- [ ] 가맹점 공지 API는 NoticeRecipient 기준 본인 매장 또는 계정 수신 범위를 서버에서 검증한다.
- [ ] 본사/리더/파트너 발신 공지는 발신 조직, 수신 범위, 읽음 상태를 분리 저장한다.
- [ ] 가맹점 프로필 저장 API는 변경 가능 필드만 직접 저장하고 중요 정보는 수정요청 API로 분기한다.
- [ ] 가맹점 활동 로그 API는 merchantId/storeId/userId 기준 조회 권한을 서버에서 검증한다.
- [ ] 공지 상세보기, 읽음 처리, 프로필 저장, 계정 변경, 활동로그 상세 조회는 ActivityLog에 기록한다.
- [ ] 활동 로그 상세 응답은 민감 payload, 토큰, 개인키, 서명 원문을 반환하지 않는다.

### 12.45 보류 및 확인 필요

- [ ] 로그인 진입 라우트를 `/auth`, `/login`, `/partners/login` 중 어디로 둘지 확정한다.
- [ ] 리더/파트너/가맹점 로그인 카드를 별도 로그인 화면으로 연결할지, 공통 로그인 화면에 role selection만 넘길지 확정한다.
- [ ] role selection과 실제 권한 불일치 시 차단할지, 올바른 대시보드로 자동 이동할지 정책을 확정한다.
- [ ] `partner_applications` 승인 후 `users`, `partners`, `merchants`, `distributor_contracts`를 생성 또는 연결하는 승인 API 범위를 확정한다.
- [ ] AuthActivityLog를 별도 테이블로 둘지 기존 ActivityLog에 통합할지 확정한다.
- [ ] 리더 2FA 수단을 TOTP, 이메일 OTP, SMS OTP 중 무엇으로 둘지 확정한다.
- [ ] 파트너 2FA 수단을 리더와 동일하게 둘지, 별도 정책으로 둘지 확정한다.
- [ ] 가맹점 2FA 수단을 리더/파트너와 동일하게 둘지, 별도 정책으로 둘지 확정한다.
- [ ] 로그인 유지 만료 시간과 신뢰 기기 정책을 확정한다.
- [ ] 리더 비밀번호 찾기 수단과 본사 승인/재설정 개입 여부를 확정한다.
- [ ] 파트너 비활성 상태의 접근 제한 범위를 로그인 차단, 조회 전용, 정산 차단 중 어디까지로 둘지 확정한다.
- [ ] 파트너 own merchants scope의 원천을 계약 소유, 추천자, 정산 경로 중 무엇으로 둘지 확정한다.
- [ ] 가맹점 own store scope의 원천을 merchantId, storeId, owner user mapping 중 무엇으로 둘지 확정한다.
- [ ] 매장 비활성 또는 결제 제한 상태에서 대시보드 조회를 허용할지, 결제/정산 액션만 차단할지 확정한다.
- [ ] 국가 리더 승인 상태의 원천을 `partners`, `LeaderProfile`, 별도 계정 테이블 중 어디로 둘지 확정한다.
- [ ] 파트너 가입신청 라우트를 `/partner/signup`, `/partners/apply`, 기존 공개 신청 라우트 중 어디로 둘지 확정한다.
- [ ] 파트너 가입신청은 `partner_applications` 저장 기준으로 API/프론트 필드 매핑을 확정한다.
- [ ] 리더코드 형식과 본사 직접 계약 신청의 구분 값을 확정한다.
- [ ] 파트너 가입신청 상태는 `partner_applications` 표준 상태 전이와 화면 라벨 매핑을 적용한다.
- [ ] 파트너 가입 시 KORION WALLET nonce 서명은 Wallet 연동 완료 조건으로 적용하고, 승인 전 필수/보완 가능 여부는 운영 정책으로 확정한다.
- [ ] 가맹점 가입신청 라우트를 `/merchant/signup`, `/merchants/apply`, 기존 공개 신청 라우트 중 어디로 둘지 확정한다.
- [ ] 가맹점 가입신청은 `partner_applications` 저장 기준으로 API/프론트 필드 매핑을 확정한다.
- [ ] 리더 코드, 파트너 코드, 본사 직접 계약 선택값의 우선순위와 동시 입력 제한 정책을 확정한다.
- [ ] 중복 가맹점 검토 기준을 매장명, 주소, 사업자/증빙자료, Wallet 중 어떤 조합으로 둘지 확정한다.
- [ ] 가맹점 가입신청 상태 `승인요청`, `매장대기`, `결제비활성`과 기존 가맹점 상태 코드의 매핑을 확정한다.
- [ ] 승인 전 결제 기능 비활성 범위를 QR/NFC/BLE 전체 차단인지, 테스트 결제 허용인지 확정한다.
- [ ] 공통 모달/토스트 메시지 키와 다국어 관리 위치를 확정한다.
- [ ] 성공 토스트 자동 닫힘 시간과 중요 결과의 모달 강제 표시 기준을 확정한다.
- [ ] ActivityLog와 AuditLog 중 어떤 이벤트를 어느 테이블에 기록할지 기준을 확정한다.
- [ ] requestId/idempotency key를 프론트에서 생성할지, 백엔드에서 발급할지 확정한다.
- [ ] `users`, `partners`, `merchants`, `partner_applications`, `ReferralCode`, `WalletLink`, `ApprovalWorkflow`의 실제 테이블명과 소유 서비스를 확정한다.
- [ ] 휴대폰과 WhatsApp을 동일 필드로 볼지 별도 연락 채널로 볼지 확정한다.
- [ ] Telegram 중복 확인 기준을 username, numeric id, 인증된 chat id 중 무엇으로 둘지 확정한다.
- [ ] Wallet 중복 확인 범위는 신청 중 상태와 승인 완료 계정을 모두 포함한다.
- [ ] `kori_partners`가 최종 구현 대상인지, 아니면 참고 UI 프로젝트인지 확정한다.
- [ ] 대시보드 API를 새 백엔드에 만들지, 기존 서비스에 붙일지 확정한다.
- [ ] 국가 리더의 국가 scope 소스 테이블과 계약 관계 기준을 확정한다.
- [ ] 수수료/정산 summary의 원천 데이터와 배치 주기를 확정한다.
- [ ] 리스크 알림 기준과 severity 산정 기준을 운영자가 확정한다.
- [ ] 파트너 후보의 실행계획 입력 방식이 텍스트인지 파일 첨부인지 확정한다.
- [ ] 자료요청 대응 시 추가 파일 첨부가 필요한지 확정한다.
- [ ] 파트너 후보 승인 후 KORION WALLET 재인증이 필요한지 확정한다.
- [ ] 파트너 비활성 상태의 의미가 영업 중단인지, 정산 제외인지, 로그인 제한인지 확정한다.
- [ ] 권한 변경 요청의 권한 종류와 승인 플로우를 확정한다.
- [ ] 활동 메모 수정/삭제 권한과 보존 기간을 확정한다.
- [ ] 가맹점 `문제` 상태와 RiskFlag 처리 상태의 관계를 확정한다.
- [ ] 허위 가맹점 의심 탐지 기준과 수동 등록 권한을 확정한다.
- [ ] 매장 상태 코드와 가맹점 승인 상태 코드를 분리 정의한다.
- [ ] 오프라인 결제의 Sync 대기 허용 시간과 Sync 실패 기준을 확정한다.
- [ ] Sync 실패 거래의 정산 보류 자동 처리 기준을 확정한다.
- [ ] Excel 다운로드 최대 기간, 최대 건수, 마스킹 정책을 확정한다.
- [ ] 마지막 정산일 기준을 리더별, 국가별, 정산 경로별 중 어디에 둘지 확정한다.
- [ ] 파트너 자동정산이 리더 정산신청 생성 시 즉시 생성되는지, 본사 승인 후 생성되는지 확정한다.
- [ ] 정산 신청 보류 상태에서 재신청 가능 조건을 확정한다.
- [ ] `조정필요` 상태가 수동 조정 대기인지 자동 재계산 대기인지 확정한다.
- [ ] SettlementRequest와 Commission의 연결 기준이 1:N인지 N:M인지 확정한다.
- [ ] 정산내역 다운로드 파일의 컬럼과 KORION WALLET 주소 마스킹 정책을 확정한다.
- [ ] 본사 공지 수신 대상 기준이 전체 리더, 국가별 리더, 개별 리더 중 어떤 단위인지 확정한다.
- [ ] 상세보기 시 자동 읽음 처리할지, 별도 읽음 처리 액션이 필요한지 확정한다.
- [ ] 공지유형 코드와 정책 링크/첨부파일 지원 범위를 확정한다.
- [ ] 리더 공지 발송 채널 범위와 채널별 템플릿 정책을 확정한다.
- [ ] 예약 발송 취소/수정 가능 시간을 확정한다.
- [ ] 읽음확인 보존 기간과 수신자별 개인정보 마스킹 기준을 확정한다.
- [ ] 재발송 대상 범위를 실패 대상만으로 제한할지, 미확인 대상까지 허용할지 확정한다.
- [ ] 읽음률 산식에서 실패/취소/예약대기 수신자를 제외할지 확정한다.
- [ ] 재발송 시 원본 Notice를 복제할지, 동일 Notice의 추가 발송 회차로 관리할지 확정한다.
- [ ] 파트너 own merchant scope의 기준이 계약 소유, 추천자, 정산 경로 중 무엇인지 확정한다.
- [ ] 파트너 정산가능금액에서 보류 제외 기준을 리더 정산과 동일하게 둘지 확정한다.
- [ ] 파트너 상세 이동/정산 이동의 대상 라우트를 확정한다.
- [ ] 가맹점 요청 최종 승인 주체가 리더인지 본사인지, 또는 조건별 분기인지 확정한다.
- [ ] 가맹점 증빙 파일 종류와 필수 여부를 확정한다.
- [ ] 자료요청 대응 시 기존 증빙 대체/추가 정책을 확정한다.
- [ ] 파트너 own merchant scope의 기준을 등록 요청 승인 기준으로 볼지, 계약 소유 기준으로 볼지 확정한다.
- [ ] 파트너 정보수정 요청의 승인 주체와 상태값을 확정한다.
- [ ] 정지 상태 가맹점의 파트너 조회/수정요청 허용 범위를 확정한다.
- [ ] 파트너 수수료 정산 상태값을 영문 코드로 고정할지, 기존 한글 정산 상태와 매핑할지 확정한다.
- [ ] 파트너 정산가능수수료의 기준 기간이 리더 정산일을 따르는지 별도 파트너 정산일을 따르는지 확정한다.
- [ ] 보류거래 해소 후 자동 정산가능수수료에 포함할지 수동 재계산할지 확정한다.
- [ ] 파트너가 발송 가능한 채널 범위와 템플릿 사용 강제 여부를 확정한다.
- [ ] 웰컴 공지 자동 발송 조건이 가맹점 승인 시점인지 파트너 수동 발송인지 확정한다.
- [ ] 파트너 발송 공지의 읽음확인 보존 기간과 개인정보 마스킹 기준을 확정한다.
- [ ] 자료 카테고리 체계와 국가/언어별 배포 범위를 확정한다.
- [ ] 만료 자료의 다운로드 허용 여부와 보존 기간을 확정한다.
- [ ] 공유 링크 외부 접근 허용 범위와 만료 시간 기본값을 확정한다.
- [ ] 자료 파일 저장소와 바이러스/악성 파일 검사 방식을 확정한다.
- [ ] ActivityLog 보존 기간과 검색 가능 기간을 확정한다.
- [ ] 활동 로그 상세에서 노출 가능한 메타데이터 범위를 확정한다.
- [ ] 실패/검토중 상태의 정의와 매핑 이벤트를 확정한다.
- [ ] 가맹점 대시보드의 KPI 범위가 매출, 결제 건수, 환불/취소 제외 금액 중 어디까지인지 확정한다.
- [ ] QR/NFC/BLE 상태 산출 기준이 단말 등록 상태인지, 최근 결제 성공률인지, Sync 상태인지 확정한다.
- [ ] 가맹점 계정과 매장 scope의 매핑 기준이 1:1인지 N:1인지 확정한다.
- [ ] Sync대기/Sync실패 거래를 가맹점 화면에서 정산 보류로 표시할지 단순 결제 상태로만 표시할지 확정한다.
- [ ] 가맹점이 직접 저장 가능한 필드와 수정요청이 필요한 중요 정보 필드를 확정한다.
- [ ] 중요 정보 변경 검토 주체가 본사인지, 리더인지, 파트너인지 계약 경로별로 확정한다.
- [ ] 상품상태 코드가 판매 가능 여부인지, 결제 가능 여부인지, 노출 가능 여부인지 확정한다.
- [ ] Wallet 변경 시 기존 정산 대기 건의 지급 주소 적용 기준을 확정한다.
- [ ] 가맹점이 수신하는 상위 조직 공지 범위를 본사, 리더, 파트너 중 어디까지 허용할지 확정한다.
- [ ] 공지 상세보기 시 자동 읽음 처리할지, 별도 읽음 처리 액션이 필요한지 가맹점 정책을 확정한다.
- [ ] 가맹점 프로필에서 직접 저장 가능한 필드와 매장정보 수정요청으로 보내야 하는 필드를 확정한다.
- [ ] 가맹점 ActivityLog 유형 코드와 보존 기간을 확정한다.
- [ ] 현재 로컬 `kori_partners` 미커밋 변경을 폐기할지, 리뷰 후 이어갈지 결정한다.

## 13. 1차 마일스톤

1. 로그인과 역할 기반 접근 제어 구현
2. 본사, 리더, 파트너, 가맹점 계약 구조 구현
3. 리더, 파트너, 가맹점 관리 데이터 구조 구현
4. KORION WALLET TRX 주소 등록과 nonce 서명 인증 구현
5. 리더와 파트너의 스토어 개설 차단 및 가맹점 전용 스토어 정책 구현
6. 수수료 정책, 정산 경로 정책, 정산 배치 생성 구현
7. 정산일 기반 거래 기간 산출과 정산일 변경 잠금 정책 구현
8. 정산 경로별 가맹점, 파트너, 리더, 본사 귀속 금액 분리 조회 구현
9. 정산 확정, 보류, 반려 상태 처리 구현
10. 가맹점, 파트너, 리더 KORION WALLET 지급 요청, 승인, 완료, 실패 상태 처리 구현
11. 감사 로그와 권한 검증 테스트 추가

## 14. 현재 구현 기준 및 미작업 점검

### 14.1 현재 반영 기준

- [x] 공개 파트너/가맹점 가입신청 저장소는 기존 `partner_applications`를 확장해서 사용한다.
- [x] 가입신청 생성 시 `users`, `partners`, `merchants` 권한은 생성하지 않고 승인 전 비활성 상태를 유지한다.
- [x] 가입신청 비밀번호는 평문 저장을 금지하고 `partner_applications.password_hash`에 해시로만 저장한다.
- [x] 가입신청 표준 상태는 `REQUESTED`, `REVIEWING`, `NEED_MORE_INFO`, `HOLD`, `APPROVED`, `REJECTED`, `CANCELLED`를 기준으로 한다.
- [x] 로그인 API는 role selection 기반으로 `/leader/dashboard`, `/partner/dashboard`, `/merchant/dashboard` 라우팅 값을 반환한다.
- [x] Wallet 인증은 KORION WALLET TRX 주소 기반 인증을 전제로 하며, 개인키/시드문구/PIN은 입력받지 않는다.
- [x] 프론트 가입 화면은 백엔드 auth API 기준으로 중복확인, 이메일 인증, ReferralCode 검증, Wallet 주소 검증, 가입신청 제출을 호출한다.
- [x] OpenAPI 기준 엔드포인트: `POST /api/auth/login`, `GET /api/auth/availability`, `GET /api/auth/referral-codes/{code}/validate`, `POST /api/auth/email-verifications/send`, `POST /api/auth/email-verifications/confirm`, `POST /api/auth/wallet-links/verify`, `POST /api/auth/signup-applications`.
- [x] Flyway V116은 `partner_applications` 확장, V117은 이메일/Wallet 검증 이력 테이블 생성 기준으로 정리한다.

### 14.2 검증 결과

- [x] `korion_chong`: `./gradlew test` 통과.
- [x] `kori_partners`: `npm run build` 통과.
- [x] `coin_system_flyway`: `./gradlew processResources --no-daemon` 통과.
- [ ] `coin_system_flyway`: `./gradlew flywayValidate --no-daemon`은 로컬 PostgreSQL `127.0.0.1:5432/coin_system_cloud` 접속 불가로 미검증.
- [ ] 로컬 환경에는 `psql`, `postgres`, `initdb`, `flyway` CLI가 없고 Docker socket도 없어 실제 DB 마이그레이션 적용 검증은 별도 DB 환경에서 수행해야 한다.

### 14.3 미작업 체크리스트

- [ ] KORION Wallet nonce 발급 API와 실제 서명 검증 어댑터를 확정하고 구현한다.
- [ ] 이메일 인증 발송 어댑터를 실제 메일/SMS/알림 서비스와 연결한다.
- [x] `partner_applications.APPROVED` 이후 `users`, `partners`, `merchants`, `distributor_contracts`, `distributor_wallet_addresses`를 생성 또는 연결하는 승인 API를 구현한다.
- [ ] `ApprovalWorkflow` 또는 상태 이력 테이블을 확정하고 상태 전이 actor/reason/requested materials를 저장한다.
- [ ] `requestId`와 idempotency key의 발급 주체, 중복 제출 처리, 재시도 정책을 확정한다.
- [ ] 파트너/가맹점 가입신청의 중복 정책을 `loginId`, `email`, `Telegram`, `WhatsApp`, `Wallet`, 매장명/주소/증빙 기준으로 세분화한다.
- [ ] 로그인 2FA, JWT/session 발급, refresh 정책, 신뢰 기기 정책을 구현한다.
- [ ] 리더/파트너/가맹점 scope 검증을 모든 대시보드, 관리, 거래로그, 정산, 공지 API에 일관 적용한다.
- [ ] 정산 기준일, 마지막 정산일, 보류 거래, Sync 실패/환불/취소 제외 정책을 코드 테이블 또는 정책 데이터로 분리한다.
- [ ] 지급 API는 정산금 기준으로 대상자의 KORION WALLET 주소와 온체인 거래 식별자 또는 외부 지급 참조값을 연결한다.
- [ ] 운영 DB에서 V116/V117 실제 적용, 롤백 리허설, 인덱스 생성 시간과 lock 영향을 확인한다.
- [ ] `coin_system_flyway` 로컬 V116 변경이 `partner_applications` 확장 기준과 일치하는지 실제 적용 전 재확인한다.

### 14.4 개발 여부 재점검 메모

- [x] 인증/가입신청 API 개발 확인: `/api/auth/login`, `/api/auth/availability`, `/api/auth/referral-codes/{code}/validate`, `/api/auth/email-verifications/send`, `/api/auth/email-verifications/confirm`, `/api/auth/wallet-links/verify`, `/api/auth/signup-applications`.
- [x] 리더 API 개발 확인: `/api/leader/dashboard`, `/api/leader/partners`.
- [x] 프론트 개발 확인: `/login`, `/login/:role`, `/signup/:role` 라우트와 가입신청 API 연동.
- [x] DB 설계 확인: V116 `partner_applications` 확장, V117 이메일/Wallet 검증 이력 테이블.
- [ ] 미구현 확인: 로그인 화면의 실제 백엔드 로그인 호출 연결은 아직 완료되지 않았다.
- [ ] 미구현 확인: 2FA, 로그인 유지 세션, 비밀번호 찾기, JWT/refresh token 발급은 아직 구현되지 않았다.
- [ ] 미구현 확인: Wallet nonce 발급 API와 실제 KORION Wallet 서명 검증 어댑터는 아직 구현되지 않았다.
- [ ] 미구현 확인: 가입신청 승인 후 `users`, `partners`, `merchants`, 계약, 지급 Wallet을 생성/연결하는 승인 API는 아직 구현되지 않았다.
- [ ] 미구현 확인: 파트너 등록 요청, 가맹점 관리, 거래로그, 정산신청/내역, 공지 발송/수신, 자료실, 활동 로그, 가맹점 API의 백엔드 실구현은 아직 남아 있다.
