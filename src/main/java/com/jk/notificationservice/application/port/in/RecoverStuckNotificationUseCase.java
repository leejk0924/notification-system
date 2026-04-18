package com.jk.notificationservice.application.port.in;

public interface RecoverStuckNotificationUseCase {

    /**
     * 일정 시간 이상 PROCESSING 상태로 멈춘 알림을 PENDING으로 복구한다.
     */
    void recover();
}
