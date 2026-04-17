package com.jk.notificationservice.adapter.out.send;

import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockEmailChannelSender implements ChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationRequest request) {
        log.info("[EMAIL Mock] 발송. id={}, type={}, recipientId={}, payload={}",
                request.getId(), request.getNotificationType(), request.getRecipientId(), request.getPayload());
    }
}
