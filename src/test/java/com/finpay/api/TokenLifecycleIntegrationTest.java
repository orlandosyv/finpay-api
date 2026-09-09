package com.finpay.api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.finpay.api.config.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class TokenLifecycleIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void rotatesRefreshTokenAndRevokesSessionOnLogout() throws Exception {
        register();
        TokenPair loginTokens = login();

        mockMvc.perform(get("/api/merchant/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("token-owner@finpay.test"));

        TokenPair refreshedTokens = refresh(loginTokens.refreshToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(loginTokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Refresh token is invalid or expired"));

        mockMvc.perform(get("/api/merchant/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(refreshedTokens.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(refreshedTokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshedTokens.refreshToken())))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/merchant/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(refreshedTokens.accessToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required or the access token is invalid"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshedTokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Refresh token is invalid or expired"));
    }

    @Test
    void rejectsUnknownRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody("unknown-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/auth/refresh"));
    }

    private void register() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "Token Shop",
                                  "email": "token-owner@finpay.test",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private TokenPair login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "token-owner@finpay.test",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshExpiresIn").value(604800))
                .andReturn();
        return tokenPair(result);
    }

    private TokenPair refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        return tokenPair(result);
    }

    private TokenPair tokenPair(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        return new TokenPair(
                JsonPath.read(response, "$.accessToken"),
                JsonPath.read(response, "$.refreshToken"));
    }

    private String refreshBody(String refreshToken) {
        return """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
