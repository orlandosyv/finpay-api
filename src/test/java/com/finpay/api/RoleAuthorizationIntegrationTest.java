package com.finpay.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.finpay.api.model.MerchantRole;
import com.finpay.api.model.MerchantUser;
import com.finpay.api.repository.MerchantUserRepository;
import com.finpay.api.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class RoleAuthorizationIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantUserRepository merchantUserRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminManagesMerchantUsersAndOperatorHasLimitedPaymentPermissions() throws Exception {
        Long merchantId = register(
                "RBAC Shop",
                "rbac-admin@finpay.test",
                "StrongPassword123!");
        String adminToken = login("rbac-admin@finpay.test", "StrongPassword123!");

        mockMvc.perform(get("/api/merchant/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.merchantName").value("RBAC Shop"))
                .andExpect(jsonPath("$.email").value("rbac-admin@finpay.test"))
                .andExpect(jsonPath("$.role").value("MERCHANT_ADMIN"));

        mockMvc.perform(post("/api/merchant/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "rbac-operator@finpay.test",
                                  "password": "OperatorPassword123!",
                                  "role": "MERCHANT_USER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("rbac-operator@finpay.test"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("MERCHANT_USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(get("/api/merchant/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("MERCHANT_ADMIN"))
                .andExpect(jsonPath("$[1].role").value("MERCHANT_USER"));

        String operatorToken = login(
                "rbac-operator@finpay.test",
                "OperatorPassword123!");

        mockMvc.perform(get("/api/merchant/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.role").value("MERCHANT_USER"));

        MvcResult createPaymentResult = mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operatorToken))
                        .header("Idempotency-Key", "rbac-operator-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 95.00,
                                  "currency": "PEN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        Number paymentId = JsonPath.read(
                createPaymentResult.getResponse().getContentAsString(),
                "$.id");

        mockMvc.perform(get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        assertForbidden(patch("/api/payments/{id}/approve", paymentId.longValue())
                .header(HttpHeaders.AUTHORIZATION, bearer(operatorToken)));
        assertForbidden(get("/api/merchant/users")
                .header(HttpHeaders.AUTHORIZATION, bearer(operatorToken)));
        assertForbidden(post("/api/merchant/users")
                .header(HttpHeaders.AUTHORIZATION, bearer(operatorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "not-allowed@finpay.test",
                          "password": "AnotherPassword123!",
                          "role": "MERCHANT_USER"
                        }
                        """));

        mockMvc.perform(patch("/api/payments/{id}/approve", paymentId.longValue())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        MerchantUser operatorMembership = merchantUserRepository
                .findAllByUserIdOrderByIdAsc(userRepository
                        .findByEmailIgnoreCase("rbac-operator@finpay.test")
                        .orElseThrow()
                        .getId())
                .getFirst();
        assertThat(operatorMembership.getMerchant().getId()).isEqualTo(merchantId);
        assertThat(operatorMembership.getRole()).isEqualTo(MerchantRole.MERCHANT_USER);
    }

    @Test
    void adminCanOnlyListUsersFromItsOwnMerchant() throws Exception {
        register("First Team", "first-team@finpay.test", "StrongPassword123!");
        String firstToken = login("first-team@finpay.test", "StrongPassword123!");
        createOperator(firstToken, "first-operator@finpay.test");

        register("Second Team", "second-team@finpay.test", "AnotherPassword456!");
        String secondToken = login("second-team@finpay.test", "AnotherPassword456!");

        mockMvc.perform(get("/api/merchant/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("second-team@finpay.test"));
    }

    @Test
    void validatesNewMerchantUserInput() throws Exception {
        register("Validation Team", "validation-admin@finpay.test", "StrongPassword123!");
        String adminToken = login("validation-admin@finpay.test", "StrongPassword123!");

        mockMvc.perform(post("/api/merchant/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "short",
                                  "role": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.role").value("Role is required"));
    }

    private void assertForbidden(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to access this resource"));
    }

    private Long register(String merchantName, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(merchantName, email, password)))
                .andExpect(status().isCreated())
                .andReturn();
        Number merchantId = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.merchantId");
        return merchantId.longValue();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private void createOperator(String adminToken, String email) throws Exception {
        mockMvc.perform(post("/api/merchant/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "OperatorPassword123!",
                                  "role": "MERCHANT_USER"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
