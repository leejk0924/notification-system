package com.jk.notificationservice.application.port.in;

import com.jk.notificationservice.domain.event.NotificationEvent;

public interface RegisterNotificationUseCase {

    /**
     * 알림 이벤트를 수신하여 발송 요청을 등록한다.
     * 멱등성 키 중복 시 무시하고, 등록 실패 시 DeadLetter로 보관한다.
     *
     * @param event 등록할 알림 이벤트
     */
    void register(NotificationEvent event);
}