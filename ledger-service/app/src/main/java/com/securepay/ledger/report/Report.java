package com.securepay.ledger.report;

import java.time.Instant;
import java.util.UUID;

public class Report {
    private final UUID id;
    private final String type;
    private final UUID accountId;
    private final Instant createdAt;
    private final String s3Key;

    public Report(UUID id, String type, UUID accountId, Instant createdAt, String s3Key) {
        this.id = id;
        this.type = type;
        this.accountId = accountId;
        this.createdAt = createdAt;
        this.s3Key = s3Key;
    }

    public UUID getId() { return id; }
    public String getType() { return type; }
    public UUID getAccountId() { return accountId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getS3Key() { return s3Key; }

    public static Report of(String type, UUID accountId) {
        return new Report(UUID.randomUUID(), type, accountId, Instant.now(), null);
    }

    public Report withS3Key(String s3Key) {
        return new Report(this.id, this.type, this.accountId, this.createdAt, s3Key);
    }
}
