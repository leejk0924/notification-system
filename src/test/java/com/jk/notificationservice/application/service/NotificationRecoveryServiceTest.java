package com.jk.notificationservice.application.service;

import com.jk.notificationservice.config.NotificationSchedulerProperties;
import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;
import com.jk.notificationservice.domain.NotificationStatus;
import com.jk.notificationservice.domain.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRecoveryServiceTest {

    @InjectMocks
    private NotificationRecoveryService sut;

    @Mock
    private NotificationFacade notificationFacade;

    @Mock
    private NotificationSchedulerProperties properties;

    @Nested
    @DisplayName("recover 메서드")
    class Recover {

        @Test
        @DisplayName("stuck 건이 없으면 save를 호출하지 않는다")
        void recover_stuck없음_save미호출() {
            // given
            given(notificationFacade.findStuckProcessing(any(), anyInt()))
                    .willReturn(List.of());
            given(properties.stuckThreshold()).willReturn(Duration.ofMinutes(5));

            // when
            sut.recover();

            // then
            then(notificationFacade).should(never()).save(any());
        }

        @Test
        @DisplayName("stuck PROCESSING 건을 PENDING으로 복구하고 저장한다")
        void recover_stuck있음_PENDING복구() {
            // given
            NotificationRequest stuck = createProcessingRequest(1L, 0);
            given(notificationFacade.findStuckProcessing(any(), anyInt())).willReturn(List.of(stuck));
            given(notificationFacade.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(properties.stuckThreshold()).willReturn(Duration.ofMinutes(5));

            // when
            sut.recover();

            // then
            assertThat(stuck.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(stuck.getNextRetryAt()).isNull();
            then(notificationFacade).should(times(1)).save(stuck);
        }

        @Test
        @DisplayName("여러 stuck 건을 모두 복구한다")
        void recover_여러건_모두복구() {
            // given
            NotificationRequest stuck1 = createProcessingRequest(1L, 0);
            NotificationRequest stuck2 = createProcessingRequest(2L, 1);
            given(notificationFacade.findStuckProcessing(any(), anyInt())).willReturn(List.of(stuck1, stuck2));
            given(notificationFacade.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(properties.stuckThreshold()).willReturn(Duration.ofMinutes(5));

            // when
            sut.recover();

            // then
            assertThat(stuck1.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(stuck1.getNextRetryAt()).isNull();
            assertThat(stuck2.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(stuck2.getNextRetryAt()).isNull();
            then(notificationFacade).should(times(2)).save(any());
        }

        @Test
        @DisplayName("복구 시 retryCount는 변경되지 않는다")
        void recover_retryCount유지() {
            // given
            NotificationRequest stuck = createProcessingRequest(1L, 2);
            given(notificationFacade.findStuckProcessing(any(), anyInt())).willReturn(List.of(stuck));
            given(notificationFacade.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(properties.stuckThreshold()).willReturn(Duration.ofMinutes(5));

            // when
            sut.recover();

            // then
            assertThat(stuck.getRetryCount()).isEqualTo(2);
            then(notificationFacade).should(times(1)).save(any());
        }
    }

    private static NotificationRequest createProcessingRequest(Long id, int retryCount) {
        return NotificationRequest.reconstruct(
                id, 1L, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.EMAIL,
                NotificationStatus.PROCESSING, "test-key-" + id, "ENROLLMENT", 1L,
                null, null, LocalDateTime.now().plusHours(24),
                retryCount, 3, null, null,
                false, null, null, null, null, null
        );
    }
}
