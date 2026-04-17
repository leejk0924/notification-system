package com.jk.notificationservice.adapter.out.send;

import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;

public interface ChannelSender {

    NotificationChannel channel();

    void send(NotificationRequest request);
}
