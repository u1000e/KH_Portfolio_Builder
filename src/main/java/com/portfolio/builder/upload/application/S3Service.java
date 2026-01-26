package com.portfolio.builder.upload.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    public Map<String, String> generatePresignedUrl(String fileName, String contentType) {
        // 파일명에서 확장자 추출
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex);
        }

        // 고유한 키 생성
        String key = "portfolios/" + UUID.randomUUID().toString() + extension;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);

        log.info("Generated presigned URL for file: {}", fileName);

        return Map.of(
                "uploadUrl", presignedUrl,
                "fileUrl", fileUrl,
                "key", key
        );
    }
}
