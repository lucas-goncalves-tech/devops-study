package com.securepay.ledger.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "s3.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpReportRepository implements ReportRepository {
    private static final Logger log = LoggerFactory.getLogger(NoOpReportRepository.class);

    @Override
    public void save(Report report, byte[] content) {
        log.debug("S3 disabled. Skipping report save: id={}, type={}, size={} bytes",
                report.getId(), report.getType(), content != null ? content.length : 0);
    }

    @Override
    public byte[] findById(UUID id) {
        log.debug("S3 disabled. Returning empty content for report: {}", id);
        return new byte[0];
    }

    @Override
    public void delete(UUID id) {
        log.debug("S3 disabled. Skipping report deletion: {}", id);
    }
}
