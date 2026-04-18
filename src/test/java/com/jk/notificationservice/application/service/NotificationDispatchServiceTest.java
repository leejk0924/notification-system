package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.out.NotificationSendPort;
import com.jk.notificationservice.application.port.out.RetryPolicyPort;
import com.jk.notificationservice.common.NotificationSendFailureException;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @InjectMocks
    private NotificationDispatchService sut;

    @Mock
    private NotificationFacade notificationFacade;

    @Mock
    private NotificationSendPort notificationSendPort;

    @Mock
    private RetryPolicyPort retryPolicyPort;

    @Nested
    @DisplayName("dispatch 메서드")
    class Dispatch {

        @Test
        @DisplayName("발송 성공 시 SENT로 전환된다")
        void dispatch_발송성공_SENT전환() {
            NotificationRequest processing = createProcessingRequest(0);
            given(notificationFacade.claimPendingForDispatch(any(), anyInt())).willReturn(List.of(processing));
            given(notificationFacade.save(any())).willAnswer(inv -> inv.getArgument(0));

            sut.dispatch();

            assertThat(processing.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(processing.getSentAt()).isNotNull();
            then(notificationFacade).should(times(1)).save(processing);
        }

        @Test
        @DisplayName("발송 실패 시 재시도 횟수를 증가시키고 PENDING으로 전환된다")
        void dispatch_일시적실패_재시도예약() {
            NotificationRequest processing = createProcessingRequest(0);
            LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(5);
            given(notificationFacade.claimPendingForDispatch(any(), anyInt())).willReturn(List.of(processing));
            given(notificationFacade.save(any())).willAnswer(inv -> inv.getArgument(0));
            willThrow(new NotificationSendFailureException("일시 오류", null)).given(notificationSendPort).send(any());
            given(retryPolicyPort.calculateNextRetryAt(anyInt())).willReturn(nextRetry);

            sut.dispatch();

            assertThat(processing.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(processing.getLastFailureReason()).isEqualTo("일시 오류");
            assertThat(processing.getRetryCount()).isEqualTo(1);
            assertThat(processing.getNextRetryAt()).isNotNull();
            then(notificationFacade).should(times(1)).save(processing);
        }

        @Test
        @DisplayName("재시도 횟수 초과 시 DEAD_LETTER로 전환된다")
        void dispatch_재시도초과_DEAD_LETTER전환() {
            NotificationRequest processing = createProcessingRequest(3);
            given(notificationFacade.claimPendingForDispatch(any(), anyInt())).willReturn(List.of(processing));
            given(notificationFacade.save(any())).willAnswer(inv -> inv.getArgument(0));
            willThrow(new NotificationSendFailureException("발송 실패", null)).given(notificationSendPort).send(any());

            sut.dispatch();

            assertThat(processing.getStatus()).isEqualTo(NotificationStatus.DEAD_LETTER);
            assertThat(processing.getLastFailureReason()).isEqualTo("발송 실패");
            then(notificationFacade).should(times(1)).save(processing);
        }

        @Test
        @DisplayName("발송 대상이 없으면 send와 save를 호출하지 않는다")
        void dispatch_대상없음_미호출() {
            given(notificationFacade.claimPendingForDispatch(any(), anyInt())).willReturn(List.of());

            sut.dispatch();

            then(notificationSendPort).should(never()).send(any());
            then(notificationFacade).should(never()).save(any());
        }
    }

    private static NotificationRequest createProcessingRequest(int retryCount) {
        return NotificationRequest.reconstruct(
                1L, 1L, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.EMAIL,
                NotificationStatus.PROCESSING, "test-key", "ENROLLMENT", 1L,
                null, null, LocalDateTime.now().plusHours(24),
                retryCount, 3,
                null, null, false, null, null, null, null, null
        );
    }
}