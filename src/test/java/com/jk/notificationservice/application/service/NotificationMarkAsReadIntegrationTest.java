package com.jk.notificationservice.application.service;

import com.jk.notificationservice.TestcontainersConfiguration;
import com.jk.notificationservice.adapter.out.persistence.NotificationRequestEntity;
import com.jk.notificationservice.adapter.out.persistence.NotificationRequestJpaRepository;
import com.jk.notificationservice.application.port.in.MarkNotificationAsReadUseCase;
import com.jk.notificationservice.application.port.out.NotificationRepository;
import com.jk.notificationservice.common.exception.NotificationErrorCode;
import com.jk.notificationservice.common.exception.NotificationException;
import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;
import com.jk.notificationservice.domain.NotificationStatus;
import com.jk.notificationservice.domain.NotificationType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationMarkAsReadIntegrationTest {

    @Autowired
    private MarkNotificationAsReadUseCase markNotificationAsReadUseCase;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationRequestJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @AfterAll
    static void tearDown(@Autowired NotificationRequestJpaRepository jpaRepository) {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("SENT IN_APP 알림 읽음 처리 시 DB에 read=true, readAt이 기록된다")
    void markAsRead_SENT_IN_APP_DB_반영() {
        // given
        NotificationRequest saved = saveSentInApp("READ_IT:1", 1L);

        // when
        markNotificationAsReadUseCase.markAsRead(saved.getId(), 1L);

        // then
        jpaRepository.findById(saved.getId()).ifPresent(entity -> {
            assertThat(entity.isRead()).isTrue();
            assertThat(entity.getReadAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("이미 읽은 알림 재요청 시 readAt이 변경되지 않는다 (멱등)")
    void markAsRead_이미읽음_readAt_불변() {
        // given
        NotificationRequest saved = saveSentInApp("READ_IT:2", 1L);
        markNotificationAsReadUseCase.markAsRead(saved.getId(), 1L);
        var firstReadAt = jpaRepository.findById(saved.getId())
                .map(NotificationRequestEntity::getReadAt).orElseThrow();

        // when
        markNotificationAsReadUseCase.markAsRead(saved.getId(), 1L);

        // then
        jpaRepository.findById(saved.getId()).ifPresent(entity ->
                assertThat(entity.getReadAt()).isEqualTo(firstReadAt)
        );
    }

    @Test
    @DisplayName("PENDING 상태 알림 읽음 처리 시 INVALID_STATE 예외가 발생한다")
    void markAsRead_PENDING_상태_INVALID_STATE_예외() {
        // given
        NotificationRequest saved = notificationRepository.save(NotificationRequest.create(
                1L, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.IN_APP,
                "READ_IT:3", 3, null, null, null, null
        ));

        // when & then
        assertThatThrownBy(() -> markNotificationAsReadUseCase.markAsRead(saved.getId(), 1L))
                .isInstanceOf(NotificationException.class)
                .extracting(e -> ((NotificationException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.INVALID_STATE);
    }

    @Test
    @DisplayName("다른 수신자의 알림 읽음 처리 시 ACCESS_DENIED 예외가 발생한다")
    void markAsRead_다른_수신자_ACCESS_DENIED_예외() {
        // given
        NotificationRequest saved = saveSentInApp("READ_IT:4", 1L);

        // when & then — recipientId=999로 요청
        assertThatThrownBy(() -> markNotificationAsReadUseCase.markAsRead(saved.getId(), 999L))
                .isInstanceOf(NotificationException.class)
                .extracting(e -> ((NotificationException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 ID 읽음 처리 시 NOT_FOUND 예외가 발생한다")
    void markAsRead_없는_ID_NOT_FOUND_예외() {
        assertThatThrownBy(() -> markNotificationAsReadUseCase.markAsRead(999999L, 1L))
                .isInstanceOf(NotificationException.class)
                .extracting(e -> ((NotificationException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOT_FOUND);
    }

    private NotificationRequest saveSentInApp(String idempotencyKey, Long recipientId) {
        // given
        NotificationRequest request = NotificationRequest.create(
                recipientId, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.IN_APP,
                idempotencyKey, 3, null, null, null, null
        );
        ReflectionTestUtils.setField(request, "status", NotificationStatus.SENT);
        return notificationRepository.save(request);
    }
}
