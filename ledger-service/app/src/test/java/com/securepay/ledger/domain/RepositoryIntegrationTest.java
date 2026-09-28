package com.securepay.ledger.domain;

import com.securepay.ledger.domain.entity.*;
import com.securepay.ledger.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Deve persistir Account e Wallet associada com versionamento inicial 0")
    void shouldPersistAccountAndWallet() {
        Account account = Account.builder()
                .email("lucas@example.com")
                .passwordHash("$2a$10$dummyHash")
                .fullName("Lucas Tech")
                .role(AccountRole.ROLE_USER)
                .createdAt(Instant.now())
                .build();

        Account savedAccount = accountRepository.save(account);
        assertThat(savedAccount.getId()).isNotNull();

        Wallet wallet = Wallet.builder()
                .account(savedAccount)
                .balance(new BigDecimal("1000.00"))
                .currency("BRL")
                .updatedAt(Instant.now())
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        entityManager.flush();
        entityManager.clear();

        Wallet fetchedWallet = walletRepository.findById(savedWallet.getId()).orElseThrow();
        assertThat(fetchedWallet.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(fetchedWallet.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Deve rejeitar Transactions duplicadas com a mesma Idempotency Key (Unique Constraint)")
    void shouldRejectDuplicateTransactionIdempotencyKey() {
        Account acc = Account.builder()
                .email("trader@example.com")
                .passwordHash("$2a$10$hash")
                .fullName("Trader")
                .role(AccountRole.ROLE_USER)
                .createdAt(Instant.now())
                .build();
        accountRepository.save(acc);

        Wallet wallet1 = walletRepository.save(Wallet.builder().account(acc).balance(new BigDecimal("500.00")).currency("BRL").updatedAt(Instant.now()).build());
        Wallet wallet2 = walletRepository.save(Wallet.builder().account(acc).balance(new BigDecimal("100.00")).currency("BRL").updatedAt(Instant.now()).build());

        String idempotencyKey = "tx-unique-key-12345";

        Transaction tx1 = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .sourceWallet(wallet1)
                .destinationWallet(wallet2)
                .amount(new BigDecimal("50.00"))
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(tx1);
        entityManager.flush();

        Transaction tx2 = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .sourceWallet(wallet1)
                .destinationWallet(wallet2)
                .amount(new BigDecimal("50.00"))
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        assertThatThrownBy(() -> {
            transactionRepository.save(tx2);
            entityManager.flush();
        }).satisfies(ex -> assertThat(ex).isInstanceOfAny(
                DataIntegrityViolationException.class,
                org.hibernate.exception.ConstraintViolationException.class
        ));
    }
}
