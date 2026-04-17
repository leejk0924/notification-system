package com.jk.notificationservice.application.port.in;

import com.jk.notificationservice.domain.NotificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryNotificationUseCase {

    /**
     * ID로 알림 요청을 조회한다.
     *
     * @param id 알림 요청 ID
     * @return 알림 요청 도메인 객체
     */
    NotificationRequest findById(Long id);

    /**
     * 수신자 ID 기준으로 알림 목록을 페이징 조회한다.
     *
     * @param recipientId 수신자 ID
     * @param read        읽음 여부 필터 (null 이면 전체 조회)
     * @param pageable    페이징 정보
     * @return 알림 요청 페이지
     */
    Page<NotificationRequest> findByRecipientId(Long recipientId, Boolean read, Pageable pageable);
}