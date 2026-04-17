package com.jk.notificationservice.application.port.in;

import com.jk.notificationservice.domain.event.NotificationEvent;

public interface RegisterNotificationUseCase {

    void register(NotificationEvent event);
}