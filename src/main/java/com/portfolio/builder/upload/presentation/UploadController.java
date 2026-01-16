package com.portfolio.builder.upload.presentation;

import com.portfolio.builder.upload.application.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final S3Service s3Service;

    @PostMapping("/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @RequestBody Map<String, String> request) {
        String fileName = request.get("fileName");
        String contentType = request.get("contentType");

        if (fileName == null || contentType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "fileName and contentType are required"));
        }

        return ResponseEntity.ok(s3Service.generatePresignedUrl(fileName, contentType));
    }
}
