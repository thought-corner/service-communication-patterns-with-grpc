# service-communication-patterns-with-grpc

> **gRPC 기반 MSA** — user·order 서비스를 독립 배포 단위로 분리하고, 서비스 간 호출을 **gRPC(HTTP/2 + Protocol Buffers) blocking stub**으로 수행한다.
> 호출은 **deadline로 대기 시간을 유한하게 묶고**, 다운스트림 장애는 **Resilience4j 재시도 + 서킷 브레이커**로
> 격리해 부분 장애에도 시스템이 계속 응답하도록 설계한 학습용 프로젝트.

---

## 이 프로젝트가 다루는 것

MSA에서 서비스를 나누는 순간 **"한 서비스의 장애가 다른 서비스로 어떻게 전파되는가"** 가 핵심 문제가 된다.
이 저장소는 그 문제를, 서비스 간 통신을 **gRPC blocking stub**으로 구성하고 **재시도 + 서킷 브레이커로 격리**하는 방식으로 다룬다.

- 도메인을 **user-service / order-service** 두 개의 독립 배포 단위로 분리
- user-service가 order-service를 **gRPC(server-to-server)** 로 호출해 사용자 프로필과 주문 목록을 **합성(API Composition)**
- 호출을 **gRPC blocking stub**으로 수행하되, **deadline(4초)** 으로 대기 시간을 유한하게 묶어 스레드가 무한정 점유되지 않게 함
- 다운스트림 장애는 **재시도(bounded) + 서킷 브레이커 + deadline** 으로 격리하고, 종착 실패는 엔드포인트 성격에 따라 **부가=폴백(degrade) / 필수=503** 으로 분기

## 시스템 구성

| 서비스               | 프로토콜 / 포트           | 책임                                                                      | 저장소            |
|-------------------|---------------------|-------------------------------------------------------------------------|----------------|
| **user-service**  | REST 8081           | 사용자 도메인, `/users/me` 응답을 위해 order-service를 **gRPC로 호출하는 쪽(Aggregator)** | H2 (in-memory) |
| **order-service** | gRPC 9090 (관리 8082) | 주문 도메인, 주문 생성·조회를 **gRPC로 제공하는 쪽(Provider)**                            | H2 (in-memory) |

두 서비스는 DB를 공유하지 않고(Database per Service), user-service가 order-service의 gRPC API를 호출해 사용자 프로필과 주문 목록을 조합한다.

```
Client ──REST──▶ user-service :8081 ──gRPC(HTTP/2 + Protobuf)──▶ order-service :9090
                  (외부 API는 REST 유지)   │  OrderQueryService.GetOrders(userId)
                  재시도+서킷으로 감쌈  ◀───┘  종착 실패 시 부가=UNAVAILABLE degrade / 필수=503
```

> 외부 클라이언트(브라우저·모바일)는 gRPC를 직접 호출하기 어렵다. 그래서 user-service의 **외부 API는 REST로 유지**하고, **서비스 간 내부 호출만 gRPC**로 둔다.

---

## 통신 구조 — API Composition

user-service는 **aggregator(BFF 성격)** 다. `/users/me`는 사용자 프로필을 만들기 위해 order-service를 호출해 **프로필 + 주문을 하나로 합성**한다.
주문을 받아와야 응답을 만들 수 있으므로 **데이터 의존성은 요청-응답 합성**이며, 이는 전송 방식(REST/gRPC)과 무관하게 유지된다.
gRPC로 바뀌는 것은 **전송 계층(HTTP/2 + Protobuf)과 계약 방식(.proto 스키마 우선)** 이다. 호출 자체는 REST 시절과 동일하게 **동기 블로킹**이다(gRPC blocking
stub).

| 구분       | 기존 (REST)             | 현재 (gRPC)                   |
|----------|-----------------------|-----------------------------|
| 직렬화      | JSON (텍스트)            | Protocol Buffers (바이너리)     |
| 전송       | HTTP/1.1              | HTTP/2 (멀티플렉싱)              |
| 계약       | 문서 / 암묵               | `.proto` 스키마 우선, 코드젠으로 강제   |
| 클라이언트 호출 | 동기 블로킹 (RestTemplate) | 동기 블로킹 (gRPC blocking stub) |

---

## 문제 인식 — 서비스 간 호출의 결합

`/users/me`는 사용자 프로필을 만들기 위해 order-service를 호출한다. order-service가 **느려지거나 죽으면** 다음이 연쇄적으로 발생할 수 있다.

