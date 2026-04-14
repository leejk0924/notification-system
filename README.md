# 알림 발송 시스템
수강 신청 완료, 결제 확정, 강의 시작 D-1 등 다양한 이벤트 발생 시 사용자에게 이메일 또는 인앱 알림을 발송하는 시스템이다.

## 프로젝트 개요
알림 발송은 비즈니스 트랜잭션과 분리된 비동기 구조로 처리된다. 이벤트 발생 시 알림 요청을 즉시 DB에 등록하고, 별도 워커가 이를 감지해 실제 발송을 처리하는 Outbox 패턴 기반으로 설계 되었다.

네트워크 장애나 외부 서버 오류 같은 일시적 장애에는 지수 백오프 기반의 재시도 정책으로 대응하며, 최종 실패 건은 DEAD_LETTER 상태로 보관해 수동 재처리하도록 하였다.

동일 이벤트에 대한 중복 발송은 멱등성 키와 DB UNIQUE 제약으로 차단하고, 다중 인스턴스 환경에서의 중복 처리는 비관적 락으로 방지한다.
 
## 기술 스택

| 구분        | 기술                               | 버전    |
|------------|----------------------------------|--------|
| Language   | Java                             | 21     |
| Framework  | Spring Boot                      | 4.0.6  |
| ORM        | Spring Data JPA / Hibernate      |        |
| Database   | MySQL                            | 8.4    |
| 비동기 처리  | DB 기반 큐 (SKIP LOCKED)           |        |
| 테스트      | JUnit 5, Testcontainers, AssertJ |        |
| 인프라      | Docker / Docker Compose          |        |

> 메시지 브로커(Kafka 등) 도입은 추후 계획

## 패키지 구조

헥사고날 아키텍처(Ports & Adapters)를 기반으로 구성했다.
메시지 브로커가 확정되지 않은 상황에서 `application` 레이어를 건드리지 않고 어댑터만 교체할 수 있도록 포트를 통해 외부 의존을 격리했다.

```
com.jk.notificationservice
│
├── domain                                        # 순수 도메인 (외부 의존 없음)
│   ├── NotificationRequest.java                  # 도메인 모델
│   ├── NotificationStatus.java                   # Enum
│   └── NotificationChannel.java                  # Enum
│
├── application                                   # 유스케이스 & 포트 정의
│   ├── port
│   │   ├── in                                   # 인바운드 포트 (유스케이스 인터페이스)
│   │   │   ├── RegisterNotificationUseCase.java
│   │   │   ├── QueryNotificationUseCase.java
│   │   │   └── ProcessNotificationUseCase.java
│   │   └── out                                  # 아웃바운드 포트
│   │       ├── NotificationRepository.java
│   │       └── NotificationSender.java          # 채널/브로커 추상화 핵심 포인트
│   └── service
│       ├── NotificationCommandService.java       # RegisterNotificationUseCase 구현
│       └── NotificationQueryService.java         # QueryNotificationUseCase 구현
│
└── adapter
    ├── in                                        # 인바운드 어댑터 (외부 → 앱)
    │   ├── web
    │   │   ├── NotificationController.java
    │   │   └── dto
    │   │       ├── NotificationCreateRequest.java
    │   │       └── NotificationResponse.java
    │   ├── worker
    │   │   └── NotificationWorker.java           # DB 폴링 & 발송 처리 (SKIP LOCKED)
    │   └── scheduler
    │       ├── StuckRecoveryScheduler.java        # PROCESSING stuck 복구
    │       ├── ScheduledNotificationScheduler.java # 예약 발송
    │       └── ExpiredNotificationScheduler.java  # 만료 알림 EXPIRED 처리
    └── out                                       # 아웃바운드 어댑터 (앱 → 외부)
        ├── persistence
        │   ├── NotificationRequestEntity.java    # JPA Entity
        │   ├── NotificationRequestJpaRepository.java
        │   └── NotificationPersistenceAdapter.java  # NotificationRepository 구현
        └── sender
            ├── EmailSender.java                  # NotificationSender 구현체 (Mock/로그)
            └── InAppSender.java                  # NotificationSender 구현체
```

**메시지 브로커 전환 시 변경 범위**

