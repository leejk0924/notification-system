package com.jk.notificationservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static com.jk.notificationservice.domain.NotificationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationStatusTest {

    @Test
    @DisplayName("모든 상태는 terminal 또는 non-terminal로 분류되어야 한다.")
    void 모든_상태_분류_안전성_검증() {
        Set<NotificationStatus> terminal = EnumSet.of(SENT, FAILED, DEAD_LETTER, EXPIRED);
        Set<NotificationStatus> nonTerminal = EnumSet.of(PENDING, PROCESSING);

        assertThat(EnumSet.allOf(NotificationStatus.class))
                .containsAll(terminal)
                .containsAll(nonTerminal)
                .hasSize(terminal.size() + nonTerminal.size());

        assertThat(terminal).doesNotContainAnyElementsOf(nonTerminal);
    }
}