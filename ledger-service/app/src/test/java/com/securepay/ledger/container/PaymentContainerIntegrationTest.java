package com.securepay.ledger.container;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.ledger.payment.dto.DepositRequest;
import com.securepay.ledger.payment.dto.TransferRequest;
import com.securepay.ledger.security.dto.LoginRequest;
import com.securepay.ledger.security.dto.RegisterRequest;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaymentContainerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("securepay_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Garante ciclo completo de pagamento persistido em contêiner PostgreSQL real via Testcontainers")
    void shouldExecuteFullPaymentCycleWithRealPostgreSqlContainer() throws Exception {
        // 1. Cadastrar Usuário A e obter carteira
        String emailA = "user-a-" + UUID.randomUUID() + "@corp.com";
        MvcResult regResultA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(emailA, "SecretA123", "User A"))))
                .andExpect(status().isCreated())
                .andReturn();
        String walletAId = objectMapper.readTree(regResultA.getResponse().getContentAsString()).get("walletId").asText();

        // 2. Login Usuário A
        MvcResult loginResultA = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(emailA, "SecretA123"))))
                .andExpect(status().isOk())
                .andReturn();
        String tokenA = objectMapper.readTree(loginResultA.getResponse().getContentAsString()).get("accessToken").asText();

        // 3. Cadastrar Usuário B e obter carteira
        String emailB = "user-b-" + UUID.randomUUID() + "@corp.com";
        MvcResult regResultB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(emailB, "SecretB123", "User B"))))
                .andExpect(status().isCreated())
                .andReturn();
        String walletBId = objectMapper.readTree(regResultB.getResponse().getContentAsString()).get("walletId").asText();

        // 4. Depositar 1000.00 na carteira de A
        mockMvc.perform(post("/api/v1/wallets/deposit")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("1000.00"), "BRL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.00));

        // 5. Transferir 350.00 de A para B com idempotência
        String idemKey = "real-pg-tx-" + UUID.randomUUID();
        TransferRequest transferRequest = new TransferRequest(UUID.fromString(walletBId), new BigDecimal("350.00"));

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(350.00));

        // 6. Validar saldo final persistido na carteira de A (1000 - 350 = 650)
        mockMvc.perform(get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(650.00));
    }
}