```
# 현재 (DB 기반 큐)
adapter/in/worker/NotificationWorker.java         ← DB 폴링
adapter/out/sender/EmailSender.java               ← 직접 발송

# Kafka 전환 시 추가/교체
adapter/in/messaging/NotificationConsumer.java    ← Kafka Consumer
adapter/out/messaging/NotificationProducer.java   ← Kafka Producer
```

`domain` / `application` 레이어는 변경 없음

## 실행 방법

### 사전 요구사항
- Java 21
- Docker / Docker Compose

### 로컬 실행

**1. MySQL 실행**
```bash
docker compose up -d
```

**2. 애플리케이션 실행**
```bash
./gradlew clean build
 
java -jar build/libs/notification-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## API 목록 및 예시

> 구현 완료 후 작성 예정

## 데이터 모델 설명

### 테이블 구조 — notification_requests

| 컬럼명 | 타입 | 기본값 | 설명 |
|--------|------|--------|------|
| id | BIGINT PK | AUTO_INCREMENT | |
| recipient_id | BIGINT | | 수신자 사용자 ID |
| notification_type | VARCHAR(64) | | 알림 유형 (ENROLLMENT_COMPLETED 등) |
| channel | VARCHAR(16) | | 발송 채널 (EMAIL / IN_APP) |
| status | VARCHAR(20) | PENDING | PENDING / PROCESSING / SENT / FAILED / DEAD_LETTER / EXPIRED (아래 상태 전이 참고) |
| idempotency_key | VARCHAR(512) UNIQUE | | 중복 발송 방지용 멱등성 키 (`{type}:{refType}:{refId}:{recipientId}:{channel}` 형식) |
| reference_type | VARCHAR(64) | NULL | 이벤트 원본 엔티티 유형 (ENROLLMENT 등) |
| reference_id | BIGINT | NULL | 이벤트 원본 엔티티 ID |
| payload | JSON | NULL | 발송 시점에 원본 데이터 조회 후 저장 |
| scheduled_at | DATETIME | NULL | 예약 발송 시각 (NULL이면 즉시 발송) |
| expire_at | DATETIME | NULL | 알림 유효 기한 (초과 시 EXPIRED 처리) |
| retry_count | INT | 0 | 현재 재시도 횟수 |
| max_retry_count | INT | 3 | 최대 재시도 횟수 (유형별 조정 가능) |
| next_retry_at | DATETIME | NULL | 다음 재시도 예정 시각 (지수 백오프 + Jitter) |
| failure_reason | TEXT | NULL | 실패 원인 (디버깅용) |
| is_read | TINYINT(1) | 0 | 읽음 여부 — IN_APP 전용. EMAIL 채널은 애플리케이션 레벨에서 업데이트하지 않음 |
| read_at | DATETIME | NULL | 읽은 시각 — IN_APP 전용 |
| version | BIGINT | 0 | 낙관적 락 — 읽음 처리 전용 (상태 전이는 SKIP LOCKED로 분리) |
| created_at | DATETIME | CURRENT_TIMESTAMP | |
| updated_at | DATETIME | CURRENT_TIMESTAMP ON UPDATE | PROCESSING stuck 감지 스케줄러가 이 컬럼만으로 N분 이상 PROCESSING 여부 판단 |

### 알림 유형별 TTL

| 알림 유형 | TTL | 이유 |
|-----------|-----|------|
| 강의 시작 D-1 | 2시간 | 강의 시작 이후에는 의미 없음 |
| 수강 신청 완료 | 24시간 | 당일 내 수신이 중요 |
| 결제 확정 | 12시간 | 결제 직후 인지가 중요 |

### 인덱스 전략

| 인덱스 | 컬럼 | 용도 |
|--------|------|------|
| `uk_idempotency_key` | `(idempotency_key)` | 중복 발송 최종 방어선. 동시 INSERT 경합 시 DB 차단 |
| `idx_status_next_retry` | `(status, next_retry_at)` | 워커 PENDING 건 폴링. SKIP LOCKED 쿼리에서 직접 사용 |
| `idx_status_updated_at` | `(status, updated_at)` | PROCESSING stuck 감지 스케줄러 전용 |
| `idx_recipient_read` | `(recipient_id, is_read, status)` | 사용자 알림 목록 API. 수신자 + 읽음 + 상태 필터 |
| `idx_status_scheduled_at` | `(status, scheduled_at)` | 예약 발송 스케줄러 전용 |
| `idx_status_expire_at` | `(status, expire_at)` | 만료 알림 처리 스케줄러 전용 |

## 요구사항 해석 및 가정

### 인증/인가
요구사항에서 "userId를 헤더나 파라미터로 전달하는 방식도 허용"으로 명시되어 있어, `X-User-Id` 요청 헤더로 수신자를 식별하는 방식을 채택했다.

### "비즈니스 트랜잭션에 영향을 주어서는 안 된다"
알림 발송 실패가 수강 신청이나 결제 트랜잭션을 롤백시켜서는 안 된다는 의미로 해석했다.
단순히 예외를 무시(try-catch 후 로그)하는 방식은 알림 유실을 허용하므로 요구사항 위반으로 판단했다.
이를 해결하기 위해 Outbox 패턴을 채택했다 — 비즈니스 트랜잭션과 동일한 커넥션에서 알림 요청을 DB에 등록하고, 별도 워커가 발송을 처리한다.

### "실제 메시지 브로커 없이 구현하되, 전환 가능한 구조"
발송 처리 로직을 인터페이스로 추상화하고, DB 기반 큐를 구현체로 사용했다.
Kafka 등 메시지 브로커 도입 시 구현체만 교체하면 된다.

### "처리 중 상태가 일정 시간 이상 지속되는 경우 복구"
`updated_at ON UPDATE CURRENT_TIMESTAMP`를 활용해 별도 타임스탬프 컬럼 없이 `status = 'PROCESSING' AND updated_at < NOW() - INTERVAL N MINUTE` 조건으로 stuck 행을 감지하고 `PENDING`으로 복구하는 스케줄러를 구성했다.

### 중복 발송 방지 범위
"동일 이벤트"를 `{notificationType}:{referenceType}:{referenceId}:{recipientId}:{channel}` 조합으로 정의했다.
이를 `idempotency_key`로 사용하고 DB UNIQUE 제약을 걸어 동시 요청이 들어와도 한 건만 등록되도록 했다.

### 선택 구현 채택 여부
| 항목                | 채택 | 내용                                              |
|-------------------|----|-------------------------------------------------|
| 발송 스케줄링           | O  | `scheduled_at` 컬럼으로 예약 발송 지원                    |
| 읽음 처리 (다중 기기)     | O  | 낙관적 락(`version`)으로 동시 읽음 처리 시 충돌 방지             |
| 최종 실패 보관 및 수동 재시도 | O  | `DEAD_LETTER` 상태 보관, 수동 재시도 시 `retry_count` 초기화 |
| 알림 템플릿 관리         | X  | 미구현 — 미구현/제약사항 참고                               |

## 설계 결정과 이유

### Outbox 패턴 채택
알림 발송을 비즈니스 트랜잭션과 같은 커넥션에서 DB에 먼저 기록한 뒤 워커가 발송하는 구조를 택했다.
트랜잭션 커밋 직후 네트워크 장애가 발생하더라도 알림 유실 없이 재처리할 수 있다.

### DB 기반 큐 (SKIP LOCKED)
초기 트래픽 규모에서는 메시지 브로커 없이 MySQL만으로 워커 간 중복 처리를 방지할 수 있다.
`SELECT ... FOR UPDATE SKIP LOCKED`로 각 워커가 서로 다른 행을 점유해 동시성을 확보한다.
트래픽이 증가하면 메시지 브로커로 전환할 수 있도록 발송 처리 로직을 인터페이스로 분리해 둔다.

### 지수 백오프 + Jitter 재시도
네트워크 장애나 외부 서버 일시 오류는 즉시 재시도보다 간격을 늘려가며 재시도하는 것이 효과적이다.
다중 인스턴스 환경에서 같은 시각에 실패한 알림들이 동시에 재시도되어 외부 서비스에 부하가 몰리는 것을 막기 위해 Jitter(무작위 편차)를 추가한다.

```
next_retry_at = now + (baseDelay * 2^retryCount) + random(0, 30초)
```

최종 실패 건은 `DEAD_LETTER` 상태로 보관해 수동 재처리 경로를 확보한다.
`expire_at`을 초과한 알림은 재시도 없이 `EXPIRED` 처리한다.

### 멱등성 키 + UNIQUE 제약
동일 이벤트가 중복 인입되더라도 DB UNIQUE 제약으로 중복 발송을 원천 차단한다.

멱등성 키 형식은 `{notificationType}:{referenceType}:{referenceId}:{recipientId}:{channel}`이며, 현재 최대 수십 자 수준이다.
향후 복합 이벤트나 외부 UUID가 포함될 경우를 대비해 VARCHAR(512)로 여유를 두었다. UNIQUE 인덱스가 하나이므로 인덱스 비용은 허용 범위다.

### 낙관적 락과 비관적 락의 용도 분리
상태 전이(PENDING → PROCESSING 등)는 `FOR UPDATE SKIP LOCKED`(비관적 락)로 처리하고,
읽음 처리(`is_read` 업데이트)는 `version`(낙관적 락)으로 처리한다.
두 락을 같은 경로에서 혼용하면 데드락 위험이 있으므로 용도를 명확히 분리했다.

### updated_at으로 PROCESSING stuck 감지
`updated_at`에 `ON UPDATE CURRENT_TIMESTAMP`를 적용하면 별도 타임스탬프 컬럼 없이
스케줄러가 `status = 'PROCESSING' AND updated_at < NOW() - INTERVAL N MINUTE` 조건만으로
오래 멈춰있는 행을 감지해 PENDING으로 복구할 수 있다.

### 인덱스 전략

각 인덱스는 특정 쿼리 패턴 하나에만 대응하도록 설계했다.

| 인덱스 | 컬럼 | 용도 |
|--------|------|------|
| `uk_idempotency_key` | `(idempotency_key)` | 중복 발송 최종 방어선. 동시 INSERT 경합 시 DB가 차단 → `DataIntegrityViolationException` 처리 |
| `idx_status_next_retry` | `(status, next_retry_at)` | 워커 핵심 쿼리. PENDING + next_retry_at <= now 조건 풀스캔 방지. SKIP LOCKED 쿼리에서 직접 사용 |
| `idx_status_updated_at` | `(status, updated_at)` | PROCESSING stuck 감지 스케줄러 전용. status 없이 updated_at만 있으면 전체 테이블 스캔 |
| `idx_recipient_read` | `(recipient_id, is_read, status)` | 사용자 알림 목록 API. 수신자 + 읽음 + 상태 필터 조합이 가장 빈번한 쿼리 패턴 |
| `idx_status_scheduled_at` | `(status, scheduled_at)` | 예약 발송 스케줄러 전용. 매 분 실행되므로 인덱스 없이는 풀스캔 반복 |
| `idx_status_expire_at` | `(status, expire_at)` | 만료 알림 처리 스케줄러 전용. expire_at 초과 건 빠른 탐색 |

**인덱스 수에 대한 판단**

인덱스가 6개로 많아 보일 수 있으나, 이 서비스의 쓰기 빈도는 비즈니스 이벤트(수강 신청, 결제 등) 발생 시로 한정되어 있어 INSERT 비용 증가는 허용 범위다.
반면 워커는 초 단위로 폴링하므로, 인덱스 없이 풀스캔이 반복되는 비용이 훨씬 크다.

우선순위를 구분하면 다음과 같다:
- **필수**: `uk_idempotency_key`, `idx_status_next_retry`, `idx_status_updated_at`, `idx_recipient_read` — 정확성 또는 빈번한 쿼리에 직결
- **선택**: `idx_status_scheduled_at`, `idx_status_expire_at` — 분 단위 스케줄러용. 데이터가 쌓인 뒤 EXPLAIN으로 실행 계획을 확인하고 불필요하면 제거

## 테스트 실행 방법

> Testcontainers를 사용하므로 Docker가 실행 중이어야 합니다.

```bash
./gradlew test
```

## 비동기 처리 구조 및 재시도 정책

### 처리 흐름

```
이벤트 발생
    │
    ▼
