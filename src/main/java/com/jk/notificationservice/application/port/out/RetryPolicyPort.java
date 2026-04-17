package com.jk.notificationservice.application.port.out;

import java.time.LocalDateTime;

public interface RetryPolicyPort {

    LocalDateTime calculateNextRetryAt(int retryCount);
}
