package com.jk.notificationservice.adapter.out.send;

import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockInAppChannelSender implements ChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(NotificationRequest request) {
        log.info("[IN_APP Mock] 발송. id={}, type={}, recipientId={}, payload={}",
                request.getId(), request.getNotificationType(), request.getRecipientId(), request.getPayload());
    }
}
