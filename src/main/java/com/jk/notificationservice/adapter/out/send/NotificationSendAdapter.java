package com.jk.notificationservice.adapter.out.send;

import com.jk.notificationservice.application.port.out.NotificationSendPort;
import com.jk.notificationservice.common.NotificationSendFailureException;
import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NotificationSendAdapter implements NotificationSendPort {

    private final Map<NotificationChannel, ChannelSender> senders;

    public NotificationSendAdapter(List<ChannelSender> senders) {
        this.senders = senders.stream()
                .collect(Collectors.toMap(ChannelSender::channel, s -> s));
    }

    @Override
    public void send(NotificationRequest request) {
        ChannelSender sender = senders.get(request.getChannel());
        if (sender == null) {
            throw new NotificationSendFailureException("지원하지 않는 채널: " + request.getChannel(), null);
        }
        sender.send(request);
    }
}
