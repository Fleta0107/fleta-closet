# Backend MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring Boot 백엔드 MVP 구현 — JWT 인증(회원가입/로그인/갱신/로그아웃) + 의류 CRUD API (이미지 업로드 포함)

**Architecture:** 모듈형 모놀리스. `auth`, `closet`, `common` 세 도메인이 각각 독립적인 controller/service/repository/domain 레이어를 가짐. 도메인 간 직접 의존은 `common`을 통해서만 이루어짐. 각 Task 완료 후 반드시 리뷰하고 다음 단계 진행.

**Tech Stack:** Java 21, Spring Boot 3.4, Spring Security 6, Spring Data JPA, PostgreSQL 16, Flyway, jjwt 0.12, Testcontainers, Gradle, Docker Compose, Lombok

---

## 파일 구조

```
backend/
├── build.gradle
├── docker-compose.yml
├── src/main/java/com/fleta/closet/
│   ├── FletaClosetApplication.java
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── repository/UserRepository.java
│   │   └── domain/
│   │       ├── User.java
│   │       ├── SignupRequest.java       (record)
│   │       ├── LoginRequest.java        (record)
│   │       ├── TokenResponse.java       (record)
│   │       └── UserResponse.java        (record)
│   ├── closet/
│   │   ├── controller/ClothingController.java
│   │   ├── service/ClothingService.java
│   │   ├── repository/ClothingRepository.java
│   │   └── domain/
│   │       ├── ClothingItem.java
│   │       ├── Category.java            (enum)
│   │       ├── ClothingRequest.java     (record)
│   │       └── ClothingResponse.java    (record)
│   └── common/
│       ├── config/
│       │   ├── JpaConfig.java           ← @EnableJpaAuditing 분리
│       │   └── SecurityConfig.java
│       ├── exception/
│       │   ├── AppException.java
│       │   ├── ErrorResponse.java       (record)
│       │   └── GlobalExceptionHandler.java
│       ├── security/
│       │   ├── JwtTokenProvider.java
│       │   └── JwtAuthFilter.java
│       └── storage/FileStorageService.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__create_users.sql
│       └── V2__create_clothing_items.sql
└── src/test/java/com/fleta/closet/
    ├── auth/
    │   ├── AuthServiceTest.java
    │   └── AuthIntegrationTest.java
    ├── closet/
    │   ├── ClothingServiceTest.java
    │   └── ClothingIntegrationTest.java
    └── common/
        └── security/JwtTokenProviderTest.java
```

---

### Task 1: 프로젝트 초기 세팅 ✅

> 📚 **학습 포인트:** Gradle 멀티 모듈이 아닌 단일 모듈로 시작. 나중에 필요 시 분리. `spring-boot-starter-*`는 의존성 묶음(BOM) — 버전을 직접 명시하지 않아도 Spring Boot가 호환 버전을 관리함.

> 🔄 **리팩토링 반영:** `@EnableJpaAuditing`을 `JpaConfig.java`로 분리 (WebMvcTest 슬라이스 테스트 안전성). `.gitignore`에 `.env`, `application-local.yml` 패턴 추가.

**Files:**
- Create: `backend/build.gradle`
- Create: `backend/settings.gradle`
- Create: `backend/docker-compose.yml`
- Create: `backend/.gitignore`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/fleta/closet/FletaClosetApplication.java`
- Create: `backend/src/main/java/com/fleta/closet/common/config/JpaConfig.java`

- [ ] **Step 1: backend 디렉토리 생성 및 build.gradle 작성**

```groovy
// backend/build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.0'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.fleta'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // DB & Migration
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
    runtimeOnly 'org.postgresql:postgresql'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // Lombok (보일러플레이트 코드 제거 — @Getter, @Builder 등)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: docker-compose.yml 작성**

```yaml
# backend/docker-compose.yml
services:
  postgres:
    image: postgres:16
    container_name: fleta-postgres
    environment:
      POSTGRES_DB: fleta_closet
      POSTGRES_USER: fleta
      POSTGRES_PASSWORD: fleta1234
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  postgres-data:
```

- [ ] **Step 3: application.yml 작성**

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fleta_closet
    username: fleta
    password: fleta1234
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate   # Flyway가 스키마 관리 → JPA는 검증만
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: fleta-closet-secret-key-must-be-at-least-256-bits-long-for-hmac-sha

app:
  file-storage:
    base-path: ${user.home}/fleta-uploads

logging:
  level:
    com.fleta: DEBUG
```

- [ ] **Step 4: 메인 애플리케이션 클래스 + JpaConfig 작성**

```java
// backend/src/main/java/com/fleta/closet/FletaClosetApplication.java
package com.fleta.closet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FletaClosetApplication {
    public static void main(String[] args) {
        SpringApplication.run(FletaClosetApplication.class, args);
    }
}
```

```java
// backend/src/main/java/com/fleta/closet/common/config/JpaConfig.java
package com.fleta.closet.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing  // FletaClosetApplication에서 분리 — @WebMvcTest 슬라이스 테스트 충돌 방지
public class JpaConfig {
}
```

> 💡 **이유:** `@EnableJpaAuditing`을 메인 클래스에 두면 `@WebMvcTest` 테스트 시 JPA 컨텍스트가 없어 오류 발생. 별도 `@Configuration`으로 분리하면 안전.

- [ ] **Step 5: Docker로 PostgreSQL 실행 후 빌드 확인**

```bash
cd backend
docker compose up -d

# 잠시 후 DB 기동 확인
docker compose ps
# Expected: fleta-postgres ... Up

# 빌드 확인 (Flyway 마이그레이션 파일이 없어 실패하지만, 컴파일은 성공해야 함)
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 6: 커밋**

```bash
git add backend/
git commit -m "chore: initialize Spring Boot backend project"
git commit -m "chore: improve project config (JpaConfig, .gitignore, yml comments)"
```

---

### Task 2: Flyway DB 마이그레이션 ✅

