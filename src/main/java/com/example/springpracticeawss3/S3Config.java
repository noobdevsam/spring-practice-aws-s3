package com.example.springpracticeawss3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

@Configuration
public class S3Config {

    private final Region region = Region.US_EAST_1; // Change to your target region

    @Value("${storage.s3.access-key}")
    private String accessKey;

    @Value("${storage.s3.secret-key}")
    private String secretKey;

    @Value("${storage.s3.endpoint}")
    private String endpoint;

    @Value("${storage.s3.path-style-access-enabled:}")
    private boolean pathStyleAccessEnabled;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(region)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(pathStyleAccessEnabled) // Path-based access enabled
                                .chunkedEncodingEnabled(true) // Enable chunked encoding for large file uploads
                                .build())
                .overrideConfiguration((builder) -> {
                    builder.apiCallTimeout(Duration.ofMinutes(5))
                            .apiCallAttemptTimeout(Duration.ofMinutes(1))
                            .retryPolicy()
                            .toBuilder()
                            .build();
                })
                .endpointOverride(URI.create(endpoint)) // Custom endpoint for S3-compatible storage
                .build();
    }

    // Advanced: Async Client required for non-blocking I/O and Transfer Manager
    @Bean
    public S3AsyncClient s3AsyncClient() {
        return S3AsyncClient.builder()
                .region(region)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .crossRegionAccessEnabled(true)
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                .overrideConfiguration((builder) -> {
                    builder.apiCallTimeout(Duration.ofMinutes(5))
                            .apiCallAttemptTimeout(Duration.ofMinutes(1))
                            .retryPolicy()
                            .toBuilder()
                            .build();
                })
                .endpointOverride(URI.create(endpoint)) // Custom endpoint for S3-compatible storage
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccessEnabled)
                        .build())
                .endpointOverride(URI.create(endpoint)) // Custom endpoint for S3-compatible storage
                .build();
    }

}