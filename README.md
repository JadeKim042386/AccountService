# 계좌 시스템 개발

## Development Environment

- Intellij IDEA Ultimate
- Java 17
- Gradle 7.4.1
- Spring Boot 2.7.18

## API

### 계좌 생성 API

- POST /account
- 파라미터: 사용자 아이디, 초기 잔액
- 정책: 사용자가 없는 경우, 계좌가 10개(사용자당 최대 보유 가능 계좌 수)인 경우 실패 응답
- 응답: 사용자 아이디, 계좌번호, 등록일

### 계좌 해지 API

- DELETE /account
- 파라미터: 사용자 아이디, 계좌번호
- 정책: 사용자 또는 계좌가 없는 경우, 사용자 아이디와 계좌 소유주가 다른 경우, 계좌가 이미 해지 상태인 경우, 잔액이 있는 경우 실패 응답
- 응답: 사용자 아이디, 계좌번호, 해지일시

### 계좌 확인 API

- GET /account?user_id={userId}
- 파라미터: 사용자 아이디
- 정책: 사용자 없는 경우 실패 응답
- 응답: List<계좌번호, 잔액> 구조로 응답

### 잔액 사용 API

- POST /transaction/use
- 파라미터: 사용자 아이디, 계좌번호, 거래금액
- 정책: 사용자 없는 경우, 사용자 아이다와 계좌 소유주가 다른 경우, 계좌가 이미 해지 상태인 경우, 거래금액이 잔액보다 큰 경우, 거래금액이 잔액보다 큰 경우, 거래 금액이 너무 작거나 큰 경우 실패 응답
  - 해당 계좌에서 거래가 진행 중일 때 다른 거래 요청이 오는 경우 해당 거래가 동시에 잘못 처리되는 것을 방지해야한다.(동기화 문제)
- 응답: 계좌번호, 거래 결과 코드(성공/실패), 거래 아이디, 거래금액, 잔액, 거래일시

### 잔액 사용 취소 API

- POST /transaction/cancel
- 파라미터: 거래 아이디, 취소 요청 금액
- 정책: 거래 아이디에 해당하는 거래가 없는 경우, 거래금액과 거래 취소 금액이 다른 경우 실패 응답
- 응답: 계좌번호, 거래 결과 코드(성공/실패), 거래 아이디, 거래금액, 거래일시

### 거래 확인 API

- GET /transaction/{transactionId}
- 파라미터: 거래 아이디
- 정책: 해당 거래 아이디의 거래가 없는 경우 실패 응답
- 응답: 계좌번호, 거래종류(사용/취소), 거래 결과 코드(성공/실패), 거래 아이디, 거래금액, 거래 일시