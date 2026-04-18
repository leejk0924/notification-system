package com.jk.notificationservice.application.service;


import com.jk.notificationservice.common.exception.NotificationException;
import com.jk.notificationservice.common.exception.NotificationErrorCode;
import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;
import com.jk.notificationservice.domain.NotificationStatus;
import com.jk.notificationservice.domain.NotificationType;
import static org.mockito.Mockito.times;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class NotificationReadCommandServiceTest {

    private final NotificationFacade notificationFacade = mock(NotificationFacade.class);
    private final NotificationReadCommandService sut = new NotificationReadCommandService(notificationFacade);

    @DisplayName("markAsRead 메서드")
    @Nested
    class MarkAsRead {

        @Test
        @DisplayName("미읽음 IN_APP 알림을 읽음 처리하면 저장된 결과를 반환한다")
        void markAsRead_미읽음_IN_APP_성공() {
            // given
            NotificationRequest request = inAppRequest(1L, false);
            NotificationRequest saved = inAppRequest(1L, true);
            given(notificationFacade.findById(1L)).willReturn(request);
            given(notificationFacade.save(request)).willReturn(saved);

            // when
            NotificationRequest result = sut.markAsRead(1L, 1L);

            // then
            assertThat(result.isRead()).isTrue();
            then(notificationFacade).should().save(request);
        }

        @Test
        @DisplayName("이미 읽음 상태인 알림은 save 없이 현재 상태를 반환한다 (멱등)")
        void markAsRead_이미읽음_멱등() {
            // given
            NotificationRequest request = inAppRequest(1L, true);
            given(notificationFacade.findById(1L)).willReturn(request);

            // when
            NotificationRequest result = sut.markAsRead(1L, 1L);

            // then
            assertThat(result.isRead()).isTrue();
            then(notificationFacade).should(never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 알림 ID면 NotificationNotFoundException이 발생한다")
        void markAsRead_없는_ID_예외() {
            // given
            given(notificationFacade.findById(99L))
                    .willThrow(new NotificationException(NotificationErrorCode.NOT_FOUND, "알림 요청을 찾을 수 없습니다. id: 99"));

            // when & then
            assertThatThrownBy(() -> sut.markAsRead(99L, 1L))
                    .isInstanceOf(NotificationException.class);
        }

        @Test
        @DisplayName("요청자의 recipientId가 알림의 recipientId와 다르면 NotificationAccessDeniedException이 발생한다")
        void markAsRead_다른_수신자_예외() {
            // given
            NotificationRequest request = inAppRequest(1L, false); // recipientId = 1L
            given(notificationFacade.findById(1L)).willReturn(request);

            // when & then
            assertThatThrownBy(() -> sut.markAsRead(1L, 999L))
                    .isInstanceOf(NotificationException.class);
        }

        @Test
        @DisplayName("낙관적 잠금 충돌 시 최신 상태를 재조회해 반환한다 (동시 요청 멱등)")
        void markAsRead_낙관적잠금_충돌_재조회() {
            // given
            NotificationRequest request = inAppRequest(1L, false);
            NotificationRequest latest = inAppRequest(1L, true);
            given(notificationFacade.findById(1L))
                    .willReturn(request)
                    .willReturn(latest);
            given(notificationFacade.save(request))
                    .willThrow(new ObjectOptimisticLockingFailureException(NotificationRequest.class, 1L));

            // when
            NotificationRequest result = sut.markAsRead(1L, 1L);

            // then
            assertThat(result.isRead()).isTrue();
            then(notificationFacade).should(times(2)).findById(1L);
        }

        @Test
        @DisplayName("없는 ID 예외의 errorCode는 NOT_FOUND이다")
        void markAsRead_없는_ID_예외_NOT_FOUND_errorCode() {
            // given
            given(notificationFacade.findById(99L))
                    .willThrow(new NotificationException(NotificationErrorCode.NOT_FOUND, "알림 요청을 찾을 수 없습니다. id: 99"));

            // when & then
            assertThatThrownBy(() -> sut.markAsRead(99L, 1L))
                    .isInstanceOf(NotificationException.class)
                    .extracting(e -> ((NotificationException) e).getErrorCode())
                    .isEqualTo(NotificationErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("다른 수신자 예외의 errorCode는 ACCESS_DENIED이다")
        void markAsRead_다른_수신자_예외_ACCESS_DENIED_errorCode() {
            // given
            NotificationRequest request = inAppRequest(1L, false);
            given(notificationFacade.findById(1L)).willReturn(request);

            // when & then
            assertThatThrownBy(() -> sut.markAsRead(1L, 999L))
                    .isInstanceOf(NotificationException.class)
                    .extracting(e -> ((NotificationException) e).getErrorCode())
                    .isEqualTo(NotificationErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("SENT가 아닌 상태 알림 읽음 처리 시 도메인 INVALID_STATE 예외가 전파된다")
        void markAsRead_PENDING_상태_INVALID_STATE_예외_전파() {
            // given — PENDING 상태 알림 (status를 SENT로 바꾸지 않음)
            NotificationRequest pending = NotificationRequest.create(
                    1L, NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.IN_APP,
                    "pending-key", 3, null, null, null, null
            );
            given(notificationFacade.findById(1L)).willReturn(pending);

            // when & then
            assertThatThrownBy(() -> sut.markAsRead(1L, 1L))
                    .isInstanceOf(NotificationException.class)
                    .extracting(e -> ((NotificationException) e).getErrorCode())
                    .isEqualTo(NotificationErrorCode.INVALID_STATE);
        }
    }

    private NotificationRequest inAppRequest(Long recipientId, boolean isRead) {
        // given
        NotificationRequest request = NotificationRequest.create(
                recipientId,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.IN_APP,
                "key-" + recipientId + "-" + isRead,
                3, null, null, null, null
        );
        ReflectionTestUtils.setField(request, "status", NotificationStatus.SENT);
        if (isRead) {
            request.markAsRead();
        }
        return request;
    }
}
