package com.jk.notificationservice.domain.event;

import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationType;

public record NotificationEvent(
        Long recipientId,
        NotificationType notificationType,
        NotificationChannel channel,
        String referenceType,
        Long referenceId
) {}