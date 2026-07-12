# service-communication-patterns-with-grpc

> **gRPC 기반 MSA** — user·order 서비스를 독립 배포 단위로 분리하고, 서비스 간 호출을 **gRPC(HTTP/2 + Protocol Buffers)** 로 수행한다.
> 호출은 **논블로킹 코루틴**으로 처리해 스레드 풀 고갈형 장애 전파를 근본에서 막고, 남는 다운스트림 장애는 **Resilience4j 서킷 브레이커**로
> 격리해 부분 장애에도 시스템이 계속 응답하도록 설계한 학습용 프로젝트.

---

## 이 프로젝트가 다루는 것

MSA에서 서비스를 나누는 순간 **"한 서비스의 장애가 다른 서비스로 어떻게 전파되는가"** 가 핵심 문제가 된다.
이 저장소는 그 문제를, 서비스 간 통신을 **gRPC + 논블로킹 코루틴**으로 구성하고 **서킷 브레이커로 격리**하는 방식으로 다룬다.

- 도메인을 **user-service / order-service** 두 개의 독립 배포 단위로 분리
- user-service가 order-service를 **gRPC(server-to-server)** 로 호출해 사용자 프로필과 주문 목록을 **합성(API Composition)**
- 호출을 **논블로킹 코루틴 스텁**으로 수행해 대기 중 요청 스레드를 점유하지 않음
- 남는 다운스트림 장애는 **서킷 브레이커 + deadline + 폴백(degrade)** 으로 끊어냄

## 시스템 구성

| 서비스               | 프로토콜 / 포트          | 책임                                                                 | 저장소            |
|-------------------|--------------------|--------------------------------------------------------------------|----------------|
| **user-service**  | REST 8081          | 사용자 도메인, `/users/me` 응답을 위해 order-service를 **gRPC로 호출하는 쪽(Aggregator)** | H2 (in-memory) |
| **order-service** | gRPC 9090 (관리 8082) | 주문 도메인, 주문 생성·조회를 **gRPC로 제공하는 쪽(Provider)**                        | H2 (in-memory) |

두 서비스는 DB를 공유하지 않고(Database per Service), user-service가 order-service의 gRPC API를 호출해 사용자 프로필과 주문 목록을 조합한다.

```
Client ──REST──▶ user-service :8081 ──gRPC(코루틴 · HTTP/2+Protobuf)──▶ order-service :9090
                  (외부 API는 REST 유지)   │  OrderService.GetOrders(userId)
                  서킷 브레이커로 감쌈  ◀───┘  실패 시 OrdersResult.UNAVAILABLE 로 degrade
```

> 외부 클라이언트(브라우저·모바일)는 gRPC를 직접 호출하기 어렵다. 그래서 user-service의 **외부 API는 REST로 유지**하고, **서비스 간 내부 호출만 gRPC**로 둔다.

---

## 통신 구조 — API Composition

user-service는 **aggregator(BFF 성격)** 다. `/users/me`는 사용자 프로필을 만들기 위해 order-service를 호출해 **프로필 + 주문을 하나로 합성**한다.
주문을 받아와야 응답을 만들 수 있으므로 **데이터 의존성은 요청-응답 합성**이며, 이는 전송 방식(REST/gRPC)과 무관하게 유지된다.
gRPC로 바뀌는 것은 **전송 계층(HTTP/2 + Protobuf)과 스레딩 모델(논블로킹 코루틴)** 이다.

| 구분          | 기존 (REST)              | 현재 (gRPC)                          |
|-------------|------------------------|------------------------------------|
| 직렬화         | JSON (텍스트)             | Protocol Buffers (바이너리)           |
| 전송          | HTTP/1.1               | HTTP/2 (멀티플렉싱)                    |
| 계약          | 문서 / 암묵                | `.proto` 스키마 우선, 코드젠으로 강제       |
| 클라이언트 호출    | 동기 블로킹 (RestTemplate)  | 논블로킹 코루틴 스텁 (grpc-kotlin)        |

