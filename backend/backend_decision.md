# Backend Architecture Decisions

---

## [2026-03-28] JwtAuthFilter: 단일 parseToken() 호출로 이중 파싱 제거

### 배경

`JwtAuthFilter`에서 JWT 검증 시 `JwtTokenProvider`의 `isAccessToken()`과 `getUserId()`를 각각 호출하면, 내부에서 `parseToken()`이 2회 실행된다. `parseToken()`은 매 호출마다 HMAC-SHA256 서명 검증 + JWT 디코딩을 수행하므로, 인증이 필요한 모든 요청에서 불필요한 암호화 연산이 1회 추가된다.

```java
// 수정 전 — parseToken() 2회 호출 (서명 검증 2회)
if (!jwtTokenProvider.isAccessToken(token)) throw AppException.invalidToken();
Long userId = jwtTokenProvider.getUserId(token);
```

### 결정

`JwtAuthFilter.doFilterInternal()`에서 `parseToken()`을 1회 호출해 `Claims` 객체를 받아두고, 이후 type 검증과 userId 추출을 모두 해당 `Claims`에서 수행한다.

```java
// 수정 후 — parseToken() 1회 호출 (서명 검증 1회)
Claims claims = jwtTokenProvider.parseToken(token);
if (!"access".equals(claims.get("type", String.class))) {
    throw AppException.invalidToken();
}
Long userId = Long.parseLong(claims.getSubject());
```

### 이유

- 인증이 필요한 **모든 API 요청**에서 불필요한 HMAC 연산 1회 제거
- `JwtTokenProvider` API는 변경 없음 — `parseToken()`은 이미 public 메서드
- 동작 보장: `JwtAuthFilterTest` 4개 테스트로 리팩토링 전후 동일한 동작 검증

### 트레이드오프

- `JwtAuthFilter`가 JWT `type` 클레임 키(`"access"`)를 직접 알게 됨 → JWT 내부 구조 지식이 필터에 노출
- 대안으로 `JwtTokenProvider`에 `getUserIdFromAccessToken(String token)` 같은 메서드를 추가하는 방법도 있으나, MVP 단계에서는 YAGNI 원칙에 따라 최소 변경을 선택

---

## [2026-03-28] SecurityConfig: 인증 실패 시 HTTP 상태 코드 동작

### 결정

인증 실패(토큰 없음/만료/잘못됨) 시 Spring Security 기본값인 **403 Forbidden**을 그대로 사용한다.

### 이유

- Task 11 통합 테스트 전까지 프론트엔드 연동 요구사항이 확정되지 않음
- 별도 `AuthenticationEntryPoint`를 추가하면 401을 반환할 수 있으나, 현재 MVP 범위에서는 불필요

### 검토 시점

Task 11 (Auth 통합 테스트) 작성 시 프론트엔드 요구사항(401 vs 403)을 확인 후 재결정.
