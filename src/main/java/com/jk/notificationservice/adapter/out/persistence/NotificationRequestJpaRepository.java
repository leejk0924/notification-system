package com.jk.notificationservice.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRequestJpaRepository extends JpaRepository<NotificationRequestEntity, Long> {

    Optional<NotificationRequestEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT e FROM NotificationRequestEntity e WHERE e.recipientId = :recipientId AND (:read IS NULL OR e.isRead = :read)")
    Page<NotificationRequestEntity> findByRecipientId(
            @Param("recipientId") Long recipientId,
            @Param("read") Boolean read,
            Pageable pageable
    );
}