package com.fleta.closet.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleta.closet.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExceptionTestController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean JwtTokenProvider jwtTokenProvider;  // JwtAuthFilter 의존성 해소

    @Test
    void appException_returnsCorrectStatusAndErrorCode() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/app-exception"))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(404);
        assertThat(body.get("code").asText()).isEqualTo("CLOTHING_NOT_FOUND");
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    @Test
    void validationException_returns400WithFieldMessage() throws Exception {
        String body = "{\"email\": \"not-an-email\"}";

        MvcResult result = mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(responseBody.get("status").asInt()).isEqualTo(400);
        assertThat(responseBody.get("code").asText()).isEqualTo("INVALID_INPUT");
        assertThat(responseBody.get("message").asText()).contains("email");
    }

    @Test
    void generalException_returns500() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/general-exception"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        JsonNode responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(responseBody.get("status").asInt()).isEqualTo(500);
        assertThat(responseBody.get("code").asText()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
