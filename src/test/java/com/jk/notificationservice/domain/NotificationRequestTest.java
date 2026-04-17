package com.jk.notificationservice.domain;

import com.jk.notificationservice.common.NotificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static com.jk.notificationservice.domain.NotificationChannel.EMAIL;
import static com.jk.notificationservice.domain.NotificationChannel.IN_APP;
import static com.jk.notificationservice.domain.NotificationStatus.PROCESSING;
import static com.jk.notificationservice.domain.NotificationType.ENROLLMENT_COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationRequestTest {

    @DisplayName("isExpired 메서드")
    @Nested
    class IsExpired {

        @Test
        @DisplayName("expireAt이 현재 시각과 정확히 같으면 만료되지 않은 것으로 판단")
        void isExpired_expireAt이_현재시각과_같으면_false() {
            LocalDateTime now = LocalDateTime.now();
            NotificationRequest sut = create(now);

            assertThat(sut.isExpired(now)).isFalse();
        }

        private NotificationRequest create(LocalDateTime expireAt) {
            NotificationRequest request = NotificationRequest.create(
                    1L, ENROLLMENT_COMPLETED, EMAIL, "test-key", 3,
                    null, null, null, null
            );
            ReflectionTestUtils.setField(request, "expireAt", expireAt);
            return request;
        }
    }

    @DisplayName("isRetryable 메서드")
    @Nested
    class IsRetryable {

        @DisplayName("retryCount와 maxRetryCount 비교에 따라 재시도 가능 여부를 반환한다")
        @ParameterizedTest(name = "{2}")
        @MethodSource("isRetryableArguments")
        void isRetryable(int retryCount, boolean expected, String displayName) {
            NotificationRequest sut = createWithRetryCount(retryCount);

            assertThat(sut.isRetryable()).isEqualTo(expected);
        }

        static Stream<Arguments> isRetryableArguments() {
            return Stream.of(
                    Arguments.of(0, true,  "retryCount가 0이면 재시도 가능"),
                    Arguments.of(2, true,  "retryCount가 maxRetryCount보다 작으면 재시도 가능"),
                    Arguments.of(3, false, "retryCount가 maxRetryCount와 같으면 재시도 불가 (경계값)")
            );
        }

        private NotificationRequest createWithRetryCount(int retryCount) {
            NotificationRequest request = NotificationRequest.create(
                    1L, ENROLLMENT_COMPLETED, EMAIL, "test-key", 3,
                    null, null, null, null
            );
            ReflectionTestUtils.setField(request, "retryCount", retryCount);
            return request;
        }
    }

    @DisplayName("handleFailure 메서드")
    @Nested
    class HandleFailure {

        @Test
        @DisplayName("재시도 가능하면 PENDING으로 전이하고 retryCount를 증가시킨다")
        void handleFailure_재시도가능하면_PENDING() {
            NotificationRequest sut = createProcessing(0, 3);
            LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(5);

            sut.handleFailure("일시 오류", nextRetryAt);

            assertThat(sut.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(sut.getRetryCount()).isEqualTo(1);
            assertThat(sut.getNextRetryAt()).isEqualTo(nextRetryAt);
        }

        @Test
        @DisplayName("재시도 횟수 소진 시 DEAD_LETTER로 전이하고 실패 사유를 기록한다")
        void handleFailure_재시도소진시_DEAD_LETTER() {
            NotificationRequest sut = createProcessing(3, 3);

            sut.handleFailure("최종 실패", null);

            assertThat(sut.getStatus()).isEqualTo(NotificationStatus.DEAD_LETTER);
            assertThat(sut.getFailureReason()).isEqualTo("최종 실패");
        }
    }

    @DisplayName("상태 전이 가드")
    @Nested
    class StateTransitionGuard {

        @Test
        @DisplayName("PENDING이 아닌 상태에서 markAsProcessing 호출 시 예외가 발생한다")
        void markAsProcessing_PENDING_아닌_상태에서_예외() {
            NotificationRequest sut = createProcessing(0, 3);

            assertThatThrownBy(sut::markAsProcessing)
                    .isInstanceOf(NotificationException.class);
        }

        @Test
        @DisplayName("PROCESSING이 아닌 상태에서 markAsSent 호출 시 예외가 발생한다")
        void markAsSent_PROCESSING_아닌_상태에서_예외() {
            NotificationRequest sut = NotificationRequest.create(
                    1L, ENROLLMENT_COMPLETED, EMAIL, "test-key", 3,
                    null, null, null, null
            );

            assertThatThrownBy(sut::markAsSent)
                    .isInstanceOf(NotificationException.class);
        }

        @Test
        @DisplayName("PENDING·PROCESSING이 아닌 상태에서 markAsExpired 호출 시 예외가 발생한다")
        void markAsExpired_허용되지_않는_상태에서_예외() {
            NotificationRequest sut = createProcessing(0, 3);
            sut.markAsSent();

            assertThatThrownBy(sut::markAsExpired)
                    .isInstanceOf(NotificationException.class);
        }
    }

    @DisplayName("markAsRead 메서드")
    @Nested
    class MarkAsRead {

        @Test
        @DisplayName("IN_APP 채널이면 읽음 처리된다")
        void markAsRead_IN_APP이면_읽음처리() {
            NotificationRequest sut = NotificationRequest.create(
                    1L, ENROLLMENT_COMPLETED, IN_APP, "test-key", 3,
                    null, null, null, null
            );

            sut.markAsRead();

            assertThat(sut.isRead()).isTrue();
            assertThat(sut.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("IN_APP이 아닌 채널이면 읽음 처리되지 않는다")
        void markAsRead_EMAIL이면_읽음처리_안됨() {
            NotificationRequest sut = NotificationRequest.create(
                    1L, ENROLLMENT_COMPLETED, EMAIL, "test-key", 3,
                    null, null, null, null
            );

            sut.markAsRead();

            assertThat(sut.isRead()).isFalse();
            assertThat(sut.getReadAt()).isNull();
        }
    }

    // PROCESSING 상태의 NotificationRequest 생성 헬퍼
    private NotificationRequest createProcessing(int retryCount, int maxRetryCount) {
        NotificationRequest request = NotificationRequest.create(
                1L, ENROLLMENT_COMPLETED, EMAIL, "test-key", maxRetryCount,
                null, null, null, null
        );
        ReflectionTestUtils.setField(request, "retryCount", retryCount);
        ReflectionTestUtils.setField(request, "status", PROCESSING);
        return request;
    }
}