package com.jk.notificationservice.adapter.out.policy;

import com.jk.notificationservice.application.port.out.RetryPolicyPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ExponentialBackoffRetryPolicyAdapter implements RetryPolicyPort {

    @Override
    public LocalDateTime calculateNextRetryAt(int retryCount) {
        // 지수 백오프: 2^retryCount * 60초 (최대 30분)
        long delaySeconds = Math.min((long) Math.pow(2, retryCount) * 60L, 1800L);
        // 지터(Jitter): 0~9초 무작위 지연 추가 (경합 방지)
        long jitter = (long) (Math.random() * 10);
        
        return LocalDateTime.now().plusSeconds(delaySeconds + jitter);
    }
}
