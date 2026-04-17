package com.jk.notificationservice.adapter.in.scheduler;

import com.jk.notificationservice.adapter.out.persistence.NotificationRequestJpaRepository;
import com.jk.notificationservice.application.port.out.NotificationRepository;
import com.jk.notificationservice.application.port.out.NotificationSendPort;
import com.jk.notificationservice.common.NotificationSendFailureException;
import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;
import com.jk.notificationservice.domain.NotificationStatus;
import com.jk.notificationservice.domain.NotificationType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.jk.notificationservice.TestcontainersConfiguration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationDispatchWorkerIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationRequestJpaRepository jpaRepository;

    @MockitoBean
    private NotificationSendPort notificationSendPort;

    @BeforeEach
    void setUp() {
        Mockito.reset(notificationSendPort);
    }

    @AfterAll
    static void tearDown(@Autowired NotificationRequestJpaRepository jpaRepository) {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("PENDING 알림을 스케줄러가 자동 발송하여 SENT로 전환된다")
    void dispatch_PENDING_알림_SENT전환() {
        // given
        String idempotencyKey = "DISPATCH_IT:ENROLLMENT:200:1:EMAIL";
        notificationRepository.save(NotificationRequest.create(
                1L, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.EMAIL,
                idempotencyKey, 3, "ENROLLMENT", 200L, null, null
        ));

        // when && then
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(jpaRepository.findByIdempotencyKey(idempotencyKey))
                        .isPresent()
                        .hasValueSatisfying(entity -> {
                            assertThat(entity.getStatus()).isEqualTo(NotificationStatus.SENT);
                            assertThat(entity.getSentAt()).isNotNull();
                        })
        );
    }

    @Test
    @DisplayName("발송 실패 횟수 초과 시 스케줄러가 DEAD_LETTER로 전환된다")
    void dispatch_재시도초과_DEAD_LETTER전환() {
        // given
        willThrow(new NotificationSendFailureException("발송 실패", null)).given(notificationSendPort).send(any());

        String idempotencyKey = "DISPATCH_IT:ENROLLMENT:201:1:EMAIL";
        notificationRepository.save(NotificationRequest.create(
                1L, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.EMAIL,
                idempotencyKey, 0, "ENROLLMENT", 201L, null, null  // maxRetryCount=0 → 첫 실패에 DEAD_LETTER
        ));

        // when && then
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(jpaRepository.findByIdempotencyKey(idempotencyKey))
                        .isPresent()
                        .hasValueSatisfying(entity -> {
                            assertThat(entity.getStatus()).isEqualTo(NotificationStatus.DEAD_LETTER);
                            assertThat(entity.getLastFailureReason()).isEqualTo("발송 실패");
                        })
        );
    }
}
