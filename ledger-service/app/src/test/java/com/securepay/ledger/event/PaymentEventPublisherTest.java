package com.securepay.ledger.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.ledger.domain.entity.Transaction;
import com.securepay.ledger.domain.entity.TransactionStatus;
import com.securepay.ledger.domain.entity.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("NoOpPaymentEventPublisher deve executar com segurança sem lançar exceções")
    void noOpPublisherShouldNotThrow() {
        NoOpPaymentEventPublisher publisher = new NoOpPaymentEventPublisher();

        assertThatCode(() -> publisher.publishPaymentProcessed(null)).doesNotThrowAnyException();

        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .amount(BigDecimal.valueOf(150.00))
                .status(TransactionStatus.COMPLETED)
                .build();

        assertThatCode(() -> publisher.publishPaymentProcessed(tx)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RedisPaymentEventPublisher deve ignorar transação nula sem chamar o Redis")
    void redisPublisherShouldIgnoreNullTransaction() {
        RedisPaymentEventPublisher publisher = new RedisPaymentEventPublisher(redisTemplate, objectMapper);

        assertThatCode(() -> publisher.publishPaymentProcessed(null)).doesNotThrowAnyException();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("RedisPaymentEventPublisher deve publicar evento no Redis Streams com payload JSON correto")
    @SuppressWarnings("unchecked")
    void redisPublisherShouldPublishFormattedEventToStream() throws Exception {
        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOps);
        when(streamOps.add(any(MapRecord.class))).thenReturn(RecordId.of("1700000000000-0"));

        RedisPaymentEventPublisher publisher = new RedisPaymentEventPublisher(redisTemplate, objectMapper);

        UUID txId = UUID.randomUUID();
        Wallet destWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .currency("BRL")
                .build();

        Transaction tx = Transaction.builder()
                .id(txId)
                .idempotencyKey("idemp-xyz-123")
                .destinationWallet(destWallet)
                .amount(BigDecimal.valueOf(250.50))
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.parse("2026-09-09T10:00:00Z"))
                .build();

        publisher.publishPaymentProcessed(tx);

        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());

        MapRecord<String, String, String> record = captor.getValue();
        assertThat(record.getStream()).isEqualTo("payment-events");

        Map<String, String> value = record.getValue();
        assertThat(value).containsKey("payload");

        Map<String, Object> payload = objectMapper.readValue(value.get("payload"), Map.class);
        assertThat(payload.get("orderId")).isEqualTo("idemp-xyz-123");
        assertThat(payload.get("amount")).isEqualTo(250.5);
        assertThat(payload.get("currency")).isEqualTo("BRL");
        assertThat(payload.get("status")).isEqualTo("PROCESSED");
        assertThat(payload.get("timestamp")).isEqualTo("2026-09-09T10:00:00Z");
        assertThat(payload.get("eventId")).asString().startsWith("evt_");
    }

    @Test
    @DisplayName("RedisPaymentEventPublisher deve capturar e tratar falhas de conexão no Redis sem abortar fluxo")
    @SuppressWarnings("unchecked")
    void redisPublisherShouldCatchAndHandleRedisExceptionsGracefully() {
        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOps);
        when(streamOps.add(any(MapRecord.class))).thenThrow(new RuntimeException("Redis connection refused"));

        RedisPaymentEventPublisher publisher = new RedisPaymentEventPublisher(redisTemplate, objectMapper);

        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .amount(BigDecimal.valueOf(50.00))
                .status(TransactionStatus.COMPLETED)
                .build();

        assertThatCode(() -> publisher.publishPaymentProcessed(tx)).doesNotThrowAnyException();
    }
}
