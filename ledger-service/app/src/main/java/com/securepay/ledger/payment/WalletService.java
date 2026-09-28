package com.securepay.ledger.payment;

import com.securepay.ledger.domain.entity.Account;
import com.securepay.ledger.domain.entity.AuditLog;
import com.securepay.ledger.domain.entity.Wallet;
import com.securepay.ledger.domain.repository.AccountRepository;
import com.securepay.ledger.domain.repository.AuditLogRepository;
import com.securepay.ledger.domain.repository.WalletRepository;
import com.securepay.ledger.payment.dto.WalletResponse;
import com.securepay.ledger.payment.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class WalletService {

    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final AuditLogRepository auditLogRepository;

    public WalletService(AccountRepository accountRepository, WalletRepository walletRepository, AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByEmail(String userEmail) {
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada para o email: " + userEmail));

        Wallet wallet = walletRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada para a conta: " + account.getId()));

        return toResponse(wallet);
    }

    @Transactional
    public WalletResponse deposit(String userEmail, BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor de depósito inválido");
        }

        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada para o email: " + userEmail));

        Wallet wallet = walletRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada"));

        wallet.setBalance(wallet.getBalance().add(amount));
        if (currency != null && !currency.isBlank()) {
            wallet.setCurrency(currency);
        }
        Wallet updated = walletRepository.save(wallet);

        auditLogRepository.save(AuditLog.builder()
                .account(account)
                .action("DEPOSIT")
                .resource("wallet:" + wallet.getId())
                .timestamp(Instant.now())
                .build());

        return toResponse(updated);
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getAccount().getId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getVersion(),
                wallet.getUpdatedAt()
        );
    }
}
