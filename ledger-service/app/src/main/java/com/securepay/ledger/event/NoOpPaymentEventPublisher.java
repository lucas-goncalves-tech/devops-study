package com.securepay.ledger.event;

import com.securepay.ledger.domain.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPaymentEventPublisher implements PaymentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(NoOpPaymentEventPublisher.class);

    @Override
    public void publishPaymentProcessed(Transaction transaction) {
        if (transaction != null) {
            log.debug("Redis disabled. Skipping event publication for transaction: {}", transaction.getId());
        } else {
            log.debug("Redis disabled. Skipping event publication for null transaction");
        }
    }
}