> 📚 **학습 포인트:** `ddl-auto=create`로 JPA가 자동으로 테이블을 만들 수 있지만, 운영에서는 절대 사용 불가. Flyway는 SQL 파일을 버전별로 관리해 DB 스키마를 코드처럼 변경 이력을 추적함. `V숫자__설명.sql` 네이밍 규칙이 중요.

> 🔄 **리팩토링 반영:** `clothing_item_tags`에 복합 PK `(clothing_item_id, tag)` 추가 — 태그 중복 방지 및 조회 인덱스 확보. **Task 12에서 JPA 엔티티 tags 필드를 `List<String>` 대신 `Set<String>`으로 선언할 것.**

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_users.sql`
- Create: `backend/src/main/resources/db/migration/V2__create_clothing_items.sql`

- [ ] **Step 1: users 테이블 마이그레이션 작성**

```sql
-- backend/src/main/resources/db/migration/V1__create_users.sql
CREATE TABLE users (
    id            BIGSERIAL       PRIMARY KEY,
    email         VARCHAR(255)    NOT NULL UNIQUE,
    password      VARCHAR(255)    NOT NULL,
    nickname      VARCHAR(50)     NOT NULL,
    refresh_token VARCHAR(500),
    created_at    TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP       NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 2: clothing_items 테이블 마이그레이션 작성**

```sql
-- backend/src/main/resources/db/migration/V2__create_clothing_items.sql
CREATE TABLE clothing_items (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category    VARCHAR(50)     NOT NULL,
    brand       VARCHAR(100),
    color       VARCHAR(50),
    image_path  VARCHAR(500),
    memo        TEXT,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- tags는 별도 테이블로 관리 (JPA @ElementCollection)
-- 복합 PK로 중복 태그 방지 및 인덱스 확보 → JPA 엔티티에서 Set<String> 사용
CREATE TABLE clothing_item_tags (
    clothing_item_id BIGINT       NOT NULL REFERENCES clothing_items(id) ON DELETE CASCADE,
    tag              VARCHAR(100) NOT NULL,
    PRIMARY KEY (clothing_item_id, tag)
);

CREATE INDEX idx_clothing_items_user_id ON clothing_items(user_id);
CREATE INDEX idx_clothing_items_category ON clothing_items(category);
```

- [ ] **Step 3: 마이그레이션 적용 확인**

```bash
cd backend
./gradlew bootRun
# Expected: Flyway 로그에 "Successfully applied 2 migrations" 출력 후 애플리케이션 기동

# 또는 직접 확인
docker exec -it fleta-postgres psql -U fleta -d fleta_closet -c "\dt"
# Expected: users, clothing_items, clothing_item_tags, flyway_schema_history 테이블 목록
```

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/resources/db/
git commit -m "feat: add Flyway DB migrations for users and clothing_items"
```

---

### Task 3: 공통 예외 레이어

> 📚 **학습 포인트:** 비즈니스 예외를 하나의 `AppException`으로 통일하면 컨트롤러마다 예외 처리 코드를 반복하지 않아도 됨. `@RestControllerAdvice`는 모든 컨트롤러의 예외를 한 곳에서 잡아 JSON 응답으로 변환함.

**Files:**
- Create: `backend/src/main/java/com/fleta/closet/common/exception/AppException.java`
- Create: `backend/src/main/java/com/fleta/closet/common/exception/ErrorResponse.java`
- Create: `backend/src/main/java/com/fleta/closet/common/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: AppException 작성 (팩토리 메서드 패턴)**

```java
// common/exception/AppException.java
package com.fleta.closet.common.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final int status;
    private final String code;

    public AppException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    // 팩토리 메서드 — 에러 코드를 한 곳에서 관리
    public static AppException duplicateEmail() {
        return new AppException(409, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다");
    }

    public static AppException invalidCredentials() {
        return new AppException(401, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다");
    }

    public static AppException expiredToken() {
        return new AppException(401, "EXPIRED_TOKEN", "만료된 토큰입니다");
    }

    public static AppException invalidToken() {
        return new AppException(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다");
    }

    public static AppException forbidden() {
        return new AppException(403, "FORBIDDEN", "접근 권한이 없습니다");
    }

    public static AppException clothingNotFound() {
        return new AppException(404, "CLOTHING_NOT_FOUND", "의류를 찾을 수 없습니다");
    }

    public static AppException userNotFound() {
        return new AppException(404, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다");
    }
}
```

- [ ] **Step 2: ErrorResponse 작성**

```java
// common/exception/ErrorResponse.java
package com.fleta.closet.common.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
    int status,
    String code,
    String message,
    LocalDateTime timestamp
) {
    public static ErrorResponse of(AppException e) {
        return new ErrorResponse(e.getStatus(), e.getCode(), e.getMessage(), LocalDateTime.now());
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, LocalDateTime.now());
    }
}
```

- [ ] **Step 3: GlobalExceptionHandler 작성**

```java
// common/exception/GlobalExceptionHandler.java
package com.fleta.closet.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e) {
        return ResponseEntity.status(e.getStatus()).body(ErrorResponse.of(e));
    }

    // @Valid 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        String message = fieldError.getField() + ": " + fieldError.getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(400, "INVALID_INPUT", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(500, "INTERNAL_ERROR", "서버 오류가 발생했습니다"));
    }
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/fleta/closet/common/exception/
git commit -m "feat: add common exception layer (AppException, GlobalExceptionHandler)"
```

---

### Task 4: JWT 토큰 발급/검증 (TDD)

> 📚 **학습 포인트:** JWT는 Header.Payload.Signature 3부분으로 구성. 서버는 서명을 검증해 토큰 위변조 여부를 확인. Access Token(단명)과 Refresh Token(장명)을 분리하는 이유: Access Token이 탈취되어도 15분 후 만료되고, Refresh Token으로만 갱신 가능하기 때문.

**Files:**
- Create: `backend/src/test/java/com/fleta/closet/common/security/JwtTokenProviderTest.java`
- Create: `backend/src/main/java/com/fleta/closet/common/security/JwtTokenProvider.java`

- [ ] **Step 1: 실패하는 테스트 먼저 작성**

```java
// common/security/JwtTokenProviderTest.java
package com.fleta.closet.common.security;

import com.fleta.closet.common.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    // Spring 없이 직접 인스턴스화 — 단위 테스트는 Spring Context 불필요
    private static final String SECRET =
        "fleta-closet-secret-key-must-be-at-least-256-bits-long-for-hmac-sha";
    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET);

    @Test
    @DisplayName("Access Token 생성 후 userId 추출 성공")
    void createAccessToken_and_getUserId() {
        String token = provider.createAccessToken(42L);

        assertThat(provider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("Access Token type 확인")
    void accessToken_isAccessToken_true() {
        String token = provider.createAccessToken(1L);

        assertThat(provider.isAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("Refresh Token은 isAccessToken false")
    void refreshToken_isAccessToken_false() {
        String token = provider.createRefreshToken(1L);

        assertThat(provider.isAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰 파싱 시 INVALID_TOKEN 예외")
    void invalidToken_throws_AppException() {
        assertThatThrownBy(() -> provider.parseToken("invalid.token.value"))
            .isInstanceOf(AppException.class)
            .extracting("code").isEqualTo("INVALID_TOKEN");
    }
}
```

- [ ] **Step 2: 테스트 실행해서 컴파일 에러 확인**

```bash
./gradlew test --tests "com.fleta.closet.common.security.JwtTokenProviderTest"
# Expected: 컴파일 에러 — JwtTokenProvider 클래스가 없으므로 실패
```

- [ ] **Step 3: JwtTokenProvider 구현**

```java
// common/security/JwtTokenProvider.java
package com.fleta.closet.common.security;

import com.fleta.closet.common.exception.AppException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private static final long ACCESS_TOKEN_EXPIRY  = 15 * 60 * 1000L;          // 15분
    private static final long REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60 * 1000L; // 7일

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId) {
        return buildToken(userId, "access", ACCESS_TOKEN_EXPIRY);
    }

    public String createRefreshToken(Long userId) {
        return buildToken(userId, "refresh", REFRESH_TOKEN_EXPIRY);
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            throw AppException.expiredToken();
        } catch (JwtException | IllegalArgumentException e) {
            throw AppException.invalidToken();
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseToken(token).get("type", String.class));
    }

    private String buildToken(Long userId, String type, long expiry) {
        Date now = new Date();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", type)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiry))
            .signWith(key)
            .compact();
    }
}
```

- [ ] **Step 4: 테스트 실행해서 통과 확인**

```bash
./gradlew test --tests "com.fleta.closet.common.security.JwtTokenProviderTest"
# Expected: BUILD SUCCESSFUL — 4 tests passed
```

- [ ] **Step 5: 커밋**

```bash
git add backend/src/
git commit -m "feat: implement JwtTokenProvider with TDD"
```

---

### Task 5: JWT 인증 필터 + Security 설정

> 📚 **학습 포인트:** Spring Security는 요청마다 필터 체인을 통과함. `JwtAuthFilter`는 `Authorization: Bearer <token>` 헤더에서 토큰을 꺼내 검증하고, 유효하면 `SecurityContext`에 사용자 정보를 저장함. 이후 컨트롤러에서 `@AuthenticationPrincipal`로 꺼낼 수 있음.

**Files:**
- Create: `backend/src/main/java/com/fleta/closet/common/security/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/fleta/closet/common/config/SecurityConfig.java`

- [ ] **Step 1: JwtAuthFilter 작성**

```java
// common/security/JwtAuthFilter.java
package com.fleta.closet.common.security;

import com.fleta.closet.common.exception.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (!jwtTokenProvider.isAccessToken(token)) {
                    throw AppException.invalidToken();
                }
                Long userId = jwtTokenProvider.getUserId(token);
                // principal로 userId(Long)를 저장 — 컨트롤러에서 @AuthenticationPrincipal Long userId로 꺼냄
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (AppException e) {
                // 토큰 오류는 SecurityContext에 저장하지 않고 통과 — 이후 인증 필요 경로에서 403 발생
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 2: SecurityConfig 작성**

```java
// common/config/SecurityConfig.java
package com.fleta.closet.common.config;

import com.fleta.closet.common.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)           // REST API는 CSRF 불필요
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login", "/api/auth/refresh").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/fleta/closet/common/
git commit -m "feat: add JWT auth filter and Security config"
```

---

### Task 6: User 엔티티 + Repository

> 📚 **학습 포인트:** `@NoArgsConstructor(access = PROTECTED)`는 JPA 스펙 요구사항 (JPA가 내부적으로 기본 생성자를 사용). `protected`로 막아 코드에서 직접 `new User()`를 쓰지 못하게 하고 반드시 빌더를 쓰도록 강제함.

**Files:**
- Create: `backend/src/main/java/com/fleta/closet/auth/domain/User.java`
- Create: `backend/src/main/java/com/fleta/closet/auth/repository/UserRepository.java`

- [ ] **Step 1: User 엔티티 작성**

```java
// auth/domain/User.java
package com.fleta.closet.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    private String refreshToken;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }
}
```

- [ ] **Step 2: UserRepository 작성**

```java
// auth/repository/UserRepository.java
package com.fleta.closet.auth.repository;

