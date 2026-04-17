package com.jk.notificationservice.application.port.out;

import java.time.LocalDateTime;

public interface RetryPolicyPort {
    /**
     * 재시도 횟수에 따른 다음 재시도 시간을 계산합니다.
     * @param retryCount 현재까지의 재시도 횟수
     * @return 다음 재시도 시간
     */
    LocalDateTime calculateNextRetryAt(int retryCount);
}