1. user-service의 요청 처리 자원이 응답을 기다리며 묶인다.
2. 호출이 쌓이면 **자원(스레드 풀)이 고갈**된다.
3. 결국 order와 **무관한 사용자 조회 기능까지 함께 죽는다** — 장애가 서비스 경계를 넘어 전파(cascading failure)된다.

즉 서비스는 분리했지만 **런타임 결합(temporal coupling)** 은 남아, order-service의 가용성이 user-service의 가용성을 그대로 끌어내린다.
이 프로젝트는 이 결합을 **두 층위**로 끊는다 — (1) **deadline**로 한 호출이 스레드를 붙잡는 시간을 유한하게 묶고, (2) **재시도 + 서킷 브레이커**로 다운스트림 장애를 격리한다.

## 해결 (1) — deadline로 스레드 점유 시간을 유한하게

gRPC 호출은 **blocking stub**으로 수행한다(코루틴/논블로킹 아님). 블로킹 호출은 응답을 기다리는 동안 요청 스레드를 점유하므로, **다운스트림이 무한정 느려지면 스레드가 계속 묶여 위 2·3번이
발생**한다. 핵심은 그 점유 시간을 **유한하게 bound**하는 것이다.

- 모든 호출에 **gRPC deadline(4초, `withDeadlineAfter`)** 을 건다. 다운스트림이 죽거나 멈춰도 스레드는 최대 4초 뒤 `DEADLINE_EXCEEDED`로 풀려나므로, 점유가
  무한정 쌓이지 않는다.
- 스레드를 아예 반납하는 **논블로킹(코루틴/R2DBC)까지 가지 않은 이유**: 이 프로젝트의 주제는 **통신 패턴(전송·계약·장애 격리)** 이지 리액티브 스택 전환이 아니다. 동기 블로킹을 유지하되
  deadline + 서킷 브레이커로 스레드 점유가 무한정 쌓이는 것을 막는다.
- 점유 자체를 0으로 만드는 것은 **서킷 브레이커의 OPEN 상태**다(해결 2). OPEN이면 호출을 아예 시도하지 않고 즉시 실패시켜 스레드를 잡지 않는다.

## 해결 (2) — 재시도 + 서킷 브레이커로 다운스트림 장애 격리

deadline로 개별 호출 시간을 묶어도 **다운스트림이 계속 실패·지연하는 상황 자체**는 남는다.
order-service 호출을 **Resilience4j 재시도(Retry) + 서킷 브레이커(CircuitBreaker)** 로 감싼다(둘 다 동기 데코레이터, `decorateWithResilience`).

- **재시도(외)** — 일시적 실패(`UNAVAILABLE`·`RESOURCE_EXHAUSTED`)에 한해 최대 3회, **지수 백오프 + jitter**로 재시도한다. 조회(GetOrders)라
  idempotent해서 안전하다.
- **서킷 브레이커(내)** — 실패가 임계치를 넘으면 회로를 열어(OPEN) 호출 자체를 즉시 차단(fail-fast)한다. 데코레이터 순서가 **Retry(외)→CircuitBreaker(내)** 이고
  `CallNotPermittedException`은 재시도 대상이 아니므로, **서킷이 OPEN이면 재시도로 부하를 증폭시키지 않는다.**

종착 실패(재시도 소진/서킷 OPEN) 시 처리는 **엔드포인트 성격**에 따라 갈린다.

- **부가 조회 `GET /users/me`** — `OrdersResult.UNAVAILABLE`로 **degrade**. order-service가 죽어도 프로필은 **주문만 비운 채 200**으로 내려준다.
- **필수 조회 `GET /users/me/orders`** — 하드 실패. `OrdersUnavailableException`을 던져 **503 + `Retry-After`** 로 매핑한다.

<p align="center">
  <img src="docs/images/circuit-breaker-states.png" width="800"/>
</p>

**상태 전이 규칙**

- **CLOSED** — 정상 상태. 호출을 그대로 통과시키되 최근 호출의 실패율을 집계한다.
- **OPEN** — 최근 10건 중 최소 5건이 집계된 상태에서 **실패율이 50%를 넘으면** 전이. 이후 모든 호출을 즉시 차단하고 `OrdersResult.UNAVAILABLE` 을 반환한다. (user
  프로필 응답은 200 유지)
