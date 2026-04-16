package com.jk.notificationservice.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRequestJpaRepository extends JpaRepository<NotificationRequestEntity, Long> {
}