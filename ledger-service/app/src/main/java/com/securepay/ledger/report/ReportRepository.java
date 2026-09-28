package com.securepay.ledger.report;

import java.util.UUID;

public interface ReportRepository {
    void save(Report report, byte[] content);
    byte[] findById(UUID id);
    void delete(UUID id);
}
