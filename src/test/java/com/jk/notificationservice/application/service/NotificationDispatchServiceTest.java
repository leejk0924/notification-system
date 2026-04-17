package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.out.NotificationSendPort;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Nested
    @DisplayName("dispatch 메서드")
    class Dispatch {

        @Test
        @DisplayName("PENDING 알림을 발송하면 PROCESSING으로 변경되었다가 최종 SENT로 전환된다")
        void dispatch_발송성공_SENT전환() {
            // given
            NotificationRequest request = createPendingRequest();
            List<NotificationStatus> capturedStatuses = new ArrayList<>();

            given(notificationFacade.findPendingForDispatch(any(LocalDateTime.class), anyInt()))
                    .willReturn(List.of(request));
            given(notificationFacade.save(any())).willAnswer(inv -> {
                NotificationRequest req = inv.getArgument(0);
                capturedStatuses.add(req.getStatus());
                return req;
            });

            // when
            sut.dispatch();

            // then
            assertThat(capturedStatuses.get(0)).isEqualTo(NotificationStatus.PROCESSING);
            assertThat(capturedStatuses.get(1)).isEqualTo(NotificationStatus.SENT);
            assertThat(request.getSentAt()).isNotNull();
            then(notificationFacade).should(times(2)).save(any());
        }

        @Test
        @DisplayName("발송 실패 시 재시도 횟수를 증가시키고 PENDING으로 전환된다")
        void dispatch_일시적실패_재시도예약() {
            // given
            NotificationRequest request = createPendingRequest();
            List<NotificationStatus> capturedStatuses = new ArrayList<>();

            given(notificationFacade.findPendingForDispatch(any(LocalDateTime.class), anyInt()))
                    .willReturn(List.of(request));
            given(notificationFacade.save(any())).willAnswer(inv -> {
                NotificationRequest req = inv.getArgument(0);
                capturedStatuses.add(req.getStatus());
                return req;
            });
            willThrow(new NotificationSendFailureException("일시 오류", null)).given(notificationSendPort).send(any());

            // when
            sut.dispatch();

            // then
            assertThat(capturedStatuses.get(0)).isEqualTo(NotificationStatus.PROCESSING);
            assertThat(capturedStatuses.get(1)).isEqualTo(NotificationStatus.PENDING);
            assertThat(request.getLastFailureReason()).isEqualTo("일시 오류");
            assertThat(request.getRetryCount()).isEqualTo(1);
            assertThat(request.getNextRetryAt()).isNotNull();
            then(notificationFacade).should(times(2)).save(any());
        }

        @Test
        @DisplayName("재시도 횟수 초과 시 DEAD_LETTER로 전환된다")
        void dispatch_재시도초과_DEAD_LETTER전환() {
            NotificationRequest request = createExhaustedRequest();
            List<NotificationStatus> capturedStatuses = new ArrayList<>();
            given(notificationFacade.findPendingForDispatch(any(LocalDateTime.class), anyInt()))
                    .willReturn(List.of(request));
            given(notificationFacade.save(any())).willAnswer(inv ->
                    {
                        NotificationRequest req = inv.getArgument(0);
                        capturedStatuses.add(req.getStatus());
                        return req;
                    }
            );
            willThrow(new NotificationSendFailureException("발송 실패", null)).given(notificationSendPort).send(any());

            sut.dispatch();

            assertThat(capturedStatuses.get(0)).isEqualTo(NotificationStatus.PROCESSING);
            assertThat(capturedStatuses.get(1)).isEqualTo(NotificationStatus.DEAD_LETTER);
            assertThat(request.getLastFailureReason()).isEqualTo("발송 실패");
            then(notificationFacade).should(times(2)).save(any());
        }

        @Test
        @DisplayName("유효 기한이 초과된 알림은 EXPIRED로 전환된다")
        void dispatch_만료된알림_EXPIRED전환() {
            // given
            NotificationRequest request = createExpiredRequest();
            given(notificationFacade.findPendingForDispatch(any(LocalDateTime.class), anyInt()))
                    .willReturn(List.of(request));
            given(notificationFacade.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            sut.dispatch();

            // then
            assertThat(request.getStatus()).isEqualTo(NotificationStatus.EXPIRED);
            then(notificationSendPort).should(never()).send(any());
            then(notificationFacade).should(times(1)).save(request);
        }

        @Test
        @DisplayName("낙관적 잠금 충돌 시 해당 알림을 건너뛴다")
        void dispatch_낙관적잠금충돌_건너뜀() {
            // given
            NotificationRequest request = createPendingRequest();
            given(notificationFacade.findPendingForDispatch(any(LocalDateTime.class), anyInt()))
                    .willReturn(List.of(request));
            willThrow(new ObjectOptimisticLockingFailureException("", null))
                    .given(notificationFacade).save(any());

            // when
            sut.dispatch();

            // then
            then(notificationSendPort).should(never()).send(any());
            then(notificationFacade).should(times(1)).save(any());
        }
    }

    private static NotificationRequest createPendingRequest() {
        return NotificationRequest.create(
                1L, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.EMAIL,
                "test-key", 3, "ENROLLMENT", 1L, null, null
        );
    }

    private static NotificationRequest createExhaustedRequest() {
        return NotificationRequest.reconstruct(
                null, 1L, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.EMAIL,
                NotificationStatus.PENDING, "test-key", "ENROLLMENT", 1L,
                null, null, LocalDateTime.now().plusHours(24),
                3, 3,
                null, null, false, null, null, null, null, null
        );
    }

    private static NotificationRequest createExpiredRequest() {
        return NotificationRequest.reconstruct(
                null, 1L, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.EMAIL,
                NotificationStatus.PENDING, "test-key", "ENROLLMENT", 1L,
                null, null, LocalDateTime.now().minusHours(1),
                0, 3,
                null, null, false, null, null, null, null, null
        );
    }
}