- **HALF_OPEN** — OPEN 진입 후 10초가 지나면 자동 전이. 시험 호출 3건을 흘려보내 정상이면 CLOSED로 회복, 실패하면 다시 OPEN.

**핵심 설계 결정**

| 항목                                           | 값 / 정책                                                           | 이유                                                                     |
|----------------------------------------------|------------------------------------------------------------------|------------------------------------------------------------------------|
| `slidingWindowSize` ≥ `minimumNumberOfCalls` | 10 ≥ 5                                                           | 최소 호출 수가 윈도우보다 크면 실패율이 영원히 계산되지 않아 **회로가 절대 열리지 않는다.** 이 불변식을 반드시 지킨다. |
| `recordException`                            | `StatusRuntimeException`의 `UNAVAILABLE` · `RESOURCE_EXHAUSTED`   | **일시적 다운스트림 장애만** 실패로 집계 (재시도 대상과 동일 predicate)                        |
| 미집계(성공 취급)                                   | 위 predicate에 안 걸리는 나머지 status (`NOT_FOUND`·`INVALID_ARGUMENT` 등) | 호출자 잘못이므로 회로를 여는 데 카운트하지 않는다 (기존 4xx 대응)                               |
| slow call                                    | `slowCallDurationThreshold` 2초, `slowCallRateThreshold` 50%      | 실패가 아니어도 **느린 응답이 절반을 넘으면** OPEN                                       |
| deadline                                     | gRPC `withDeadlineAfter(4초)`                                     | 서킷만으로는 못 막는 **느린/멈춘 호출**을 스레드 레벨에서 차단 (TimeLimiter 미사용)                |
| 재시도(Retry)                                   | `maxAttempts` 3, 지수 백오프+jitter(200ms·2.0·0.5)                    | 일시적 blip은 재시도로 흡수, 서킷 OPEN이면 fail-fast                                 |
| 폴백(degrade)                                  | `OrdersResult(status = UNAVAILABLE)` — **부가 조회 한정**              | 장애를 **예외가 아닌 값**으로 표현. 필수 조회는 예외→503로 하드 실패                            |

상태 전이(CLOSED/OPEN/HALF_OPEN)는 `CircuitBreakerRegistry` 이벤트 리스너로 로깅해 장애·회복 과정을 관측할 수 있다.

### 서킷 브레이커 예외 재분류 (REST → gRPC)

REST는 예외 "클래스"로 실패를 구분했지만, gRPC 호출은 전부 `StatusRuntimeException`으로 던져지므로 **status code로 구분**해야 한다.

| REST 시절                            | gRPC                                         | 서킷 반영                                          |
|------------------------------------|----------------------------------------------|------------------------------------------------|
| `RestClientException` (커넥션 실패)     | `StatusRuntimeException(UNAVAILABLE)`        | **실패로 집계 + 재시도**                               |
| 과부하                                | `StatusRuntimeException(RESOURCE_EXHAUSTED)` | **실패로 집계 + 재시도**                               |
| `TimeoutException` (느리지만 완료, 2~4초) | 성공 응답 but `duration > 2초`                    | **slow call로 집계** (실패 아님, slow-rate로 회로 반영)    |
| `TimeoutException` (deadline 초과)   | `StatusRuntimeException(DEADLINE_EXCEEDED)`  | **재시도 ✗ · 실패집계 ✗** — 4초>2초라 **slow call로만 집계** |
| `4xx (HttpClientErrorException)`   | `INVALID_ARGUMENT`, `NOT_FOUND`              | **무시** (호출자 잘못, 재시도 안 함)                       |

> **`DEADLINE_EXCEEDED`가 "실패"가 아닌 이유:** `recordException` predicate가 `UNAVAILABLE`·`RESOURCE_EXHAUSTED`만 실패로 세므로
> deadline 초과 자체는 서킷의 *실패 카운트*에 들어가지 않는다. 대신 `ignoreException`도 아니라서 Resilience4j는 이를 duration이 붙은 호출로 보고, deadline(4초) >
`slowCallDurationThreshold`(2초)이므로 **slow call로 집계**해 slow-call rate 경로로 회로를 연다. 재시도 predicate에도 없어 **재시도하지 않는다**(무한정 느린
> 다운스트림에 재시도를 퍼붓지 않기 위함). 애플리케이션 레벨에서는 다른 `StatusRuntimeException`과 함께 잡혀 부가=degrade / 필수=503으로 종착한다.

