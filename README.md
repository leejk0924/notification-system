# 알림 발송 시스템

수강 신청 완료, 결제 확정, 강의 시작 D-1 등 다양한 이벤트 발생 시 사용자에게 이메일 또는 인앱 알림을 발송하는 시스템입니다.

---

### 🚀 핵심 설계 요약
*   **Architecture**: 헥사고날 아키텍처 (Ports & Adapters) 기반의 외부 의존성 격리
*   **Reliability**: Outbox 패턴 및 지수 백오프 재시도 정책을 통한 발송 보장
*   **Concurrency**: `SKIP LOCKED` 기반의 다중 워커 분산 처리 및 낙관적 락을 통한 멱등성 유지
*   **Efficiency**: DB 기반 큐를 활용하여 별도 메시지 브로커 없이 비동기 발송 처리

---

## 📑 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [기술 스택](#기술-스택)
3. [패키지 구조](#패키지-구조)
4. [실행 방법](#실행-방법)
5. [API 목록 및 예시](#api-목록-및-예시)
6. [데이터 모델 설명](#데이터-모델-설명)
7. [요구사항 해석 및 가정](#요구사항-해석-및-가정)
8. [설계 결정과 이유](#설계-결정과-이유)
9. [테스트 실행 방법](#테스트-실행-방법)
10. [미구현 / 제약사항](#미구현-/-제약사항)
11. [비동기 처리 구조 및 재시도 정책 설명 문서](#비동기-처리-구조-및-재시도-정책-설명-문서)
12. [요구사항 해석 및 개선 의견](#요구사항-해석-및-개선-의견)
13. [AI 활용 범위](#ai-활용-범위)

---

## 프로젝트 개요
알림 발송은 비즈니스 트랜잭션과 분리된 **비동기 구조**로 처리됩니다. 이벤트 발생 시 알림 요청을 DB에 즉시 등록하고, 별도 워커가 이를 감지해 처리하는 **Outbox 패턴**을 기반으로 설계되었습니다.

장애 대응을 위해 **지수 백오프 기반 재시도**를 수행하며, 최종 실패 건은 `DEAD_LETTER` 상태로 보관하여 수동 처리가 가능하도록 했습니다. 중복 발송은 **멱등성 키**와 DB UNIQUE 제약으로 차단하며, 다중 인스턴스 환경에서의 경합은 **비관적 락(SKIP LOCKED)**으로 방지합니다.

## 기술 스택

| 구분 | 기술 | 버전 | 용도 |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 21 | |
| **Framework** | Spring Boot | 4.0.6 | |
| **Database** | MySQL | 8.4 | |
| **Async** | DB 기반 큐 | | `SKIP LOCKED`를 활용한 다중 워커 분산 처리 |
| **Concurrency**| JPA `@Version` | | 읽음 처리 동시성 제어 및 멱등 응답 보장 |
| **Test** | JUnit 5, Testcontainers | | 인프라 의존성 포함 통합 테스트 환경 구축 |

## 패키지 구조

헥사고날 아키텍처를 기반으로 외부 의존성(DB, 외부 API)을 `adapter` 계층으로 격리하여, 비즈니스 로직의 순수성을 유지합니다.

```
com.jk.notificationservice
├── domain                  # 순수 도메인 모델 (핵심 비즈니스 규칙)
├── application
│   ├── port                # 인/아웃바운드 포트 (인터페이스)
│   └── service             # 유스케이스 구현체 및 Facade
├── adapter
│   ├── in (Web, Event)     # 외부 요청 수신 (Controller, Listener, Scheduler)
│   └── out (Persistence)   # 외부 시스템 연동 (JPA, External API Policy)
├── config                  # 인프라 설정 및 속성 관리
└── common                  # 공통 예외 처리 및 유틸리티
```

---

## 실행 방법

### 사전 요구사항
*   Java 21
*   Docker / Docker Compose

### 로컬 실행
**1. 환경 설정**
* `src/main/resources/application-local.yml.example` 파일을 복사하여 `application-local.yml`을 생성합니다.

**2. MySQL 실행**
```bash
docker compose up -d
```

**3. 빌드 및 실행**
```bash
# 프로젝트 빌드 (테스트 제외)
./gradlew clean build -x test

# 생성된 JAR 파일 직접 실행
java -jar build/libs/notification-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 운영 환경 실행 (Docker/JAR)
운영 환경에서는 `application.yml`의 환경 변수 플레이스홀더를 통해 설정값을 주입합니다.

**1. 환경 변수 설정**
* `SPRING_DATASOURCE_URL`: DB 접속 주소
* `SPRING_DATASOURCE_USERNAME`: DB 계정
* `SPRING_DATASOURCE_PASSWORD`: DB 비밀번호

**2. 실행 예시**
```bash
java -jar notification-service.jar \
  --SPRING_DATASOURCE_URL=jdbc:mysql://prod-db:3306/notification_service \
  --SPRING_DATASOURCE_USERNAME=prod_user \
  --SPRING_DATASOURCE_PASSWORD=prod_pass
```

---

## API 목록 및 예시

아래 흐름대로 실행하면 알림 등록 → 발송 → 조회 → 읽음 처리까지 전체 동작을 확인할 수 있습니다.

### Step 1. 이벤트 발행으로 알림 등록
> 실제 서비스에서는 비즈니스 레이어가 이벤트를 발행합니다. 아래 API는 개발/테스트 목적으로만 사용합니다.
```bash
# 수강 신청 완료 알림 (IN_APP)
curl -X POST http://localhost:8080/api/dummy/enrollment-completed \
  -H "Content-Type: application/json" \
  -d '{"recipientId": 1, "enrollmentId": 100, "channel": "IN_APP"}'

# 결제 확정 알림 (EMAIL)
curl -X POST http://localhost:8080/api/dummy/payment-confirmed \
  -H "Content-Type: application/json" \
  -d '{"recipientId": 1, "paymentId": 200, "channel": "EMAIL"}'
```

### Step 2. 알림 조회
```bash
# 단건 조회
curl http://localhost:8080/api/notifications/1

# 수신자별 목록 조회 (전체)
curl http://localhost:8080/api/notifications \
  -H "X-User-Id: 1"
```

### Step 3. 읽음 처리 (IN_APP 전용)

이메일·SMS 채널은 발송 후 사용자가 실제로 읽었는지 확인할 수단이 없습니다. 읽음 처리는 앱 내에서 수신 여부를 직접 추적할 수 있는 **IN_APP 채널이고 SENT 상태인 알림만** 허용됩니다. 동시에 요청이 와도 최초 1건만 처리되고 나머지는 멱등하게 응답합니다.

```bash
curl -X PUT http://localhost:8080/api/notifications/1/read \
  -H "X-User-Id: 1"
```

### API 명세

#### `GET /api/notifications/{id}` — 알림 단건 조회

| 항목 | 내용 |
| :--- | :--- |
| Path | `id` — 알림 ID |
| 응답 | `200 OK` — NotificationResponse |
| 오류 | `404` 존재하지 않는 ID |

#### `GET /api/notifications` — 수신자별 목록 조회

| 항목 | 내용 |
| :--- | :--- |
| Header | `X-User-Id` (필수) — 수신자 ID |
| Query | `read` — `true` / `false` / 생략(전체) |
| Query | `page`, `size`, `sort` — 페이징 (기본: size=20, sort=createdAt) |
| 응답 | `200 OK` — `Page<NotificationResponse>` |

#### `PUT /api/notifications/{id}/read` — 읽음 처리

| 항목 | 내용 |
| :--- | :--- |
| Path | `id` — 알림 ID |
| Header | `X-User-Id` (필수) — 수신자 ID |
| 응답 | `200 OK` — NotificationResponse (`read: true`, `readAt` 포함) |
| 오류 | `404` 존재하지 않는 ID / `403` 다른 수신자 / `409` SENT 아닌 상태 |
| 멱등 | 이미 읽음 상태이면 save 없이 현재 상태 그대로 반환 |

#### `POST /api/dummy/enrollment-completed` — 수강 신청 완료 이벤트 발행

| 항목 | 내용 |
| :--- | :--- |
| Body | `recipientId` (필수), `enrollmentId` (필수), `channel` — `EMAIL` / `IN_APP` (필수) |
| 응답 | `202 Accepted` (응답 바디 없음) |

#### `POST /api/dummy/payment-confirmed` — 결제 확정 이벤트 발행

| 항목 | 내용 |
| :--- | :--- |
| Body | `recipientId` (필수), `paymentId` (필수), `channel` — `EMAIL` / `IN_APP` (필수) |
| 응답 | `202 Accepted` (응답 바디 없음) |

#### `POST /api/dummy/course-start-reminder` — 강의 시작 D-1 이벤트 발행

| 항목 | 내용 |
| :--- | :--- |
| Body | `recipientId` (필수), `courseId` (필수), `channel` — `EMAIL` / `IN_APP` (필수) |
| 응답 | `202 Accepted` (응답 바디 없음) |

**공통 에러 응답**

```json
{ "code": "NOT_FOUND", "message": "알림 요청을 찾을 수 없습니다. id: 99" }
```

| HTTP 상태 | code | 발생 조건 |
| :--- | :--- | :--- |
| `404` | `NOT_FOUND` | 존재하지 않는 알림 ID |
| `403` | `ACCESS_DENIED` | 다른 수신자의 알림 접근 |
| `409` | `INVALID_STATE` | SENT 아닌 상태에서 읽음 처리 시도 |

---

## 데이터 모델 설명

### 1. 테이블 구조 — `notification_requests`
| 컬럼명 | 타입 | 기본값 | 설명 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT PK | AUTO_INCREMENT | |
| `recipient_id` | BIGINT | | 수신자 사용자 ID |
| `notification_type` | VARCHAR(64) | | 알림 유형 |
| `channel` | VARCHAR(16) | | 발송 채널 (EMAIL / IN_APP) |
| `status` | VARCHAR(20) | PENDING | PENDING / PROCESSING / SENT / FAILED / DEAD_LETTER / EXPIRED |
| `idempotency_key` | VARCHAR(512) UNIQUE | | 중복 발송 방지용 멱등성 키 |
| `reference_type` | VARCHAR(64) | NULL | 이벤트 원본 유형 |
| `reference_id` | BIGINT | NULL | 이벤트 원본 ID |
| `payload` | JSON | NULL | 발송 데이터 페이로드 |
| `scheduled_at` | DATETIME | NULL | 예약 발송 시각 |
| `expire_at` | DATETIME | NULL | 알림 유효 기한 |
| `retry_count` | INT | 0 | 현재 재시도 횟수 |
| `max_retry_count` | INT | 3 | 최대 재시도 횟수 |
| `next_retry_at` | DATETIME | NULL | 다음 재시도 예정 시각 |
| `last_failure_reason` | TEXT | NULL | 마지막 실패 원인 이력 |
| `is_read` | TINYINT(1) | 0 | 읽음 여부 (IN_APP 전용) |
| `read_at` | DATETIME | NULL | 읽은 시각 |
| `sent_at` | DATETIME | NULL | 발송 완료 시각 |
| `version` | BIGINT | 0 | 낙관적 락 버전 |
| `created_at` | DATETIME | CURRENT_TIMESTAMP | |
| `updated_at` | DATETIME | CURRENT_TIMESTAMP | |

### 2. 알림 유형별 TTL

`expire_at`은 `NotificationRequest.create()` 내부에서 유형에 따라 자동 계산됩니다.

| 알림 유형 | TTL | 이유 |
| :--- | :--- | :--- |
| `ENROLLMENT_COMPLETED` (수강 신청 완료) | 24시간 | 당일 내 수신이 중요 |
| `PAYMENT_CONFIRMED` (결제 확정) | 12시간 | 결제 직후 인지가 중요 |
| `COURSE_START_REMINDER` (강의 시작 D-1) | 2시간 | 강의 시작 이후에는 의미 없음 |

### 3. 테이블 구조 — `notification_registration_failures`
알림 등록 단계(DB 저장)에서 실패한 건을 별도로 보관합니다.
| 컬럼명 | 타입 | 기본값 | 설명 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT PK | AUTO_INCREMENT | |
| `recipient_id` | BIGINT | | 수신자 사용자 ID |
| `notification_type` | VARCHAR(64) | | 알림 유형 |
| `channel` | VARCHAR(16) | | 발송 채널 |
| `failure_reason` | TEXT | NULL | 등록 실패 사유 |

### 3. 인덱스 전략 상세
| 인덱스 | 컬럼 | 용도 |
| :--- | :--- | :--- |
| `uk_idempotency_key` | `(idempotency_key)` | 중복 발송 최종 방어선. 동시 INSERT 경합 시 DB가 차단 |
| `idx_status_next_retry` | `(status, next_retry_at)` | 워커 핵심 쿼리. `SKIP LOCKED`와 연동하여 풀스캔 방지 |
| `idx_status_updated_at` | `(status, updated_at)` | PROCESSING stuck 감지 스케줄러 전용 |
| `idx_recipient_read` | `(recipient_id, is_read, status)` | 사용자 알림 목록 API 최적화 |
| `idx_status_scheduled_at` | `(status, scheduled_at)` | 예약 발송 스케줄러 전용 |
| `idx_status_expire_at` | `(status, expire_at)` | 만료 알림 처리 스케줄러 전용 |

> **✅ 인덱스 수에 대한 판단**: 인덱스가 6개로 많아 보일 수 있으나, INSERT 빈도보다 워커의 폴링 빈도가 훨씬 높으므로 풀스캔 비용을 줄이는 것이 성능상 압도적으로 유리합니다.
> *   **필수**: `uk_idempotency_key`, `idx_status_next_retry`, `idx_status_updated_at`, `idx_recipient_read`
> *   **선택**: `idx_status_scheduled_at`, `idx_status_expire_at`

---

## 요구사항 해석 및 가정

### 1. 인증/인가
*   요구사항에서 "userId를 헤더나 파라미터로 전달하는 방식도 허용"으로 명시되어 있어, `X-User-Id` 요청 헤더로 수신자를 식별하는 방식을 채택했습니다.

### 2. 비즈니스 트랜잭션 영향 분리
*   "알림 발송 실패가 비즈니스 트랜잭션에 영향을 주어서는 안 된다"는 요구사항을 **"비동기적으로 반드시 발송을 보장하되 비즈니스 로직을 방해하지 않는다"**로 해석했습니다.
*   이를 위해 **Outbox 패턴**을 채택하여 비즈니스 트랜잭션과 동일 커넥션에서 알림 요청을 DB에 먼저 기록하고, 별도 워커가 발송을 처리하도록 설계했습니다.

### 3. 메시지 브로커 없이 전환 가능한 구조
*   실제 메시지 브로커(Kafka 등) 없이 구현하되 전환이 가능해야 하므로, 발송 처리 로직을 인터페이스로 추상화하고 **DB 기반 큐(SKIP LOCKED)**를 구현체로 사용했습니다.

### 4. 처리 중(PROCESSING) 상태 복구
*   서버 크래시 등으로 인해 `PROCESSING`에서 멈춘 건을 복구하기 위해 `updated_at` 타임스탬프를 활용, 일정 시간 업데이트가 없는 행을 감지해 `PENDING`으로 복구하는 스케줄러를 구성했습니다.

### 5. 중복 발송 방지 범위
*   "동일 이벤트"를 `{notificationType}:{referenceType}:{referenceId}:{recipientId}:{channel}` 조합으로 정의하여 멱등성 키로 사용하고, DB UNIQUE 제약으로 중복 등록을 원천 차단합니다.

### 6. 선택 구현 채택 여부
| 항목 | 채택 | 내용 |
| :--- | :--- | :--- |
| **발송 스케줄링** | **O** | `scheduled_at` 필드를 통해 특정 시각 발송 예약 지원 및 워커 폴링 감지 |
| **알림 템플릿 관리** | **X** | 현재는 타입별 메시지 구성 로직 내 구현 (향후 관리 API로 고도화 예정) |
| **읽음 처리** | **O** | 다중 기기 동시 요청 시 낙관적 락(`version`)을 통해 최초 1건만 처리 및 멱등 응답 |
| **최종 실패 및 수동 재시도** | **O** | `DEAD_LETTER` 상태로 최종 실패 건 보관 및 운영자 수동 복구 경로 확보 |

---

## 설계 결정과 이유

### 1. 헥사고날 아키텍처 (Hexagonal Architecture)
*   **결정 이유**: 향후 **Kafka와 같은 메시지 브로커로의 인프라 전환 가능성**이 높다고 판단했습니다.
*   **이점**: 아웃바운드 포트(Port) 추상화를 통해 인프라가 변경되더라도 비즈니스 로직 수정 없이 어댑터만 교체하면 되는 **유연한 확장성**을 확보했습니다.

### 2. 분산 처리: SKIP LOCKED (비관적 락)
*   **결정 이유**: 낙관적 락은 충돌 시 불필요한 조회와 롤백으로 인한 **리소스 낭비(CPU/IO)**가 큽니다.
*   **이점**: `SELECT ... FOR UPDATE SKIP LOCKED`를 통해 이미 점유된 행은 즉시 건너뛰고 **자신이 처리할 데이터만 점유**하여 경합을 차단하고 처리량을 극대화했습니다.

### 3. Outbox 패턴 및 실패 분리
| 실패 유형 | 상태 | 저장 위치 | 원인 |
| :--- | :--- | :--- | :--- |
| **등록 실패** | — | `notification_registration_failures` | 이벤트 수신 후 DB 저장 단계 실패 |
| **발송 실패** | `DEAD_LETTER` | `notification_requests` | 워커가 실제 발송 시도 중 재시도 소진 |

### 4. 알림 등록·발송 트랜잭션 — Facade 패턴
*   **NotificationFacade**: 저장·조회 트랜잭션 경계를 분리합니다.
*   **이유**: `REQUIRES_NEW`는 외부 트랜잭션이 커넥션을 잡은 채 대기하게 하여 **커넥션 풀 고갈**을 유발할 수 있어 지양했습니다.

### 5. 이벤트 리스너 전략 — AFTER_COMMIT
*   알림 등록 리스너는 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 동작합니다.
*   **이유**: 비즈니스 로직 롤백 시 알림이 발행되지 않는 것이 정상이며, 알림 저장 실패가 비즈니스 트랜잭션까지 롤백시키지 않도록 설계했습니다.

### 6. 낙관적 락과 비관적 락의 용도 분리
*   상태 전이는 **비관적 락(`SKIP LOCKED`)**, 읽음 처리는 **낙관적 락(`version`)**으로 처리하여 데드락 위험을 방지하고 안정성을 높였습니다.

---

## 테스트 실행 방법
본 프로젝트는 **Testcontainers**를 사용하여 실제 DB 환경과 유사한 MySQL 컨테이너를 구동합니다. 따라서 로컬 환경에 **Docker**가 실행 중이어야 합니다.

```bash
# 전체 테스트 실행
./gradlew test
```

---

## 미구현 / 제약사항

### 1. 알림 템플릿 CRUD API
*   **개요**: 알림 타입별 메시지 템플릿을 배포 없이 관리할 수 있는 API 구현.
*   **목표**: 타입/채널별 조회·수정 기능 제공 및 템플릿 부재 시 기본 메시지 Fallback 정책 적용.

### 2. DEAD_LETTER 수동 재시도
*   **개요**: 최종 실패 상태의 알림을 수동으로 재시도할 수 있는 기능 구현.
*   **목표**: 재시도 시 횟수 초기화 여부 선택 옵션 제공 및 장애 상황별 재시도 정책 명확화.

### 3. 예약 발송 API
*   **개요**: 특정 시간에 알림을 발송할 수 있도록 예약 발송 요청을 처리하는 기능 구현.
*   **목표**: `scheduledAt` 필드 수신 시 예약 상태 등록 및 시간에 근접한 알림을 감지하는 스케줄러 구현.

---

## 비동기 처리 구조 및 재시도 정책 설명 문서

### 알림 등록 흐름 (현재 구현)

```
비즈니스 이벤트 발생 (수강 신청, 결제 등)
    │
    ▼
ApplicationEventPublisher.publishEvent(NotificationEvent)
    │
    ▼ @TransactionalEventListener(AFTER_COMMIT) + @Async NotificationEventListener.handle()
    │
    ├─ NotificationFacade.save()  @Transactional
    │       └─ 성공 → notification_requests (PENDING)
    │
    └─ 실패 시 DeadLetterPort.save()  @Transactional
                └─ notification_registration_failures
```

비즈니스 트랜잭션 커밋 이후 별도 스레드에서 비동기 처리되므로 알림 등록 실패가 비즈니스 트랜잭션에 영향을 주지 않는다.

### 발송 처리 흐름

```
[ExpiredNotificationScheduler] 주기적 실행
    │
    └─ expire_at < now 인 PENDING 건 일괄 ──▶ 상태 → EXPIRED
         (만료 처리의 주 책임)

이벤트 발생
    │
    ▼
알림 요청 DB 등록 (PENDING) ─── 비즈니스 트랜잭션과 동일 커넥션
    │
    ▼
NotificationFacade.claimPendingForDispatch()  @Transactional  ← 하나의 트랜잭션
    │
    ├─ SELECT ... FOR UPDATE SKIP LOCKED  ← PENDING 행 점유 (다른 워커 접근 차단)
    │
    ├─ expire_at 초과 ──▶ 상태 → EXPIRED, 저장 후 목록에서 제외 (안전망)
    │
    └─ 정상 ──▶ 상태 → PROCESSING, 저장 후 반환
    │           (SELECT 락과 PROCESSING 저장이 같은 트랜잭션 — 점유 직후 상태 전이)
    │
    ▼
dispatchSingle() — PROCESSING 상태 건만 처리
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

기본 최대 3회 재시도. 각 대기 시간에 0~9초 Jitter가 추가된다.

수강 신청·결제 직후 알림을 기대하는 사용자가 수분 내로 앱을 이탈할 수 있으므로,
재시도 간격을 길게 가져가면 TTL(`expire_at`) 초과로 EXPIRED 처리될 가능성이 높다.
빠른 재시도로 TTL 내 발송 성공률을 높이는 것을 우선한다.

```
next_retry_at = now + min(2^retryCount * 60초, 1800초) + random(0, 9초)
```

| 시도 | 기본 대기 시간 | 상한 |
|------|------------|------|
| 1회  | 1분        | 30분 |
| 2회  | 2분        | 30분 |
| 3회  | 4분        | 30분 |
| 최종 실패 | DEAD_LETTER 보관 | — |

> `expire_at` 초과 시 남은 재시도 횟수와 무관하게 EXPIRED 처리

### 중복 처리 방지 (SKIP LOCKED)
다중 인스턴스 환경에서 여러 워커가 동시에 폴링해도 `SKIP LOCKED`로 각자 다른 행을 가져간다.
이미 처리 중인 행은 건너뛰어 중복 발송을 방지한다.

`SELECT ... FOR UPDATE SKIP LOCKED`와 `PROCESSING` 상태 저장은 `claimPendingForDispatch()` 하나의 `@Transactional` 안에서 원자적으로 처리된다.
SELECT로 행을 점유한 직후 같은 트랜잭션 내에서 PROCESSING으로 전이하고 저장한 뒤 커밋한다.
SELECT와 상태 전이를 별도 트랜잭션으로 나누면, SELECT 커밋 시점에 락이 풀려 다른 인스턴스가 같은 행을 중복으로 점유할 수 있다.

### PROCESSING stuck 복구 및 서버 재시작 대응

```
[ProcessingStuckRecoveryScheduler] 주기적 실행 (stuckRecoveryDelay 간격)
    │
    └─ status = PROCESSING AND updated_at < now - stuckThreshold 인 건 조회
           │
           └─ recoverFromStuck() → 상태 → PENDING, nextRetryAt 초기화
                  (retryCount 유지 — stuck은 발송 실패가 아님)
```

서버 재시작 후 `PROCESSING`으로 남은 건도 `stuckThreshold`(기본 5분) 경과 후 동일하게 복구된다.
다중 인스턴스 환경에서 여러 워커가 동시에 복구를 시도해도 결과는 동일하므로 멱등성이 보장된다.

---

## 요구사항 해석 및 개선 의견

### 1. 해석 및 구현 전략
*   **유연한 채널 확장**: `ChannelSender` 인터페이스를 통해 향후 SMS, Push 알림 등이 추가되더라도 기존 로직 수정 없이 확장이 가능합니다.
*   **브로커 전환**: 아웃바운드 포트 추상화를 통해 향후 비즈니스 로직 변경 없이 Kafka 등으로 전환할 수 있는 구조를 갖췄습니다.

### 2. 성능 개선에 대한 고찰 (아쉬운 점 및 해결 방안)
*   **벌크 업데이트 제약**: 알림 발송 워커에서 대량의 알림 상태를 한 번에 변경할 때 `saveAll`을 통한 배치 처리를 시도했으나, **낙관적 락(`@Version`)**으로 인해 Hibernate가 엔티티별로 개별 `UPDATE`를 수행하는 것을 확인했습니다. 데이터 정합성을 위해 낙관적 락을 유지해야 했기에 발생한 DB 네트워크 왕복 비용(Round-trip)이 성능상의 아쉬움으로 남았습니다.
*   **기술적 대안 제안**:
    *   이를 해결하기 위해 `JdbcTemplate`을 활용한 **JDBC Batch Update**를 도입하거나, Native SQL의 `@Modifying` 쿼리를 사용하여 `WHERE id IN (...)` 조건으로 버전 필드를 수동 업데이트하는 방식을 도입하면 성능을 대폭 개선할 수 있을 것으로 판단됩니다.
    *   또한, 이미 `SKIP LOCKED`로 물리적 점유를 확보한 상태(PENDING -> PROCESSING)에서는 낙관적 락 체크를 선택적으로 생략하여 영속성 컨텍스트의 오버헤드를 줄이는 최적화 방안을 고려 중입니다.

---

## AI 활용 범위
설계와 구현은 직접 진행하였으며, AI는 아래 용도로만 보조적으로 활용하였습니다.

*   **문서 첨삭**: README 문장 표현 및 전체적인 구성 검토
*   **엣지 케이스 확인**: 설계 과정에서 놓칠 수 있는 예외 상황(데드락, 중복 발송, stuck 감지 등) 점검
*   **설계 조언**: 재시도 횟수, TTL, 락 전략 등 결정 시 일반적인 업계 사례 참고
*   **커밋 메시지 추천**: 프로젝트 컨벤션(`CONTRIBUTING.md`)을 기준으로 커밋 메시지 초안 추천 (최종 내용은 직접 검토 후 작성)
*   **코드 참고**: 보일러플레이트, 테스트 코드 골격 등 반복적인 코드 작성 시 초안 참고 (로직 구현 및 검증은 직접 수행)

---
*본 프로젝트는 요구사항에 따라 실제 메시지 브로커 없이 구현되었으나, 향후 Kafka 등으로 전환이 용이하도록 설계되었습니다.*
