package com.securepay.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LedgerServiceApplicationTests {

    @Test
    @DisplayName("Garante que o Spring ApplicationContext inicializa com sucesso")
    void contextLoads() {
    }
}
