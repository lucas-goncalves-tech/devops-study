package com.securepay.ledger.event;

import com.securepay.ledger.domain.entity.Transaction;

public interface PaymentEventPublisher {
    void publishPaymentProcessed(Transaction transaction);
}
