package com.tontinepro.tontinepro_backend.infrastructure.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinioConfig {

    @Value("${app.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.minio.access-key:tontinepro}")
    private String accessKey;

    @Value("${app.minio.secret-key:changeme-minio-secret}")
    private String secretKey;

    public static final String BUCKET_MEMBRES  = "tontinepro-membres";
    public static final String BUCKET_TONTINES = "tontinepro-tontines";

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @PostConstruct
    public void initBuckets() {
        // La création des buckets est gérée par MinioStorageService au premier upload
    }
}
