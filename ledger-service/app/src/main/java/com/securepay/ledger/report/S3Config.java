package com.securepay.ledger.report;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "s3.enabled", havingValue = "true")
@EnableConfigurationProperties(S3ConfigProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3ConfigProperties config) {
        var builder = S3Client.builder()
                .region(Region.of(config.getRegion()));

        if (config.getEndpointUrl() != null && !config.getEndpointUrl().isBlank()) {
            builder.endpointOverride(URI.create(config.getEndpointUrl()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));
        }

        return builder.build();
    }
}