import com.fleta.closet.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByRefreshToken(String refreshToken);
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/fleta/closet/auth/
git commit -m "feat: add User entity and UserRepository"
```

---

### Task 7: Auth DTO 작성

> 📚 **학습 포인트:** Java 16+ `record`는 불변 데이터 객체에 최적. 생성자, getter, equals, hashCode, toString을 자동 생성. `@NotBlank`는 Spring Validation — 컨트롤러에서 `@Valid`와 함께 쓰면 자동으로 입력값을 검증하고 실패 시 `MethodArgumentNotValidException`을 던짐 (GlobalExceptionHandler에서 처리).

**Files:**
- Create: `backend/src/main/java/com/fleta/closet/auth/domain/SignupRequest.java`
- Create: `backend/src/main/java/com/fleta/closet/auth/domain/LoginRequest.java`
- Create: `backend/src/main/java/com/fleta/closet/auth/domain/TokenResponse.java`
- Create: `backend/src/main/java/com/fleta/closet/auth/domain/UserResponse.java`

- [ ] **Step 1: 4개 DTO 작성**

```java
// auth/domain/SignupRequest.java
package com.fleta.closet.auth.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @Email(message = "이메일 형식이 올바르지 않습니다")
    @NotBlank(message = "이메일을 입력해주세요")
    String email,

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    String password,

    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    String nickname
) {}
```

```java
// auth/domain/LoginRequest.java
package com.fleta.closet.auth.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password
) {}
```

```java
// auth/domain/TokenResponse.java
package com.fleta.closet.auth.domain;

