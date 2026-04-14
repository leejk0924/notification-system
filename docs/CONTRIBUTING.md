# Contributing Guide

해당 프로젝트에 기여할 때 따라야 할 규칙들입니다.

---

## 1. 커밋 메시지 규칙 (Conventional Commits)

### 포맷
```
<type>: <subject>

<body>
```

### 타입 (Type)

| 타입         | 설명                          | 예시                       |
|------------|-----------------------------|--------------------------|
| `Feat`     | 새 기능 추가                     | `Feat: 회원 조회 API 구현`     |
| `Fix`      | 버그 수정                       | `Fix: 요일 계산 오류 수정`       |
| `Docs`     | 문서 작성/수정 (코드 제외)            | `Docs: API 접속 링크 추가`     |
| `Config`   | 설정 파일 변경                    | `Config: Swagger 경로 설정`  |
| `Chore`    | 코드와 무관한 작업 (import 정리, 의존성) | `Chore: 불필요한 import 제거`  |
| `Refactor` | 코드 구조 개선 (동작 변경 없음)         | `Refactor: 중복 로직 추출`     |
| `Build`    | 빌드 시스템, 의존성 변경              | `Build: 의존성 추가`          |
| `Ci`       | CI/CD 설정 변경                 | `Ci: Github Actions 추가 ` |

### 규칙
- 주제(subject)는 명령형으로 작성: "추가하다" (X) → "추가" (O)
- 첫 글자는 대문자로 시작
- 마침표(.) 사용 금지
- 50글자 이내로 간결하게

### 본문(Body) 작성
```
Docs: API 명세 확인 링크 README.md에 추가

- Swagger에서 생성한 swagger-ui와 api-docs 바로가기 추가
- 로컬 환경 접속 정보 명시
```

### 복잡한 변경사항 작성 예시

여러 변경사항이 있을 때는 본문(body)에 구체적으로 나열하여 변경 내용을 명확하게 표현합니다.

#### 본문에서 사용하는 변경 키워드

| 키워드 | 의미 | 사용 예시 |
|--------|------|-----------|
| `add` | 새로운 파일/기능/의존성 추가 | `add: spring-boot-testcontainers` |
| `remove` | 파일/기능/의존성 제거 | `remove: H2 database dependency` |
| `update` | 기존 항목 수정/업데이트 | `update: application-local.yml 환경별 설정 유지` |
| `refactor` | 코드 구조 개선 (동작 변경 없음) | `refactor: 의존성 그룹화 및 주석 추가` |
| `extract` | 기존 코드에서 로직/클래스 추출 | `extract: 중복 검증 로직을 Validator로 분리` |
| `move` | 코드/파일 위치 이동 | `move: 조회 메서드들을 QueryService로 이동` |
| `rename` | 이름 변경 | `rename: MemberService → MemberCommandService` |
| `docs` | 문서화 (JavaDoc, 주석 등) | `docs: MemberService 메서드 JavaDoc 추가` |
| `deprecate` | 기능 사용 중단 표시 | `deprecate: legacy API endpoint` |
| `revert` | 이전 상태로 되돌림 | `revert: 잘못된 캐시 로직 제거` |

**의존성 변경 예시:**
```
Build: Testcontainers 기반 테스트 환경 구성

- remove: H2 database dependency
- add: spring-boot-testcontainers
- add: testcontainers-mysql 2.0.2
- add: Lombok test scope dependencies
- refactor: 의존성 그룹화 및 주석 추가
```

**코드 리팩토링 예시:**
```
Refactor: 회원 서비스 계층 구조 개선

- extract: 중복 검증 로직을 Validator로 분리
- rename: MemberService → MemberCommandService
- add: MemberQueryService 신규 생성
- move: 조회 메서드들을 QueryService로 이동
```

---

## 2. GitHub 이슈 작성

### 이슈는 "무엇을" "왜" 중심으로 작성

**제목:**
- 명확하고 간결하게 (50자 이내)
- 예: `회원 API 구현` 또는 `포스팅 검색 오류`

**본문:**
```markdown
## 개요
이 기능이 필요한 이유를 한 문장으로 설명

## 목표
이 이슈로 달성하려는 목표들

## 관련 문서/링크 (필요시 작성)
필요한 참고 자료 (Swagger 경로, 설계서 등)
```

**금지사항:**
- ❌ 파일명, 함수명 등 구현 세부사항 작성 → PR에서 작성
- ❌ "어떻게" 구현할지 → 설계 단계가 아니면 불필요

---

## 3. 코드 스타일

### 네이밍 규칙
- 변수/함수명: camelCase (영어)
- 클래스명: PascalCase (영어)
- 상수: UPPER_SNAKE_CASE (영어)
- 주석: 한국어

### 예시
```java
// 카테고리 코드별 조회
public CategoryResponse getCategoryByCode(String code) {
    final String CACHE_PREFIX = "CATEGORY_";
    // 구현...
}
```
---

## 참고
이 문서는 프로젝트 진행에 따라 지속적으로 업데이트됩니다.
규칙에 대한 제안사항은 이슈로 등록해주세요.
