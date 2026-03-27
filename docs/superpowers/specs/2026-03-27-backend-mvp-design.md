# Fleta's Closet — Backend MVP 설계

**날짜:** 2026-03-27
**범위:** Spring Boot 백엔드 MVP (Auth + Closet CRUD)

---

## 1. 배경 및 목적

Fleta's Closet은 개인 맞춤형 옷장 관리 및 착장 추천 앱이다. 이 문서는 Flutter 프론트엔드와 연동될 Spring Boot 백엔드 MVP의 설계를 정의한다.

MVP 범위는 두 가지 핵심 기능으로 한정한다:
- **Auth:** 회원가입, 로그인, JWT 토큰 관리
- **Closet CRUD:** 의류 아이템 등록/조회/수정/삭제 및 이미지 관리

개발 원칙: 아키텍처·언어·테스트 기법을 학습하며 진행. 세부 Task로 쪼개고 구현 완료 후 반드시 리뷰.

---

## 2. 아키텍처

### 전체 시스템 흐름

```
Flutter App (iOS/Web) → Spring Boot API (:8080) → PostgreSQL (:5432)
                                                 → Local File Storage (이미지)
```

### 프로젝트 구조: 모듈형 모놀리스 (Domain-first)

도메인별로 독립적인 레이어를 가지는 패키지 구조. 나중에 도메인 추가(wishlist, recommendation)가 용이하고, 마이크로서비스 분리도 고려 가능.

```
com.fleta.closet/
├── auth/
│   ├── controller/   AuthController
│   ├── service/      AuthService
│   ├── repository/   UserRepository
│   └── domain/       User, LoginRequest, SignupRequest, TokenResponse
├── closet/
│   ├── controller/   ClothingController
│   ├── service/      ClothingService
│   ├── repository/   ClothingRepository
│   └── domain/       ClothingItem, ClothingRequest, ClothingResponse, Category(enum)
└── common/
    ├── config/       SecurityConfig, JwtConfig
    ├── exception/    GlobalExceptionHandler, ErrorResponse
    └── security/     JwtTokenProvider, JwtAuthFilter
```

### Tech Stack

| 구분 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6 + JWT (jjwt) |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL 16 |
| Build | Gradle |
| DB Migration | Flyway |
| Test | JUnit 5 + Mockito + Testcontainers |
| Container | Docker Compose |

---

## 3. 데이터 모델 (ERD)

### users

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGSERIAL | PK |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt) |
| nickname | VARCHAR(50) | NOT NULL |
| refresh_token | VARCHAR(500) | nullable |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### clothing_items

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK → users(id), NOT NULL |
| category | VARCHAR(50) | NOT NULL (enum) |
| brand | VARCHAR(100) | nullable |
| color | VARCHAR(50) | nullable |
| tags | VARCHAR[] | nullable (PostgreSQL 배열) |
| image_path | VARCHAR(500) | nullable |
| memo | TEXT | nullable |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### Category Enum

`TOPS` | `BOTTOMS` | `OUTERWEAR` | `SHOES` | `ACCESSORIES` | `BAGS`

### 관계
- users : clothing_items = 1 : N

---

## 4. API 명세

모든 인증 필요 API는 `Authorization: Bearer <accessToken>` 헤더를 요구한다.

### Auth API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | /api/auth/signup | 회원가입 | ❌ |
| POST | /api/auth/login | 로그인 → accessToken + refreshToken | ❌ |
| POST | /api/auth/refresh | accessToken 재발급 | ❌ |
| POST | /api/auth/logout | 로그아웃 (refreshToken 무효화) | ✅ |
| GET | /api/auth/me | 내 프로필 조회 | ✅ |

### Closet API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | /api/closet/items | 내 의류 목록 조회 (?category= 필터) | ✅ |
| GET | /api/closet/items/{id} | 의류 단건 조회 | ✅ |
| POST | /api/closet/items | 의류 등록 (multipart: image + JSON) | ✅ |
| PUT | /api/closet/items/{id} | 의류 수정 (이미지 선택적 업데이트) | ✅ |
| DELETE | /api/closet/items/{id} | 의류 삭제 (이미지 파일도 삭제) | ✅ |
| GET | /api/closet/items/{id}/image | 의류 이미지 파일 반환 | ✅ |

### 공통 에러 응답

```json
{
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "이메일 형식이 올바르지 않습니다",
  "timestamp": "2026-03-27T10:00:00"
}
```

---

## 5. JWT 전략

- **Access Token:** 15분 만료, `Authorization: Bearer` 헤더로 전달
- **Refresh Token:** 7일 만료, DB(users.refresh_token)에 저장
- **Refresh Token Rotation:** 재발급 시 기존 토큰 무효화 후 새 토큰 발급 (재사용 방지)
- 로그아웃 시 DB의 refresh_token을 null로 설정

---

## 6. 에러 처리

`GlobalExceptionHandler`가 모든 예외를 일관된 `ErrorResponse` 형식으로 변환.

주요 에러 코드:
- `DUPLICATE_EMAIL` (409) — 중복 이메일 가입 시도
- `INVALID_CREDENTIALS` (401) — 로그인 실패
- `EXPIRED_TOKEN` (401) — 만료된 토큰
- `INVALID_TOKEN` (401) — 유효하지 않은 토큰
- `FORBIDDEN` (403) — 타인의 의류 접근 시도
- `CLOTHING_NOT_FOUND` (404) — 존재하지 않는 의류
- `INVALID_INPUT` (400) — 입력값 검증 실패

---

## 7. 테스트 전략 (피라미드)

**단위 테스트 (JUnit 5 + Mockito)**
- `AuthService`, `ClothingService` 비즈니스 로직
- 의존성은 Mockito로 목킹
- 빠른 피드백, 커버리지 높음

**통합 테스트 (Testcontainers)**
- 핵심 흐름: 회원가입→로그인→의류등록→조회
- 실제 PostgreSQL 컨테이너 사용 (목킹 없음)
- `@SpringBootTest` + `@AutoConfigureMockMvc`

---

## 8. 이미지 저장 전략

- 로컬 파일 시스템에 저장 (`uploads/clothing/{userId}/{uuid}.jpg`)
- API는 이미지 조회 경로를 반환 (`/api/closet/items/{id}/image`)
- 향후 S3 등 클라우드 스토리지로 마이그레이션 고려

---

## 9. 검증 방법

1. Docker Compose로 PostgreSQL 실행 후 Spring Boot 앱 기동
2. Postman 또는 cURL로 회원가입 → 로그인 → 의류 CRUD 흐름 검증
3. `./gradlew test`로 단위 + 통합 테스트 전체 실행
4. Flyway 마이그레이션 정상 적용 확인
