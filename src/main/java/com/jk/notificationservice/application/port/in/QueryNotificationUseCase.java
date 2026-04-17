package com.jk.notificationservice.application.port.in;

import com.jk.notificationservice.domain.NotificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryNotificationUseCase {

    NotificationRequest findById(Long id);

    Page<NotificationRequest> findByRecipientId(Long recipientId, Boolean read, Pageable pageable);
}