---

## 문제 인식 — 서비스 간 호출의 결합

`/users/me`는 사용자 프로필을 만들기 위해 order-service를 호출한다. order-service가 **느려지거나 죽으면** 다음이 연쇄적으로 발생할 수 있다.

1. user-service의 요청 처리 자원이 응답을 기다리며 묶인다.
2. 호출이 쌓이면 **자원(스레드 풀)이 고갈**된다.
3. 결국 order와 **무관한 사용자 조회 기능까지 함께 죽는다** — 장애가 서비스 경계를 넘어 전파(cascading failure)된다.

즉 서비스는 분리했지만 **런타임 결합(temporal coupling)** 은 남아, order-service의 가용성이 user-service의 가용성을 그대로 끌어내린다.
이 프로젝트는 이 결합을 **두 층위**로 끊는다 — (1) 논블로킹으로 스레드 점유 자체를 없애고, (2) 남는 다운스트림 장애를 서킷 브레이커로 격리한다.

## 해결 (1) — 논블로킹 코루틴으로 스레드 점유 제거

gRPC 호출을 **코루틴(grpc-kotlin) 스텁**으로 수행한다.

- 블로킹 호출은 응답을 기다리는 동안 **요청 스레드를 점유**한다. 호출이 쌓이면 스레드 풀이 고갈되며 위 2·3번이 발생한다.
- 코루틴은 I/O 대기 중 **스레드를 반납**한다. 같은 스레드 풀로 더 많은 동시 요청을 처리하므로 **스레드 풀 고갈형 전파를 근본에서 없앤다.**
- **주의:** 코루틴은 단일 호출을 더 빠르게 만들지 않는다 — **지연(latency)이 아니라 동시성 하 처리량(throughput) 개선**이다.
  또 이 이득은 요청 경로 전체가 스레드를 점유하지 않을 때만 실현되므로, 로컬 JPA 조회는 `Dispatchers.IO`로 오프로딩한다.
  (→ [JPA는 왜 R2DBC로 바꾸지 않는가](#jpa는-왜-r2dbc로-바꾸지-않는가))

## 해결 (2) — 서킷 브레이커로 남은 장애 격리

논블로킹으로 스레드 고갈은 막아도 **다운스트림이 계속 실패·지연하는 상황 자체**는 남는다.
order-service 호출을 **Resilience4j 서킷 브레이커**(`resilience4j-kotlin`의 suspend 확장)로 감싼다.
실패가 임계치를 넘으면 회로를 열어(OPEN) 호출 자체를 즉시 차단하고 미리 정의한 **폴백 값으로 degrade** 한다.
덕분에 order-service가 죽어도 user-service는 **주문만 비운 채 프로필 응답을 계속 200으로 내려준다.**

<p align="center">
  <img src="docs/images/circuit-breaker-states.png" width="800"/>
</p>

**상태 전이 규칙**

- **CLOSED** — 정상 상태. 호출을 그대로 통과시키되 최근 호출의 실패율을 집계한다.
- **OPEN** — 최근 10건 중 최소 5건이 집계된 상태에서 **실패율이 50%를 넘으면** 전이. 이후 모든 호출을 즉시 차단하고 `OrdersResult.UNAVAILABLE` 을 반환한다. (user 프로필 응답은 200 유지)
- **HALF_OPEN** — OPEN 진입 후 10초가 지나면 자동 전이. 시험 호출 3건을 흘려보내 정상이면 CLOSED로 회복, 실패하면 다시 OPEN.

**핵심 설계 결정**

| 항목                                           | 값 / 정책                                                    | 이유                                                                     |
|----------------------------------------------|-----------------------------------------------------------|------------------------------------------------------------------------|
| `slidingWindowSize` ≥ `minimumNumberOfCalls` | 10 ≥ 5                                                    | 최소 호출 수가 윈도우보다 크면 실패율이 영원히 계산되지 않아 **회로가 절대 열리지 않는다.** 이 불변식을 반드시 지킨다. |
| `recordException`                            | `StatusRuntimeException`의 `UNAVAILABLE` · `DEADLINE_EXCEEDED` | **다운스트림 장애만** 실패로 집계                                                   |
| `ignoreException`                            | `StatusRuntimeException`의 `INVALID_ARGUMENT` · `NOT_FOUND`   | 호출자 잘못이므로 회로를 여는 데 카운트하지 않는다 (기존 4xx 대응)                            |
| deadline / TimeLimiter                       | gRPC **deadline** + `withTimeout(4초)`                     | 서킷 브레이커만으로는 못 막는 **느린 응답**을 타임아웃으로 차단                                |
| 폴백(degrade)                                  | `OrdersResult(status = UNAVAILABLE)`                      | 장애를 **예외가 아닌 값**으로 표현해, 호출부가 부분 응답을 정상 흐름으로 처리                      |

상태 전이(CLOSED/OPEN/HALF_OPEN)는 `CircuitBreakerRegistry` 이벤트 리스너로 로깅해 장애·회복 과정을 관측할 수 있다.

### 서킷 브레이커 예외 재분류 (REST → gRPC)

REST는 예외 "클래스"로 실패를 구분했지만, gRPC 호출은 전부 `StatusRuntimeException`으로 던져지므로 **status code로 구분**해야 한다.

| REST 시절                          | gRPC                                        | 서킷 반영                    |
|----------------------------------|--------------------------------------------|--------------------------|
| `RestClientException`            | `StatusRuntimeException(UNAVAILABLE)`      | **실패로 집계** (다운스트림 장애) |
| `TimeoutException`               | `StatusRuntimeException(DEADLINE_EXCEEDED)`| **실패로 집계** (느린 호출)    |
| `4xx (HttpClientErrorException)` | `INVALID_ARGUMENT`, `NOT_FOUND`            | **무시** (호출자 잘못)        |

---

## 왜 gRPC인가 — 트레이드오프

gRPC로 전환하면서 얻는 것과 내주는 것을 분명히 해둔다.

| 얻는 것                                     | 내주는 것                                        |
|------------------------------------------|----------------------------------------------|
| Protobuf 바이너리 → 작은 페이로드·빠른 (역)직렬화     | 사람이 못 읽는 바이너리 → curl/브라우저로 디버깅 어려움     |
| HTTP/2 멀티플렉싱·양방향 스트리밍                  | 브라우저가 직접 호출 불가 → 게이트웨이 / gRPC-Web 필요   |
| `.proto` 스키마로 서비스 계약 강제, 다국어 코드젠     | 스키마·코드젠 빌드 파이프라인과 러닝커브 추가              |
| L7 로드밸런싱 시 커넥션 재사용 효율                 | L4 로드밸런서로는 커넥션이 고착 → L7(프록시) 필요        |

## JPA는 왜 R2DBC로 바꾸지 않는가

논블로킹으로 간다면 블로킹 기반 JPA도 R2DBC로 바꿔야 하는 것 아니냐는 질문이 자연스럽다. **바꾸지 않는다.**
성격이 다른 두 I/O를 구분하는 것이 요점이다.

| I/O 종류                        | 특성                                | 논블로킹 처리                                       |
|------------------------------|-----------------------------------|-----------------------------------------------|
| **크로스서비스 gRPC 호출 (order)** | 느려지거나 죽으면 **cascading failure의 주범** | grpc-kotlin 코루틴 스텁으로 **진짜 논블로킹** (R2DBC와 무관) |
| **로컬 JPA 조회 (H2)**          | 빠른 로컬 연산, 크로스서비스 의존 아님           | `Dispatchers.IO` 오프로딩으로 요청 스레드만 반납           |

- 논블로킹이 **정말 값어치를 하는 지점(gRPC 호출)은 이미 커버**된다.
- R2DBC는 엔티티 매핑·리포지토리 시맨틱·`@Transactional`·lazy loading을 **전면 재작성**해야 한다 → "통신 패턴" 프로젝트가 "리액티브 스택 재작성"으로 변질된다.
- 코루틴 gRPC + `Dispatchers.IO` JPA는 해킹이 아니라 **프로덕션 표준 브리지**다.
- **R2DBC가 값어치를 하는 조건:** DB가 *원격·고지연·고처리량*이라 DB 대기 자체가 스레드 풀을 고갈시킬 때. 지금은 H2 인메모리라 해당 없음.

---

## API 계약

### order-service — gRPC (9090)

`.proto` 스키마로 계약을 정의하고, 두 서비스가 생성된 스텁을 공유한다(`:proto` 모듈).

```proto
syntax = "proto3";
package order;

import "google/protobuf/timestamp.proto";

service OrderService {
  rpc GetOrders   (GetOrdersRequest)   returns (GetOrdersResponse);
  rpc CreateOrder (CreateOrderRequest) returns (Order);
}

message GetOrdersRequest  { string user_id = 1; }
message GetOrdersResponse { repeated Order orders = 1; }

message CreateOrderRequest {
  string user_id    = 1;
  string product_id = 2;
  int32  qty        = 3;
  int32  unit_price = 4;
}

message Order {
  string order_id    = 1;
  string product_id  = 2;
  int32  qty         = 3;
  int32  unit_price  = 4;
  int64  total_price = 5;                        // Int 오버플로 방지를 위해 int64
  google.protobuf.Timestamp created_at = 6;
  google.protobuf.Timestamp updated_at = 7;
}
```

### user-service — REST (8081)

| Method | Path        | 설명                                                        |
|--------|-------------|-----------------------------------------------------------|
| `POST` | `/users`    | 회원 가입                                                     |
| `POST` | `/login`    | 로그인 → 토큰 발급                                               |
| `GET`  | `/users`    | 전체 사용자 조회                                                 |
| `GET`  | `/users/me` | 내 프로필 + 내 주문 조회 (**order-service를 gRPC + 서킷 브레이커 경유로 호출**) |

---

## 기술 스택

- **Kotlin 2.3**, **Spring Boot 4.1** (Java 17)
- **Spring gRPC** + **grpc-kotlin** — 코루틴 gRPC 서버/클라이언트, **Protocol Buffers** (`com.google.protobuf` Gradle 플러그인 코드젠)
- **kotlinx-coroutines** — `suspend` 기반 논블로킹 서비스/컨트롤러, 블로킹 JPA는 `Dispatchers.IO` 오프로딩
- **Resilience4j** (`resilience4j-kotlin`) — 서킷 브레이커 · deadline
- **Spring Data JPA + H2** (Database per Service)
- **Spring Security + JWT** (user-service 외부 API 무상태 인증), **Spring Boot Actuator + Prometheus** (관측성)

## 실행 방법

```bash
# order-service (gRPC Provider) 먼저 기동 (:9090)
./gradlew :order:bootRun

# user-service (Consumer) (:8081)
./gradlew :user:bootRun
```

- user-service → order-service gRPC 채널: 기본 `127.0.0.1:9090` (환경변수 `ORDER_SERVICE_GRPC` 로 오버라이드 — 외부화된 설정)
- gRPC 엔드포인트는 curl 대신 **grpcurl** 로 확인한다:
  ```bash
  grpcurl -plaintext -d '{"user_id":"<uuid>"}' 127.0.0.1:9090 order.OrderService/GetOrders
  ```
- **서킷 브레이커 확인**: order-service를 내린 뒤 `GET /users/me` 를 반복 호출하면, 실패가 누적되며 회로가 OPEN으로 전이되고 응답의 `ordersStatus` 가
  `UNAVAILABLE` 로 바뀌는 것을 확인할 수 있다. 이때도 프로필 응답은 200으로 유지된다.
