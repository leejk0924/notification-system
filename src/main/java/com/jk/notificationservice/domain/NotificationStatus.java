package com.jk.notificationservice.domain;

import lombok.RequiredArgsConstructor;

/**
 * 알림 요청의 처리 상태를 나타낸다.
 *
 * <p>상태 전이 흐름:
 * <pre>
 * PENDING → PROCESSING → SENT
 *                      → FAILED        (영구 실패 — 재시도 불가)
 *                      → PENDING       (일시 실패 — 재시도 예약)
 *                      → DEAD_LETTER   (재시도 횟수 소진)
 * PENDING → EXPIRED                    (유효 기한 초과)
 * </pre>
 */
@RequiredArgsConstructor
public enum NotificationStatus {

    /**
     * 발송 대기 중.
     * 최초 등록 시 또는 재시도 예약 후의 상태.
     */
    PENDING(false),

    /**
     * 워커가 처리 중.
     * {@code SKIP LOCKED}로 점유된 상태이며, 일정 시간 이상 유지되면 스케줄러가 {@code PENDING}으로 복구한다.
     */
    PROCESSING(false),

    /**
     * 발송 완료.
     */
    SENT(true),

    /**
     * 영구 실패.
     * 재시도해도 동일하게 실패하는 케이스 (잘못된 수신자, 페이로드 오류 등).
     */
    FAILED(true),

    /**
     * 재시도 횟수 소진.
     * 외부 서비스 복구 후 수동 재시도 대상.
     */
    DEAD_LETTER(true),

    /**
     * 유효 기한 초과.
     * {@code expire_at} 초과 시 전이되며, 발송 의미가 없는 상태.
     */
    EXPIRED(true);

    /**
     * 처리 파이프라인 종료 여부.
     *
     * <p>{@code true}: 워커가 더 이상 처리하지 않는 최종 상태 (SENT, FAILED, DEAD_LETTER, EXPIRED)
     * <p>{@code false}: 처리가 남아있는 진행 중 상태 (PENDING, PROCESSING)
     *
     * <p>알림 자체의 성공/실패가 아니라 파이프라인이 끝났는지 여부를 나타낸다.
     */
    private final boolean terminal;

    /**
     * 더 이상 처리가 필요 없는 최종 상태 여부를 반환한다.
     */
    public boolean isTerminal() {
        return terminal;
    }
}