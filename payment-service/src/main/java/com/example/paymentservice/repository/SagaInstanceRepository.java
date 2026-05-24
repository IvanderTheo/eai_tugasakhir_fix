package com.example.paymentservice.repository;

import com.example.paymentservice.entity.SagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, Long> {
    
    Optional<SagaInstance> findBySagaId(String sagaId);
    
    Optional<SagaInstance> findByMessageId(String messageId);
    
    List<SagaInstance> findBySagaStatus(String sagaStatus);
    
    @Query("SELECT s FROM SagaInstance s WHERE s.sagaStatus = 'FAILED' AND s.retryCount < :maxRetries")
    List<SagaInstance> findFailedSagasForRetry(@Param("maxRetries") Integer maxRetries);
    
    @Query("SELECT s FROM SagaInstance s WHERE s.compensationStatus = 'PENDING'")
    List<SagaInstance> findPendingCompensations();
    
    @Query("SELECT s FROM SagaInstance s WHERE s.createdAt < :before AND s.sagaStatus IN ('COMPLETED', 'FAILED')")
    List<SagaInstance> findCompletedSagasOlderThan(@Param("before") LocalDateTime before);
}
