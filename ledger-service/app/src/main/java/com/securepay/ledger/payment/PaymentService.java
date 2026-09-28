package com.securepay.ledger.payment;

import com.securepay.ledger.domain.entity.Account;
import com.securepay.ledger.domain.entity.AuditLog;
import com.securepay.ledger.domain.entity.Transaction;
import com.securepay.ledger.domain.entity.TransactionStatus;
import com.securepay.ledger.domain.entity.Wallet;
import com.securepay.ledger.domain.repository.AccountRepository;
import com.securepay.ledger.domain.repository.AuditLogRepository;
import com.securepay.ledger.domain.repository.TransactionRepository;
import com.securepay.ledger.domain.repository.WalletRepository;
import com.securepay.ledger.payment.dto.TransactionResponse;
import com.securepay.ledger.payment.dto.TransferRequest;
import com.securepay.ledger.payment.exception.InsufficientFundsException;
import com.securepay.ledger.payment.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.securepay.ledger.event.PaymentEventPublisher paymentEventPublisher;

    public PaymentService(
            AccountRepository accountRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            AuditLogRepository auditLogRepository,
            com.securepay.ledger.event.PaymentEventPublisher paymentEventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    public TransactionResponse transfer(String senderEmail, TransferRequest request, String idempotencyKey) {
        // 1. Verificação de Idempotência
        final String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : UUID.randomUUID().toString();

        Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(effectiveKey);
        if (existingTx.isPresent()) {
            return toResponse(existingTx.get());
        }

        // 2. Busca e validação das contas e carteiras
        Account senderAccount = accountRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Conta remetente não encontrada: " + senderEmail));

        Wallet senderWallet = walletRepository.findByAccountId(senderAccount.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Carteira remetente não encontrada"));

        Wallet destinationWallet = walletRepository.findById(request.destinationWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Carteira de destino não encontrada: " + request.destinationWalletId()));

        if (senderWallet.getId().equals(destinationWallet.getId())) {
            throw new IllegalArgumentException("Transferências para a mesma carteira não são permitidas");
        }

        // 3. Validação de saldo
        if (senderWallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente. Saldo atual: " + senderWallet.getBalance() + ", Solicitado: " + request.amount());
        }

        // 4. Execução atômica (Débito e Crédito)
        senderWallet.setBalance(senderWallet.getBalance().subtract(request.amount()));
        destinationWallet.setBalance(destinationWallet.getBalance().add(request.amount()));

        walletRepository.save(senderWallet);
        walletRepository.save(destinationWallet);

        // 5. Registro da Transação
        Transaction tx = Transaction.builder()
                .idempotencyKey(effectiveKey)
                .sourceWallet(senderWallet)
                .destinationWallet(destinationWallet)
                .amount(request.amount())
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        Transaction savedTx = transactionRepository.save(tx);

        // 6. Log de Auditoria
        auditLogRepository.save(AuditLog.builder()
                .account(senderAccount)
                .action("TRANSFER_OUT")
                .resource("tx:" + savedTx.getId())
                .timestamp(Instant.now())
                .build());

        // 7. Event Dispatch
        paymentEventPublisher.publishPaymentProcessed(savedTx);

        return toResponse(savedTx);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada com id: " + id));
        return toResponse(tx);
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getIdempotencyKey(),
                tx.getSourceWallet().getId(),
                tx.getDestinationWallet().getId(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getFailureReason(),
                tx.getCreatedAt()
        );
    }
}
