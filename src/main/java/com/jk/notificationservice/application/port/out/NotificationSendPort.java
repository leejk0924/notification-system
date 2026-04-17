package com.jk.notificationservice.application.port.out;

import com.jk.notificationservice.domain.NotificationRequest;

public interface NotificationSendPort {
    void send(NotificationRequest request);
}
