package com.portfolio.builder.global.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    /**
     * ALB 헬스체크용 - 단순 응답
     * Target Group 헬스체크 경로: /health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }

    /**
     * 상세 헬스체크 - DB 연결 포함
     */
    @GetMapping("/health/detail")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        
        // DB 연결 체크
        Map<String, Object> db = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            db.put("status", "UP");
            db.put("database", conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
            health.put("status", "DOWN");
        }
        health.put("database", db);
        
        return ResponseEntity.ok(health);
    }
}
