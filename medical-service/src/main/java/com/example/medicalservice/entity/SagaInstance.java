package com.example.medicalservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_instances", indexes = {
    @Index(name = "idx_saga_id", columnList = "sagaId"),
    @Index(name = "idx_message_id", columnList = "messageId"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String sagaId;
    
    @Column(nullable = false)
    private String messageId;
    
    @Column(nullable = false)
    private String sagaTopic;
    
    @Column(nullable = false)
    private String sagaStatus; // INITIATED, IN_PROGRESS, COMPLETED, FAILED, COMPENSATED
    
    @Column(columnDefinition = "LONGTEXT")
    private String payloadJson;
    
    @Column
    private String compensationStatus; // null, PENDING, EXECUTED, FAILED
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column
    private Integer retryCount;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
