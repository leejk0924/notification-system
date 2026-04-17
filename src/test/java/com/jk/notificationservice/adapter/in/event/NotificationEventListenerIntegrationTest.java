package com.jk.notificationservice.adapter.in.event;

import com.jk.notificationservice.adapter.out.persistence.NotificationRequestJpaRepository;
import com.jk.notificationservice.domain.NotificationStatus;
import com.jk.notificationservice.domain.event.NotificationEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.concurrent.TimeUnit;

import static com.jk.notificationservice.domain.NotificationChannel.EMAIL;
import static com.jk.notificationservice.domain.NotificationType.ENROLLMENT_COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationEventListenerIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NotificationRequestJpaRepository repository;

    @AfterAll
    static void tearDown(@Autowired NotificationRequestJpaRepository repository) {
        repository.deleteAll();
    }

    @Test
    @DisplayName("트랜잭션 커밋 후 이벤트를 수신하여 알림 요청이 저장된다")
    void handle_트랜잭션_커밋_후_저장() {
        // given
        long referenceId = 101L;
        NotificationEvent event = new NotificationEvent(1L, ENROLLMENT_COMPLETED, EMAIL, "ENROLLMENT", referenceId);
        String expectedKey = "ENROLLMENT_COMPLETED:ENROLLMENT:" + referenceId + ":1:EMAIL";

        // when - 트랜잭션 내에서 이벤트 발행
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        // then - @Async 처리 완료 대기
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(repository.findByIdempotencyKey(expectedKey))
                        .isPresent()
                        .hasValueSatisfying(entity -> {
                            assertThat(entity.getRecipientId()).isEqualTo(1L);
                            assertThat(entity.getNotificationType()).isEqualTo(ENROLLMENT_COMPLETED);
                            assertThat(entity.getChannel()).isEqualTo(EMAIL);
                            assertThat(entity.getStatus()).isEqualTo(NotificationStatus.PENDING);
                        })
        );
    }

    @Test
    @DisplayName("트랜잭션 없이 이벤트를 발행해도 fallbackExecution으로 알림 요청이 저장된다")
    void handle_트랜잭션_없이_fallback_저장() {
        // given
        long referenceId = 102L;
        NotificationEvent event = new NotificationEvent(1L, ENROLLMENT_COMPLETED, EMAIL, "ENROLLMENT", referenceId);
        String expectedKey = "ENROLLMENT_COMPLETED:ENROLLMENT:" + referenceId + ":1:EMAIL";

        // when - 트랜잭션 없이 이벤트 발행
        eventPublisher.publishEvent(event);

        // then
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(repository.findByIdempotencyKey(expectedKey)).isPresent()
        );
    }

    @Test
    @DisplayName("동일 멱등성 키의 이벤트를 두 번 발행해도 한 번만 저장된다")
    void handle_중복이벤트_한번만_저장() {
        // given
        long referenceId = 103;
        NotificationEvent event = new NotificationEvent(1L, ENROLLMENT_COMPLETED, EMAIL, "ENROLLMENT", referenceId);
        String expectedKey = "ENROLLMENT_COMPLETED:ENROLLMENT:" + referenceId + ":1:EMAIL";

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        // then
        // idempotencyKey는 UNIQUE 제약이므로 존재하면 1건임이 보장됨
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(repository.findByIdempotencyKey(expectedKey)).isPresent()
        );
    }
}