public record TokenResponse(String accessToken, String refreshToken) {}
```

```java
// auth/domain/UserResponse.java
package com.fleta.closet.auth.domain;

import com.fleta.closet.auth.domain.User;

public record UserResponse(Long id, String email, String nickname) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/fleta/closet/auth/domain/
git commit -m "feat: add Auth DTOs (SignupRequest, LoginRequest, TokenResponse, UserResponse)"
```

---

### Task 8: AuthService — 회원가입 + 로그인 (TDD)

**Files:**
- Create: `backend/src/test/java/com/fleta/closet/auth/AuthServiceTest.java`
- Create: `backend/src/main/java/com/fleta/closet/auth/service/AuthService.java`

- [ ] **Step 1: 실패하는 단위 테스트 먼저 작성**

```java
// auth/AuthServiceTest.java
package com.fleta.closet.auth;

import com.fleta.closet.auth.domain.*;
import com.fleta.closet.auth.repository.UserRepository;
import com.fleta.closet.auth.service.AuthService;
import com.fleta.closet.common.exception.AppException;
import com.fleta.closet.common.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        SignupRequest request = new SignupRequest("user@test.com", "pass1234!", "테스터");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed_pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> authService.signup(request)).doesNotThrowAnyException();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복 이메일 회원가입 → DUPLICATE_EMAIL 예외")
    void signup_duplicateEmail_throws() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("dup@test.com", "pass1234!", "닉")))
            .isInstanceOf(AppException.class)
            .extracting("code").isEqualTo("DUPLICATE_EMAIL");
    }

    @Test
    @DisplayName("로그인 성공 → accessToken + refreshToken 반환")
    void login_success() {
        User user = User.builder().id(1L).email("user@test.com").password("hashed_pw").nickname("닉").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass1234!", "hashed_pw")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(user);

        TokenResponse response = authService.login(new LoginRequest("user@test.com", "pass1234!"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(user); // refreshToken DB 저장 확인
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 → INVALID_CREDENTIALS 예외")
    void login_wrongPassword_throws() {
        User user = User.builder().id(1L).email("user@test.com").password("hashed_pw").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@test.com", "wrong")))
            .isInstanceOf(AppException.class)
            .extracting("code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 로그인 → INVALID_CREDENTIALS 예외")
    void login_emailNotFound_throws() {
        when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("none@test.com", "pass")))
            .isInstanceOf(AppException.class)
            .extracting("code").isEqualTo("INVALID_CREDENTIALS");
    }
}
```

- [ ] **Step 2: 테스트 실행 → 컴파일 에러 확인**

```bash
./gradlew test --tests "com.fleta.closet.auth.AuthServiceTest"
# Expected: 컴파일 에러 — AuthService 없으므로 실패
```

- [ ] **Step 3: AuthService 구현 (signup + login)**

```java
// auth/service/AuthService.java
package com.fleta.closet.auth.service;

import com.fleta.closet.auth.domain.*;
import com.fleta.closet.auth.repository.UserRepository;
import com.fleta.closet.common.exception.AppException;
import com.fleta.closet.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw AppException.duplicateEmail();
        }
        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .nickname(request.nickname())
            .build();
        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(AppException::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw AppException.invalidCredentials();
        }

        String accessToken  = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        user.updateRefreshToken(refreshToken); // Refresh Token Rotation: DB에 저장
        userRepository.save(user);

        return new TokenResponse(accessToken, refreshToken);
    }
}
```

- [ ] **Step 4: 테스트 실행 → 모두 통과 확인**

```bash
./gradlew test --tests "com.fleta.closet.auth.AuthServiceTest"
# Expected: BUILD SUCCESSFUL — 4 tests passed
```

- [ ] **Step 5: 커밋**

```bash
git add backend/src/
git commit -m "feat: implement AuthService signup and login with TDD"
```

---

### Task 9: AuthService — 토큰 갱신 + 로그아웃 + 내 정보 (TDD)

**Files:**
- Modify: `backend/src/test/java/com/fleta/closet/auth/AuthServiceTest.java`
- Modify: `backend/src/main/java/com/fleta/closet/auth/service/AuthService.java`

- [ ] **Step 1: 테스트 추가 (AuthServiceTest에 append)**

```java
// AuthServiceTest.java 에 아래 테스트 메서드 추가

@Test
@DisplayName("유효한 Refresh Token으로 새 Access Token 발급")
void refresh_success() {
    User user = User.builder().id(1L).refreshToken("valid-refresh").build();
    when(userRepository.findByRefreshToken("valid-refresh")).thenReturn(Optional.of(user));
    when(jwtTokenProvider.getUserId("valid-refresh")).thenReturn(1L);
    when(jwtTokenProvider.isAccessToken("valid-refresh")).thenReturn(false);
    when(jwtTokenProvider.createAccessToken(1L)).thenReturn("new-access-token");
    when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("new-refresh-token");
    when(userRepository.save(any(User.class))).thenReturn(user);

    TokenResponse response = authService.refresh("valid-refresh");

    assertThat(response.accessToken()).isEqualTo("new-access-token");
    assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
}

