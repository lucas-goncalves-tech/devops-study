package com.securepay.ledger.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.ledger.payment.dto.DepositRequest;
import com.securepay.ledger.payment.dto.TransferRequest;
import com.securepay.ledger.security.dto.LoginRequest;
import com.securepay.ledger.security.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String user1Token;
    private String user1WalletId;

    private String user2Token;
    private String user2WalletId;

    @BeforeEach
    void setUp() throws Exception {
        // Register and login User 1
        String email1 = "alice-" + UUID.randomUUID() + "@example.com";
        MvcResult reg1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email1, "Pass12345", "Alice"))))
                .andExpect(status().isCreated())
                .andReturn();
        user1WalletId = objectMapper.readTree(reg1.getResponse().getContentAsString()).get("walletId").asText();

        MvcResult login1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email1, "Pass12345"))))
                .andExpect(status().isOk())
                .andReturn();
        user1Token = objectMapper.readTree(login1.getResponse().getContentAsString()).get("accessToken").asText();

        // Register and login User 2
        String email2 = "bob-" + UUID.randomUUID() + "@example.com";
        MvcResult reg2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email2, "Pass12345", "Bob"))))
                .andExpect(status().isCreated())
                .andReturn();
        user2WalletId = objectMapper.readTree(reg2.getResponse().getContentAsString()).get("walletId").asText();
    }

    @Test
    @DisplayName("Deve depositar fundos e consultar saldo atualizado na carteira")
    void shouldDepositAndCheckBalance() throws Exception {
        DepositRequest deposit = new DepositRequest(new BigDecimal("200.00"), "BRL");

        mockMvc.perform(post("/api/v1/wallets/deposit")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deposit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.00));

        mockMvc.perform(get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.00));
    }

    @Test
    @DisplayName("Deve realizar transferência atômica com sucesso entre duas carteiras")
    void shouldTransferFundsBetweenWallets() throws Exception {
        // Deposit 500 to User 1
        mockMvc.perform(post("/api/v1/wallets/deposit")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("500.00"), "BRL"))));

        TransferRequest transfer = new TransferRequest(UUID.fromString(user2WalletId), new BigDecimal("150.00"));
        String idempotencyKey = "tx-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Authorization", "Bearer " + user1Token)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(150.00));

        // Check User 1 balance (500 - 150 = 350)
        mockMvc.perform(get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(350.00));
    }

    @Test
    @DisplayName("Garante Idempotência estrita: reenviar requisição com mesma X-Idempotency-Key não duplica débito")
    void shouldBeStrictlyIdempotent() throws Exception {
        // Deposit 100 to User 1
        mockMvc.perform(post("/api/v1/wallets/deposit")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("100.00"), "BRL"))));

        TransferRequest transfer = new TransferRequest(UUID.fromString(user2WalletId), new BigDecimal("40.00"));
        String sameIdempotencyKey = "fixed-idem-key-" + UUID.randomUUID();

        // First call
        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Authorization", "Bearer " + user1Token)
                        .header("X-Idempotency-Key", sameIdempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Second call with same idempotency key (simulating network retry)
        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Authorization", "Bearer " + user1Token)
                        .header("X-Idempotency-Key", sameIdempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // User 1 balance MUST be 60.00 (100 - 40), NEVER 20.00!
        mockMvc.perform(get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(60.00));
    }

    @Test
    @DisplayName("Deve rejeitar transferência quando saldo for insuficiente retornando 422 Unprocessable Entity")
    void shouldRejectTransferWhenInsufficientBalance() throws Exception {
        TransferRequest transfer = new TransferRequest(UUID.fromString(user2WalletId), new BigDecimal("9999.00"));

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Authorization", "Bearer " + user1Token)
                        .header("X-Idempotency-Key", "tx-fail-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isUnprocessableEntity());
    }
}