알림 요청 DB 등록 (PENDING) ─── 비즈니스 트랜잭션과 동일 커넥션
    │
    ▼
워커 폴링 (SKIP LOCKED)
    │
    ├─ expire_at 초과 ──▶ 상태 → EXPIRED
    │
    ▼
상태 → PROCESSING
    │
    ├─ 성공 ──▶ 상태 → SENT
    │
    ├─ 영구 실패 ──▶ 상태 → FAILED
    │               (잘못된 수신자, 페이로드 오류 등 재시도해도 동일하게 실패하는 케이스)
    │
    └─ 일시 실패 ──▶ retry_count 증가
                        │
                        ├─ 재시도 가능 ──▶ 상태 → PENDING, next_retry_at = 지수 백오프 + Jitter
                        │
                        └─ 최대 재시도 초과 ──▶ 상태 → DEAD_LETTER
                                               (외부 서비스 복구 후 수동 재시도 대상)
```

| 상태 | 설명 |
|------|------|
| `PENDING` | 발송 대기 중 |
| `PROCESSING` | 워커가 처리 중 |
| `SENT` | 발송 완료 |
| `FAILED` | 영구 실패 — 재시도 불가 (잘못된 수신자, 페이로드 오류 등) |
| `DEAD_LETTER` | 재시도 횟수 소진 — 외부 서비스 복구 후 수동 재시도 가능 |
| `EXPIRED` | 유효 기한 초과 — 발송 의미 없음 |

### 재시도 정책 (지수 백오프 + Jitter)

최대 5회 재시도. 각 대기 시간에 0~30초 Jitter가 추가된다.

| 시도 | 기본 대기 시간 | 누적 |
|------|------------|------|
| 1회  | 1분        | ~1분  |
| 2회  | 5분        | ~6분  |
| 3회  | 10분       | ~16분 |
| 4회  | 30분       | ~46분 |
| 5회  | 60분       | ~106분 |
| 최종 실패 | DEAD_LETTER 보관 | — |

> `expire_at` 초과 시 남은 재시도 횟수와 무관하게 EXPIRED 처리

### 중복 처리 방지 (SKIP LOCKED)
다중 인스턴스 환경에서 여러 워커가 동시에 폴링해도 `SKIP LOCKED`로 각자 다른 행을 가져간다.
이미 처리 중인 행은 건너뛰어 중복 발송을 방지한다.

### 서버 재시작 대응
서버 재시작 시 `PROCESSING` 상태로 남아있는 항목은 일정 시간이 지나면 `PENDING`으로 복구해 재처리한다.

## 미구현 / 제약사항

> 구현 진행 중 파악되는 내용 작성 예정

## AI 활용 범위

설계와 구현은 직접 진행하였으며, AI는 아래 용도로만 보조적으로 활용하였다.

- **문서 첨삭**: README 문장 표현 및 구성 검토
- **엣지 케이스 확인**: 설계 과정에서 놓칠 수 있는 예외 상황(데드락, 중복 발송, stuck 감지 등) 점검
- **설계 조언**: 재시도 횟수, TTL, 락 전략 등 결정 시 일반적인 업계 사례 참고
- **커밋 메시지 추천**: 프로젝트 컨벤션(CONTRIBUTING.md)을 기준으로 커밋 메시지 초안 추천, 최종 내용은 직접 검토 후 작성
- **코드 참고**: 보일러플레이트, 테스트 코드 골격 등 반복적인 코드 작성 시 초안 참고, 로직과 검증은 직접 수행

## 요구사항 해석 및 개선 의견

### 알림 템플릿 관리
현재는 `notification_type`과 `payload`를 기반으로 발송 시점에 메시지를 구성한다.
템플릿 테이블을 별도로 두고 타입별 메시지 형식을 관리하면 코드 변경 없이 메시지 수정이 가능해진다.

### 메시지 브로커 전환
현재 DB 기반 큐는 단일 DB에 대한 폴링 부하가 있다.
트래픽이 증가하면 Kafka 같은 메시지 브로커로 전환해 처리량을 높일 수 있다.
발송 로직을 인터페이스로 분리하여 전환 시 비즈니스 로직 변경을 최소화하는 것이 좋아보인다.

### DEAD_LETTER 모니터링
현재 최종 실패 건은 `DEAD_LETTER` 상태로 보관만 한다.
운영 환경에서는 DEAD_LETTER 누적 시 관리자에게 알림(Slack, 이메일 등)을 발송하거나  별도 대시보드에서 수동 재처리할 수 있는 인터페이스가 필요하다.

### 채널 추상화 확장
현재 EMAIL과 IN_APP 두 채널을 지원한다.
채널별 발송기를 인터페이스로 추상화하여 SMS, 푸시 알림 등 채널 추가 시 구현체만 추가하면 되도록 하는 것이 좋아보인다.
