package com.jk.notificationservice.application.port.out;

import com.jk.notificationservice.domain.NotificationRequest;

public interface NotificationSendPort {

    /**
     * 알림을 실제 채널(EMAIL, IN_APP 등)로 발송한다.
     * 발송 실패 시 {@link com.jk.notificationservice.common.exception.NotificationException}(SEND_FAILURE)을 던진다.
     *
     * @param request 발송할 알림 요청
     */
    void send(NotificationRequest request);
}