@Test
@DisplayName("DB에 없는 Refresh Token → INVALID_TOKEN 예외")
void refresh_unknownToken_throws() {
    when(userRepository.findByRefreshToken("unknown")).thenReturn(Optional.empty());
    when(jwtTokenProvider.isAccessToken("unknown")).thenReturn(false);

    assertThatThrownBy(() -> authService.refresh("unknown"))
        .isInstanceOf(AppException.class)
        .extracting("code").isEqualTo("INVALID_TOKEN");
}

@Test
@DisplayName("로그아웃 시 Refresh Token null 처리")
void logout_success() {
    User user = User.builder().id(1L).refreshToken("some-token").build();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenReturn(user);

    authService.logout(1L);

    verify(userRepository).save(user);
}

@Test
@DisplayName("내 정보 조회 성공")
void getMe_success() {
    User user = User.builder().id(1L).email("me@test.com").nickname("나").build();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    UserResponse response = authService.getMe(1L);

    assertThat(response.email()).isEqualTo("me@test.com");
    assertThat(response.nickname()).isEqualTo("나");
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인 (refresh, logout, getMe 미구현)**

```bash
./gradlew test --tests "com.fleta.closet.auth.AuthServiceTest"
# Expected: 4개 새 테스트 실패
```

- [ ] **Step 3: AuthService에 refresh, logout, getMe 추가**

```java
// AuthService.java에 아래 메서드 추가

public TokenResponse refresh(String refreshToken) {
    // Access Token이 넘어온 경우 거부
    if (jwtTokenProvider.isAccessToken(refreshToken)) {
        throw AppException.invalidToken();
    }

    User user = userRepository.findByRefreshToken(refreshToken)
        .orElseThrow(AppException::invalidToken);

    // Refresh Token Rotation: 새 토큰 발급 + 기존 무효화
    String newAccess  = jwtTokenProvider.createAccessToken(user.getId());
    String newRefresh = jwtTokenProvider.createRefreshToken(user.getId());

    user.updateRefreshToken(newRefresh);
    userRepository.save(user);

    return new TokenResponse(newAccess, newRefresh);
}

public void logout(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(AppException::userNotFound);
    user.clearRefreshToken();
    userRepository.save(user);
}

@Transactional(readOnly = true)
public UserResponse getMe(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(AppException::userNotFound);
    return UserResponse.from(user);
}
```

- [ ] **Step 4: 전체 AuthServiceTest 통과 확인**

```bash
./gradlew test --tests "com.fleta.closet.auth.AuthServiceTest"
# Expected: BUILD SUCCESSFUL — 8 tests passed
```

- [ ] **Step 5: 커밋**

```bash
git add backend/src/
git commit -m "feat: add AuthService refresh, logout, getMe with TDD"
```

---

### Task 10: AuthController

> 📚 **학습 포인트:** `@AuthenticationPrincipal Long userId`는 `JwtAuthFilter`가 `SecurityContext`에 저장한 `principal`을 꺼내옴. Spring이 자동으로 주입하므로 컨트롤러에서 별도로 토큰 파싱 불필요.

**Files:**
- Create: `backend/src/main/java/com/fleta/closet/auth/controller/AuthController.java`

- [ ] **Step 1: AuthController 작성**

```java
// auth/controller/AuthController.java
package com.fleta.closet.auth.controller;

import com.fleta.closet.auth.domain.*;
import com.fleta.closet.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken.trim()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(authService.getMe(userId));
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/fleta/closet/auth/controller/
git commit -m "feat: add AuthController (signup, login, refresh, logout, me)"
```

---

### Task 11: Auth 통합 테스트

> 📚 **학습 포인트:** `Testcontainers`는 테스트 실행 시 실제 PostgreSQL Docker 컨테이너를 자동으로 시작/종료함. H2 인메모리 DB와 달리 운영 환경과 동일한 DB를 사용하므로 Flyway 마이그레이션, PostgreSQL 고유 기능도 그대로 테스트 가능.

**Files:**
- Create: `backend/src/test/java/com/fleta/closet/auth/AuthIntegrationTest.java`

- [ ] **Step 1: 통합 테스트 작성**

```java
// auth/AuthIntegrationTest.java
package com.fleta.closet.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleta.closet.auth.domain.LoginRequest;
import com.fleta.closet.auth.domain.SignupRequest;
import com.fleta.closet.auth.domain.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("fleta_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 → 로그인 → 내 정보 조회 전체 흐름")
    void signup_login_me_flow() throws Exception {
        // 1. 회원가입
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignupRequest("test@fleta.com", "password123", "테스터"))))
            .andExpect(status().isCreated());

        // 2. 로그인
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("test@fleta.com", "password123"))))
            .andExpect(status().isOk())
            .andReturn();

        TokenResponse tokens = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), TokenResponse.class);
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        // 3. 내 정보 조회
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + tokens.accessToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@fleta.com"))
            .andExpect(jsonPath("$.nickname").value("테스터"));
    }

    @Test
    @DisplayName("Refresh Token으로 새 Access Token 발급")
    void refresh_flow() throws Exception {
        // 1. 가입 + 로그인
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignupRequest("refresh@fleta.com", "password123", "갱신유저"))))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("refresh@fleta.com", "password123"))))
            .andExpect(status().isOk())
            .andReturn();

        TokenResponse tokens = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), TokenResponse.class);

        // 2. Refresh Token으로 새 토큰 발급
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.TEXT_PLAIN)
                .content(tokens.refreshToken()))
            .andExpect(status().isOk())
            .andReturn();

        TokenResponse newTokens = objectMapper.readValue(
            refreshResult.getResponse().getContentAsString(), TokenResponse.class);
        assertThat(newTokens.accessToken()).isNotBlank();
        assertThat(newTokens.accessToken()).isNotEqualTo(tokens.accessToken()); // 새 토큰 확인
    }

    @Test
    @DisplayName("인증 없이 보호된 경로 접근 → 403")
    void unauthenticated_returns_403() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("중복 이메일 회원가입 → 409")
    void signup_duplicate_returns_409() throws Exception {
        String body = objectMapper.writeValueAsString(
            new SignupRequest("dup@fleta.com", "password123", "닉"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }
}
```

- [ ] **Step 2: 테스트 실행 (Docker 실행 중 필요)**

```bash
./gradlew test --tests "com.fleta.closet.auth.AuthIntegrationTest"
# Expected: BUILD SUCCESSFUL — 4 tests passed
# 첫 실행 시 postgres:16 이미지 다운로드로 1-2분 소요
```

- [ ] **Step 3: 커밋**

```bash
git add backend/src/test/
git commit -m "test: add Auth integration tests with Testcontainers"
```

---

### Task 12: ClothingItem 엔티티 + Category + Repository

> ⚠️ **Task 2 리팩토링 반영:** `clothing_item_tags` 테이블에 복합 PK `(clothing_item_id, tag)`가 있으므로 tags 필드를 `Set<String>`으로 선언해야 함. `List<String>`은 중복을 허용하므로 DB 제약 위반 발생.

**Files:**
- Create: `backend/src/main/java/com/fleta/closet/closet/domain/Category.java`
- Create: `backend/src/main/java/com/fleta/closet/closet/domain/ClothingItem.java`
- Create: `backend/src/main/java/com/fleta/closet/closet/repository/ClothingRepository.java`

- [ ] **Step 1: Category enum 작성**

```java
// closet/domain/Category.java
package com.fleta.closet.closet.domain;

public enum Category {
    TOPS, BOTTOMS, OUTERWEAR, SHOES, ACCESSORIES, BAGS
}
```

- [ ] **Step 2: ClothingItem 엔티티 작성**

```java
// closet/domain/ClothingItem.java
package com.fleta.closet.closet.domain;

import com.fleta.closet.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clothing_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class ClothingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private String brand;
    private String color;
    private String imagePath;
    private String memo;

    // @ElementCollection: tags 별도 테이블(clothing_item_tags)로 관리
    @ElementCollection
    @CollectionTable(
        name = "clothing_item_tags",
        joinColumns = @JoinColumn(name = "clothing_item_id")
    )
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();  // List → Set: clothing_item_tags 복합 PK와 일치

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void update(Category category, String brand, String color,
                       Set<String> tags, String memo, String imagePath) {
        this.category  = category;
        this.brand     = brand;
        this.color     = color;
        this.memo      = memo;
        this.tags.clear();
        this.tags.addAll(tags != null ? tags : Set.of());
        if (imagePath != null) {
            this.imagePath = imagePath;
        }
    }
}
```

- [ ] **Step 3: ClothingRepository 작성**

```java
// closet/repository/ClothingRepository.java
package com.fleta.closet.closet.repository;

import com.fleta.closet.closet.domain.Category;
import com.fleta.closet.closet.domain.ClothingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClothingRepository extends JpaRepository<ClothingItem, Long> {
    List<ClothingItem> findByUserId(Long userId);
    List<ClothingItem> findByUserIdAndCategory(Long userId, Category category);
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/fleta/closet/closet/
git commit -m "feat: add ClothingItem entity, Category enum, ClothingRepository"
```

---

### Task 13: Clothing DTO 작성

**Files:**
- Create: `backend/src/main/java/com/fleta/closet/closet/domain/ClothingRequest.java`
- Create: `backend/src/main/java/com/fleta/closet/closet/domain/ClothingResponse.java`

- [ ] **Step 1: ClothingRequest + ClothingResponse 작성**

```java
// closet/domain/ClothingRequest.java
package com.fleta.closet.closet.domain;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ClothingRequest(
    @NotNull(message = "카테고리를 선택해주세요")
    Category category,
    String brand,
    String color,
    Set<String> tags,
    String memo
) {}
```

```java
// closet/domain/ClothingResponse.java
package com.fleta.closet.closet.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ClothingResponse(
    Long id,
    Category category,
    String brand,
    String color,
    Set<String> tags,
    String memo,
    String imageUrl,   // 이미지 조회 API URL (/api/closet/items/{id}/image)
    LocalDateTime createdAt
) {
    public static ClothingResponse from(ClothingItem item) {
        String imageUrl = item.getImagePath() != null
            ? "/api/closet/items/" + item.getId() + "/image"
            : null;
        return new ClothingResponse(
            item.getId(),
            item.getCategory(),
            item.getBrand(),
            item.getColor(),
            item.getTags(),
            item.getMemo(),
            imageUrl,
            item.getCreatedAt()
        );
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/fleta/closet/closet/domain/
git commit -m "feat: add Clothing DTOs (ClothingRequest, ClothingResponse)"
```

---

### Task 14: 파일 스토리지 서비스

**Files:**
- Create: `backend/src/main/java/com/fleta/closet/common/storage/FileStorageService.java`

- [ ] **Step 1: FileStorageService 작성**

```java
// common/storage/FileStorageService.java
package com.fleta.closet.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path basePath;

    public FileStorageService(@Value("${app.file-storage.base-path}") String basePath) {
        this.basePath = Path.of(basePath);
    }

    /**
     * 이미지 파일을 {basePath}/{userId}/{uuid}.ext 경로에 저장 후 경로 반환
     */
    public String store(Long userId, MultipartFile file) throws IOException {
        Path userDir = basePath.resolve(userId.toString());
        Files.createDirectories(userDir);

        String extension = getExtension(file.getOriginalFilename());
        String filename   = UUID.randomUUID() + extension;
        Path   target     = userDir.resolve(filename);

        file.transferTo(target);
        return target.toString();
    }

    /**
     * 저장된 경로에서 Resource 로드
     */
    public Resource load(String absolutePath) throws MalformedURLException {
        Resource resource = new UrlResource(Path.of(absolutePath).toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("파일을 읽을 수 없습니다: " + absolutePath);
        }
        return resource;
    }

    /**
     * 저장된 파일 삭제 (의류 삭제 시 호출)
     */
    public void delete(String absolutePath) throws IOException {
        Files.deleteIfExists(Path.of(absolutePath));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/fleta/closet/common/storage/
git commit -m "feat: add FileStorageService for local image storage"
```

---

### Task 15: ClothingService CRUD (TDD)

**Files:**
- Create: `backend/src/test/java/com/fleta/closet/closet/ClothingServiceTest.java`
- Create: `backend/src/main/java/com/fleta/closet/closet/service/ClothingService.java`

- [ ] **Step 1: 실패하는 단위 테스트 작성**

```java
// closet/ClothingServiceTest.java
package com.fleta.closet.closet;

import com.fleta.closet.auth.domain.User;
import com.fleta.closet.auth.repository.UserRepository;
import com.fleta.closet.closet.domain.*;
import com.fleta.closet.closet.repository.ClothingRepository;
import com.fleta.closet.closet.service.ClothingService;
import com.fleta.closet.common.exception.AppException;
import com.fleta.closet.common.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClothingServiceTest {

    @Mock ClothingRepository clothingRepository;
    @Mock UserRepository userRepository;
    @Mock FileStorageService fileStorageService;
    @InjectMocks ClothingService clothingService;

    @Test
    @DisplayName("의류 등록 성공")
    void create_success() throws Exception {
        User user = User.builder().id(1L).email("u@t.com").nickname("닉").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ClothingRequest request = new ClothingRequest(Category.TOPS, "Nike", "White", Set.of("Casual"), null);
        ClothingItem saved = ClothingItem.builder().id(10L).user(user).category(Category.TOPS).build();
        when(clothingRepository.save(any(ClothingItem.class))).thenReturn(saved);

        ClothingResponse response = clothingService.create(1L, request, null);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.category()).isEqualTo(Category.TOPS);
    }

    @Test
    @DisplayName("카테고리 필터 없이 목록 조회")
    void findAll_noFilter() {
        User user = User.builder().id(1L).build();
        ClothingItem item = ClothingItem.builder().id(1L).user(user).category(Category.TOPS).build();
        when(clothingRepository.findByUserId(1L)).thenReturn(List.of(item));

        List<ClothingResponse> result = clothingService.findAll(1L, null);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("카테고리 필터로 목록 조회")
    void findAll_withCategory() {
        User user = User.builder().id(1L).build();
        ClothingItem item = ClothingItem.builder().id(1L).user(user).category(Category.TOPS).build();
        when(clothingRepository.findByUserIdAndCategory(1L, Category.TOPS)).thenReturn(List.of(item));

        List<ClothingResponse> result = clothingService.findAll(1L, Category.TOPS);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("타인의 의류 조회 → FORBIDDEN 예외")
    void findById_otherUser_throws() {
        User owner = User.builder().id(2L).build();
        ClothingItem item = ClothingItem.builder().id(1L).user(owner).category(Category.TOPS).build();
        when(clothingRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> clothingService.findById(1L, 1L)) // userId=1, 소유자=2
            .isInstanceOf(AppException.class)
            .extracting("code").isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("존재하지 않는 의류 조회 → CLOTHING_NOT_FOUND 예외")
    void findById_notFound_throws() {
        when(clothingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clothingService.findById(1L, 99L))
            .isInstanceOf(AppException.class)
            .extracting("code").isEqualTo("CLOTHING_NOT_FOUND");
    }

    @Test
    @DisplayName("의류 삭제 성공")
    void delete_success() throws Exception {
        User user = User.builder().id(1L).build();
        ClothingItem item = ClothingItem.builder().id(1L).user(user).category(Category.TOPS).imagePath("/img/1.jpg").build();
        when(clothingRepository.findById(1L)).thenReturn(Optional.of(item));

        clothingService.delete(1L, 1L);

        verify(fileStorageService).delete("/img/1.jpg");
        verify(clothingRepository).delete(item);
    }
}
```

- [ ] **Step 2: 테스트 실행 → 컴파일 에러 확인**

```bash
./gradlew test --tests "com.fleta.closet.closet.ClothingServiceTest"
# Expected: 컴파일 에러 — ClothingService 없으므로 실패
```

- [ ] **Step 3: ClothingService 구현**

```java
// closet/service/ClothingService.java
package com.fleta.closet.closet.service;

import com.fleta.closet.auth.domain.User;
import com.fleta.closet.auth.repository.UserRepository;
import com.fleta.closet.closet.domain.*;
import com.fleta.closet.closet.repository.ClothingRepository;
import com.fleta.closet.common.exception.AppException;
import com.fleta.closet.common.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClothingService {

    private final ClothingRepository clothingRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ClothingResponse create(Long userId, ClothingRequest request, MultipartFile image) throws IOException {
        User user = userRepository.findById(userId)
            .orElseThrow(AppException::userNotFound);

        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            imagePath = fileStorageService.store(userId, image);
        }

        ClothingItem item = ClothingItem.builder()
            .user(user)
            .category(request.category())
            .brand(request.brand())
            .color(request.color())
            .tags(request.tags() != null ? request.tags() : Set.of())
            .memo(request.memo())
            .imagePath(imagePath)
            .build();

        return ClothingResponse.from(clothingRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<ClothingResponse> findAll(Long userId, Category category) {
        List<ClothingItem> items = category != null
            ? clothingRepository.findByUserIdAndCategory(userId, category)
            : clothingRepository.findByUserId(userId);

        return items.stream().map(ClothingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClothingResponse findById(Long userId, Long itemId) {
        ClothingItem item = getItemForUser(userId, itemId);
        return ClothingResponse.from(item);
    }

    public ClothingResponse update(Long userId, Long itemId,
                                    ClothingRequest request, MultipartFile image) throws IOException {
        ClothingItem item = getItemForUser(userId, itemId);

        String newImagePath = null;
        if (image != null && !image.isEmpty()) {
            if (item.getImagePath() != null) {
                fileStorageService.delete(item.getImagePath()); // 기존 이미지 삭제
            }
            newImagePath = fileStorageService.store(userId, image);
        }

        item.update(request.category(), request.brand(), request.color(),
                    request.tags(), request.memo(), newImagePath);

        return ClothingResponse.from(clothingRepository.save(item));
    }

    public void delete(Long userId, Long itemId) throws IOException {
        ClothingItem item = getItemForUser(userId, itemId);
        if (item.getImagePath() != null) {
            fileStorageService.delete(item.getImagePath());
        }
        clothingRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public Resource getImage(Long userId, Long itemId) throws Exception {
        ClothingItem item = getItemForUser(userId, itemId);
        if (item.getImagePath() == null) {
            throw AppException.clothingNotFound(); // 이미지 없는 경우
        }
        return fileStorageService.load(item.getImagePath());
    }

    private ClothingItem getItemForUser(Long userId, Long itemId) {
        ClothingItem item = clothingRepository.findById(itemId)
            .orElseThrow(AppException::clothingNotFound);
        if (!item.getUser().getId().equals(userId)) {
            throw AppException.forbidden();
        }
        return item;
    }
}
```

- [ ] **Step 4: 테스트 실행 → 모두 통과 확인**

```bash
./gradlew test --tests "com.fleta.closet.closet.ClothingServiceTest"
# Expected: BUILD SUCCESSFUL — 6 tests passed
```

- [ ] **Step 5: 커밋**

```bash
git add backend/src/
git commit -m "feat: implement ClothingService CRUD with TDD"
```

---

### Task 16: ClothingController

**Files:**
- Create: `backend/src/main/java/com/fleta/closet/closet/controller/ClothingController.java`

- [ ] **Step 1: ClothingController 작성**

```java
// closet/controller/ClothingController.java
package com.fleta.closet.closet.controller;

import com.fleta.closet.closet.domain.Category;
import com.fleta.closet.closet.domain.ClothingRequest;
import com.fleta.closet.closet.domain.ClothingResponse;
import com.fleta.closet.closet.service.ClothingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/closet/items")
@RequiredArgsConstructor
public class ClothingController {

    private final ClothingService clothingService;

    @GetMapping
    public ResponseEntity<List<ClothingResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Category category) throws Exception {
        return ResponseEntity.ok(clothingService.findAll(userId, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClothingResponse> get(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) throws Exception {
        return ResponseEntity.ok(clothingService.findById(userId, id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothingResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("data") ClothingRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(clothingService.create(userId, request, image));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothingResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestPart("data") ClothingRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {
        return ResponseEntity.ok(clothingService.update(userId, id, request, image));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) throws Exception {
        clothingService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) throws Exception {
        Resource image = clothingService.getImage(userId, id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .contentType(MediaType.IMAGE_JPEG)
            .body(image);
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/fleta/closet/closet/controller/
git commit -m "feat: add ClothingController (CRUD + image endpoints)"
```

---

### Task 17: Closet 통합 테스트

**Files:**
- Create: `backend/src/test/java/com/fleta/closet/closet/ClothingIntegrationTest.java`

- [ ] **Step 1: Closet 통합 테스트 작성**

```java
// closet/ClothingIntegrationTest.java
package com.fleta.closet.closet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleta.closet.auth.domain.LoginRequest;
import com.fleta.closet.auth.domain.SignupRequest;
import com.fleta.closet.auth.domain.TokenResponse;
import com.fleta.closet.closet.domain.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ClothingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("fleta_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트마다 새 사용자로 가입+로그인
        String email = "closet_" + System.currentTimeMillis() + "@test.com";
        mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new SignupRequest(email, "pass1234!", "유저"))));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "pass1234!"))))
            .andReturn();

        accessToken = objectMapper.readValue(
            result.getResponse().getContentAsString(), TokenResponse.class).accessToken();
    }

    @Test
    @DisplayName("의류 등록 → 목록 조회 → 단건 조회 → 삭제 흐름")
    void clothing_crud_flow() throws Exception {
        // 등록
        String requestJson = objectMapper.writeValueAsString(
            new com.fleta.closet.closet.domain.ClothingRequest(
                Category.TOPS, "Nike", "White", java.util.Set.of("Casual"), null));

        MockMultipartFile dataPart  = new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        MvcResult createResult = mockMvc.perform(multipart("/api/closet/items")
                .file(dataPart)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.category").value("TOPS"))
            .andExpect(jsonPath("$.brand").value("Nike"))
            .andReturn();

        Long itemId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // 목록 조회
        mockMvc.perform(get("/api/closet/items")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        // 카테고리 필터 조회
        mockMvc.perform(get("/api/closet/items").param("category", "BOTTOMS")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        // 단건 조회
        mockMvc.perform(get("/api/closet/items/" + itemId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(itemId));

        // 삭제
        mockMvc.perform(delete("/api/closet/items/" + itemId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNoContent());

        // 삭제 후 조회 → 404
        mockMvc.perform(get("/api/closet/items/" + itemId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 없이 의류 목록 조회 → 403")
    void unauthenticated_returns_403() throws Exception {
        mockMvc.perform(get("/api/closet/items"))
            .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: 전체 테스트 실행**

```bash
./gradlew test
# Expected: BUILD SUCCESSFUL — 전체 테스트 통과
# 테스트 결과 보고서: backend/build/reports/tests/test/index.html
```

- [ ] **Step 3: 최종 커밋**

```bash
git add backend/src/test/
git commit -m "test: add Closet integration tests — all tests passing"
```

---

## 검증 체크리스트

모든 Task 완료 후 아래를 확인하세요:

```bash
# 1. 전체 테스트 통과
cd backend
./gradlew test
# Expected: BUILD SUCCESSFUL

# 2. 앱 실행 (Docker PostgreSQL 필요)
docker compose up -d
./gradlew bootRun

# 3. 수동 API 테스트 (curl)
# 회원가입
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"me@fleta.com","password":"pass1234!","nickname":"나"}'
# Expected: 201 Created

# 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"me@fleta.com","password":"pass1234!"}'
# Expected: {"accessToken":"...","refreshToken":"..."}

# 의류 등록 (accessToken 사용)
curl -X POST http://localhost:8080/api/closet/items \
  -H "Authorization: Bearer <accessToken>" \
  -F 'data={"category":"TOPS","brand":"Nike","color":"White","tags":["Casual"]};type=application/json'
# Expected: 201 Created
```
