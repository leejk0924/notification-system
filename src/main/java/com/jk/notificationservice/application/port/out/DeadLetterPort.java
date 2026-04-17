package com.jk.notificationservice.application.port.out;

import com.jk.notificationservice.domain.event.NotificationEvent;

public interface DeadLetterPort {

    /**
     * 처리에 실패한 알림 이벤트를 Dead Letter로 보관한다.
     *
     * @param event         실패한 알림 이벤트
     * @param failureReason 실패 사유
     */
    void save(NotificationEvent event, String failureReason);
}