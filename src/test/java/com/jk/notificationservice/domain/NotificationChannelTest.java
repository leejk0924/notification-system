package com.jk.notificationservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static com.jk.notificationservice.domain.NotificationChannel.*;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationChannelTest {

    @Test
    @DisplayName("모든 채널의 읽음 추적은 지원/미지원으로 분류되어야 한다")
    void 모든_채널_읽음추적_분류_완전성_검증() {
        Set<NotificationChannel> supports = EnumSet.of(IN_APP);
        Set<NotificationChannel> notSupports = EnumSet.of(EMAIL);

        assertThat(EnumSet.allOf(NotificationChannel.class))
                .containsAll(supports)
                .containsAll(notSupports)
                .hasSize(supports.size() + notSupports.size());

        assertThat(supports).doesNotContainAnyElementsOf(notSupports);
    }
}