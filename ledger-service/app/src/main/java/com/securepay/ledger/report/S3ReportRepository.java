package com.securepay.ledger.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "s3.enabled", havingValue = "true")
public class S3ReportRepository implements ReportRepository {
    private static final Logger log = LoggerFactory.getLogger(S3ReportRepository.class);
    private final S3Client s3Client;
    private final S3ConfigProperties s3Config;

    public S3ReportRepository(S3Client s3Client, S3ConfigProperties s3Config) {
        this.s3Client = s3Client;
        this.s3Config = s3Config;
    }

    @Override
    public void save(Report report, byte[] content) {
        String key = buildKey(report);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        log.info("Saved report to S3: bucket={}, key={}, size={} bytes",
                s3Config.getBucketName(), key, content.length);
    }

    @Override
    public byte[] findById(UUID id) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(id.toString())
                .build();

        byte[] content = s3Client.getObject(request, ResponseTransformer.toBytes()).asByteArray();
        log.debug("Loaded report from S3: bucket={}, key={}, size={} bytes",
                s3Config.getBucketName(), id, content.length);
        return content;
    }

    @Override
    public void delete(UUID id) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(id.toString())
                .build();

        s3Client.deleteObject(request);
        log.info("Deleted report from S3: bucket={}, key={}", s3Config.getBucketName(), id);
    }

    private String buildKey(Report report) {
        return report.getType() + "/" + report.getId();
    }
}
