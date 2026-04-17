package com.jk.notificationservice.application.port.in;

public interface DispatchNotificationUseCase {

    /**
     * PENDING 상태의 알림을 일괄 조회하여 발송 처리한다.
     * 만료·낙관적 잠금 충돌·재시도 초과 등 각 케이스를 내부에서 처리한다.
     */
    void dispatch();
}
