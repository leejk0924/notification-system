package com.jk.notificationservice.application.port.out;

import com.jk.notificationservice.domain.event.NotificationEvent;

public interface DeadLetterPort {

    void save(NotificationEvent event, String failureReason);
}