package com.jk.notificationservice.application.port.out;

import com.jk.notificationservice.domain.NotificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    /**
     * 알림 요청을 저장하고 저장된 객체를 반환한다.
     * 낙관적 잠금 버전이 즉시 반영된 객체를 반환한다.
     *
     * @param request 저장할 알림 요청
     * @return 저장된 알림 요청
     */
    NotificationRequest save(NotificationRequest request);

    /**
     * ID로 알림 요청을 조회한다.
     *
     * @param id 알림 요청 ID
     * @return 알림 요청 (없으면 empty)
     */
    Optional<NotificationRequest> findById(Long id);

    /**
     * 멱등성 키로 알림 요청을 조회한다.
     *
     * @param idempotencyKey 멱등성 키
     * @return 알림 요청 (없으면 empty)
     */
    Optional<NotificationRequest> findByIdempotencyKey(String idempotencyKey);

    /**
     * 수신자 ID 기준으로 알림 목록을 페이징 조회한다.
     *
     * @param recipientId 수신자 ID
     * @param read        읽음 여부 필터 (null 이면 전체 조회)
     * @param pageable    페이징 정보
     * @return 알림 요청 페이지
     */
    Page<NotificationRequest> findByRecipientId(Long recipientId, Boolean read, Pageable pageable);

    /**
     * 발송 대기 중인 PENDING 알림을 배치 크기만큼 조회한다.
     * nextRetryAt이 현재 시각 이전이거나 null인 알림만 대상으로 한다.
     *
     * @param now   현재 시각
     * @param limit 최대 조회 건수
     * @return 발송 대상 알림 목록
     */
    List<NotificationRequest> findPendingForDispatch(LocalDateTime now, int limit);
}