---

## 왜 gRPC인가 — 트레이드오프

gRPC로 전환하면서 얻는 것과 내주는 것을 분명히 해둔다.

| 얻는 것                              | 내주는 것                                |
|-----------------------------------|--------------------------------------|
| Protobuf 바이너리 → 작은 페이로드·빠른 (역)직렬화 | 사람이 못 읽는 바이너리 → curl/브라우저로 디버깅 어려움   |
| HTTP/2 멀티플렉싱·양방향 스트리밍             | 브라우저가 직접 호출 불가 → 게이트웨이 / gRPC-Web 필요 |
| `.proto` 스키마로 서비스 계약 강제, 다국어 코드젠  | 스키마·코드젠 빌드 파이프라인과 러닝커브 추가            |
| L7 로드밸런싱 시 커넥션 재사용 효율             | L4 로드밸런서로는 커넥션이 고착 → L7(프록시) 필요      |

## API 계약

### order-service — gRPC (9090)

`.proto` 스키마로 계약을 정의한다. 공통 모듈로 빼지 않고 **각 서비스가 자신의 `src/main/proto`에 동일한 계약을 두고 코드젠**한다(user는 stub, order는 server base).
gRPC는 **조회(GetOrders)만** 노출하고, 주문 생성은 order-service의 REST(`POST /orders/{userId}`)로 제공한다.

```proto
syntax = "proto3";
package order.v1;

service OrderQueryService {
  rpc GetOrders (GetOrdersRequest) returns (GetOrdersResponse);
}

message GetOrdersRequest  { string user_id = 1; }
message GetOrdersResponse { repeated Order orders = 1; }

message Order {
  string order_id    = 1;
  string product_id  = 2;
  int32  qty         = 3;
  int32  unit_price  = 4;
  int64  total_price = 5;   // Int 오버플로 방지를 위해 int64
  string user_id     = 6;
  string created_at  = 7;   // ISO-8601 문자열
  string updated_at  = 8;
}
```

### user-service — REST (8081)

| Method | Path               | 설명                                                                      |
|--------|--------------------|-------------------------------------------------------------------------|
| `POST` | `/users`           | 회원 가입                                                                   |
| `POST` | `/login`           | 로그인 → 토큰 발급                                                             |
| `GET`  | `/users`           | 전체 사용자 조회                                                               |
| `GET`  | `/users/me`        | 내 프로필 + 내 주문(**부가**) — 종착 실패 시 `ordersStatus=UNAVAILABLE`로 degrade(200) |
| `GET`  | `/users/me/orders` | 내 주문(**필수**) — 종착 실패 시 **503 + `Retry-After`** 하드 실패                    |

---

## 기술 스택

- **Kotlin 2.3**, **Spring Boot 4.1** (Java 17)
- **Spring gRPC** (`org.springframework.grpc`) — gRPC 서버 / **blocking stub** 클라이언트, **Protocol Buffers** (
  `com.google.protobuf` Gradle 플러그인 코드젠)
- **Resilience4j** — 재시도(Retry) · 서킷 브레이커(CircuitBreaker) 동기 데코레이터, gRPC deadline
- **Spring Data JPA + H2** (Database per Service)
- **Spring Security + JWT** (user-service 외부 API 무상태 인증), **Spring Boot Actuator + Prometheus** (관측성)

## 실행 방법

```bash
# order-service (gRPC Provider) 먼저 기동 (:9090)
./gradlew :order:bootRun

# user-service (Consumer) (:8081)
./gradlew :user:bootRun
```

- user-service → order-service gRPC 채널: 기본 `127.0.0.1:9090` (환경변수 `ORDER_SERVICE_GRPC_HOST` · `ORDER_SERVICE_GRPC_PORT`
  로 오버라이드 — 외부화된 설정)
- gRPC 엔드포인트는 curl 대신 **grpcurl** 로 확인한다:
  ```bash
  grpcurl -plaintext -d '{"user_id":"<uuid>"}' 127.0.0.1:9090 order.v1.OrderQueryService/GetOrders
  ```
- **서킷 브레이커 확인**: order-service를 내린 뒤 `GET /users/me` 를 반복 호출하면, 실패가 누적되며 회로가 OPEN으로 전이되고 응답의 `ordersStatus` 가
  `UNAVAILABLE` 로 바뀌는 것을 확인할 수 있다. 이때도 프로필 응답은 200으로 유지된다.
