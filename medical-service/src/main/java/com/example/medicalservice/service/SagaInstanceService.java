package com.example.medicalservice.service;

import com.example.medicalservice.entity.SagaInstance;
import com.example.medicalservice.repository.SagaInstanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SagaInstanceService {
    
    @Autowired
    private SagaInstanceRepository sagaInstanceRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Create or get saga instance based on message ID (idempotency)
     */
    @Transactional
    public SagaInstance getOrCreateSagaInstance(String messageId, String sagaTopic, Map<String, Object> payload) {
        // Check if message already processed (idempotency)
        Optional<SagaInstance> existing = sagaInstanceRepository.findByMessageId(messageId);
        if (existing.isPresent()) {
            log.info("Saga instance already exists for messageId: {}, status: {}", messageId, existing.get().getSagaStatus());
            return existing.get();
        }
        
        // Create new saga instance
        SagaInstance sagaInstance = SagaInstance.builder()
            .sagaId(UUID.randomUUID().toString())
            .messageId(messageId)
            .sagaTopic(sagaTopic)
            .sagaStatus("INITIATED")
            .payloadJson(convertPayloadToJson(payload))
            .compensationStatus(null)
            .retryCount(0)
            .build();
        
        return sagaInstanceRepository.save(sagaInstance);
    }
    
    /**
     * Update saga status during processing
     */
    @Transactional
    public void updateSagaStatus(String sagaId, String status, String errorMessage) {
        Optional<SagaInstance> saga = sagaInstanceRepository.findBySagaId(sagaId);
        if (saga.isPresent()) {
            SagaInstance instance = saga.get();
            instance.setSagaStatus(status);
            instance.setErrorMessage(errorMessage);
            if ("COMPLETED".equals(status)) {
                instance.setCompletedAt(LocalDateTime.now());
            }
            sagaInstanceRepository.save(instance);
            log.info("Updated saga {} status to {}", sagaId, status);
        }
    }
    
    /**
     * Mark saga as failed and increment retry count
     */
    @Transactional
    public void markSagaFailed(String sagaId, String errorMessage) {
        Optional<SagaInstance> saga = sagaInstanceRepository.findBySagaId(sagaId);
        if (saga.isPresent()) {
            SagaInstance instance = saga.get();
            instance.setSagaStatus("FAILED");
            instance.setErrorMessage(errorMessage);
            instance.setRetryCount(instance.getRetryCount() + 1);
            sagaInstanceRepository.save(instance);
            log.error("Marked saga {} as FAILED. Retry count: {}. Error: {}", 
                sagaId, instance.getRetryCount(), errorMessage);
        }
    }
    
    /**
     * Mark saga for compensation
     */
    @Transactional
    public void markCompensationPending(String sagaId) {
        Optional<SagaInstance> saga = sagaInstanceRepository.findBySagaId(sagaId);
        if (saga.isPresent()) {
            SagaInstance instance = saga.get();
            instance.setCompensationStatus("PENDING");
            sagaInstanceRepository.save(instance);
            log.info("Marked saga {} for compensation", sagaId);
        }
    }
    
    /**
     * Mark compensation as executed
     */
    @Transactional
    public void markCompensationExecuted(String sagaId) {
        Optional<SagaInstance> saga = sagaInstanceRepository.findBySagaId(sagaId);
        if (saga.isPresent()) {
            SagaInstance instance = saga.get();
            instance.setCompensationStatus("EXECUTED");
            instance.setSagaStatus("COMPENSATED");
            instance.setCompletedAt(LocalDateTime.now());
            sagaInstanceRepository.save(instance);
            log.info("Marked saga {} compensation as EXECUTED", sagaId);
        }
    }
    
    /**
     * Check if message already processed (idempotency key)
     */
    public boolean isMessageAlreadyProcessed(String messageId) {
        return sagaInstanceRepository.findByMessageId(messageId).isPresent();
    }
    
    /**
     * Get saga instance by ID
     */
    public Optional<SagaInstance> getSagaInstance(String sagaId) {
        return sagaInstanceRepository.findBySagaId(sagaId);
    }
    
    /**
     * Get saga instance by message ID
     */
    public Optional<SagaInstance> getSagaInstanceByMessageId(String messageId) {
        return sagaInstanceRepository.findByMessageId(messageId);
    }
    
    private String convertPayloadToJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error converting payload to JSON", e);
            return "{}";
        }
    }
}
