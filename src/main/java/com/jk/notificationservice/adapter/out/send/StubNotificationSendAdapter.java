package com.jk.notificationservice.adapter.out.send;

import com.jk.notificationservice.application.port.out.NotificationSendPort;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StubNotificationSendAdapter implements NotificationSendPort {

    @Override
    public void send(NotificationRequest request) {
        log.info("알림 발송. id={}, channel={}, recipientId={}",
                request.getId(), request.getChannel(), request.getRecipientId());
    }
}
