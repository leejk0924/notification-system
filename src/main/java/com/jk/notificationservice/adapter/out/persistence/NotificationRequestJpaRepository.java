package com.jk.notificationservice.adapter.out.persistence;

import com.jk.notificationservice.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRequestJpaRepository extends JpaRepository<NotificationRequestEntity, Long> {

    Optional<NotificationRequestEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT e FROM NotificationRequestEntity e WHERE e.recipientId = :recipientId AND (:read IS NULL OR e.isRead = :read)")
    Page<NotificationRequestEntity> findByRecipientId(
            @Param("recipientId") Long recipientId,
            @Param("read") Boolean read,
            Pageable pageable
    );

    @Query("SELECT e FROM NotificationRequestEntity e " +
           "WHERE e.status = :status " +
           "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " +
           "ORDER BY e.nextRetryAt ASC")
    List<NotificationRequestEntity> findPendingForDispatch(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("SELECT e FROM NotificationRequestEntity e " +
           "WHERE e.status = 'PROCESSING' AND e.updatedAt < :stuckBefore")
    List<NotificationRequestEntity> findStuckProcessing(
            @Param("stuckBefore") LocalDateTime stuckBefore,
            Pageable pageable
    );
}