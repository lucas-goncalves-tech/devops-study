package com.securepay.ledger.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.ledger.domain.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisPaymentEventPublisher implements PaymentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(RedisPaymentEventPublisher.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisPaymentEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishPaymentProcessed(Transaction transaction) {
        if (transaction == null) {
            log.warn("Cannot publish event for null transaction");
            return;
        }
        try {
            String currency = (transaction.getDestinationWallet() != null && transaction.getDestinationWallet().getCurrency() != null)
                    ? transaction.getDestinationWallet().getCurrency()
                    : "BRL";

            String orderId = (transaction.getIdempotencyKey() != null)
                    ? transaction.getIdempotencyKey()
                    : (transaction.getId() != null ? transaction.getId().toString() : "tx_unknown");

            String timestamp = (transaction.getCreatedAt() != null)
                    ? transaction.getCreatedAt().toString()
                    : Instant.now().toString();

            double amount = (transaction.getAmount() != null)
                    ? transaction.getAmount().doubleValue()
                    : 0.0;

            String jsonPayload = objectMapper.writeValueAsString(Map.of(
                    "eventId", "evt_" + System.currentTimeMillis(),
                    "orderId", orderId,
                    "amount", amount,
                    "currency", currency,
                    "status", "PROCESSED",
                    "timestamp", timestamp
            ));

            RecordId recordId = redisTemplate.opsForStream().add(
                    StreamRecords.string(Map.of("payload", jsonPayload)).withStreamKey("payment-events")
            );
            log.info("Published payment event to stream payment-events: {}", recordId);
        } catch (Exception e) {
            log.error("Failed to publish payment event to Redis", e);
        }
    }
}
