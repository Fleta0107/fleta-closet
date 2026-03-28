package com.fleta.closet.closet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleta.closet.auth.domain.LoginRequest;
import com.fleta.closet.auth.domain.SignupRequest;
import com.fleta.closet.auth.domain.TokenResponse;
import com.fleta.closet.closet.domain.Category;
import com.fleta.closet.closet.domain.ClothingRequest;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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

    // @BeforeEach마다 새 이메일로 가입+로그인 → 테스트 간 데이터 격리
    @BeforeEach
    void setUp() throws Exception {
        String email = "closet_" + System.currentTimeMillis() + "@test.com";

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest(email, "pass1234!", "유저"))))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "pass1234!"))))
            .andExpect(status().isOk())
            .andReturn();

        accessToken = objectMapper.readValue(
            result.getResponse().getContentAsString(), TokenResponse.class).accessToken();
        assertThat(accessToken).isNotBlank();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private String loginAs(String email) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest(email, "pass1234!", "다른유저"))))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "pass1234!"))))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readValue(
            result.getResponse().getContentAsString(), TokenResponse.class).accessToken();
    }

    private Long createItem(Category category, String brand) throws Exception {
        String requestJson = objectMapper.writeValueAsString(
            new ClothingRequest(category, brand, "White", Set.of("Casual"), null));
        MockMultipartFile dataPart = new MockMultipartFile(
            "data", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/closet/items")
                .file(dataPart)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    // ─── tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("의류 등록 → 목록 조회 → 카테고리 필터 → 단건 조회")
    void create_and_read_flow() throws Exception {
        Long itemId = createItem(Category.TOPS, "Nike");

        // 목록 조회 — 방금 등록한 1건
        mockMvc.perform(get("/api/closet/items")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].category").value("TOPS"));

        // 카테고리 필터 — 없는 카테고리
        mockMvc.perform(get("/api/closet/items").param("category", "BOTTOMS")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        // 단건 조회
        mockMvc.perform(get("/api/closet/items/" + itemId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(itemId))
            .andExpect(jsonPath("$.brand").value("Nike"));
    }

    @Test
    @DisplayName("의류 수정 → 변경 내용 반영 확인")
    void update_flow() throws Exception {
        Long itemId = createItem(Category.TOPS, "Nike");

        // 수정
        String updateJson = objectMapper.writeValueAsString(
            new ClothingRequest(Category.BOTTOMS, "Adidas", "Black", Set.of(), null));
        MockMultipartFile dataPart = new MockMultipartFile(
            "data", "", MediaType.APPLICATION_JSON_VALUE, updateJson.getBytes());

        mockMvc.perform(multipart("/api/closet/items/" + itemId)
                .file(dataPart)
                .with(request -> { request.setMethod("PUT"); return request; })
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.category").value("BOTTOMS"))
            .andExpect(jsonPath("$.brand").value("Adidas"));

        // 재조회로 변경 내용 확인
        mockMvc.perform(get("/api/closet/items/" + itemId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.category").value("BOTTOMS"));
    }

    @Test
    @DisplayName("의류 삭제 → 삭제 후 조회 시 404")
    void delete_flow() throws Exception {
        Long itemId = createItem(Category.TOPS, "Nike");

        mockMvc.perform(delete("/api/closet/items/" + itemId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/closet/items/" + itemId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 사용자의 의류 조회/삭제 시 404 (소유자 정보 비노출)")
    void other_user_access_returns_404() throws Exception {
        Long itemId = createItem(Category.TOPS, "Nike");

        // 다른 사용자 로그인
        String otherToken = loginAs("other_" + System.currentTimeMillis() + "@test.com");

        // 타인 아이템 단건 조회 → 404
        mockMvc.perform(get("/api/closet/items/" + itemId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound());

        // 타인 아이템 삭제 → 404
        mockMvc.perform(delete("/api/closet/items/" + itemId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("category 없이 의류 등록 → 400 (Bean Validation)")
    void create_without_category_returns_400() throws Exception {
        String requestJson = objectMapper.writeValueAsString(
            new ClothingRequest(null, "Nike", "White", Set.of(), null));
        MockMultipartFile dataPart = new MockMultipartFile(
            "data", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(multipart("/api/closet/items")
                .file(dataPart)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증 없이 의류 목록 조회 → 401")
    void unauthenticated_returns_401() throws Exception {
        mockMvc.perform(get("/api/closet/items"))
            .andExpect(status().isUnauthorized());
    }
}
