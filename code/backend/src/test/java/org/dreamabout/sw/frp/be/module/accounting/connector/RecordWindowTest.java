package org.dreamabout.sw.frp.be.module.accounting.connector;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordWindowTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);

    @Test
    void shouldCreateDateRangeWithoutUpdatedSince() {
        var window = RecordWindow.between(FROM, TO);

        assertThat(window.from()).isEqualTo(FROM);
        assertThat(window.to()).isEqualTo(TO);
        assertThat(window.updatedSince()).isEmpty();
    }

    @Test
    void shouldNarrowWindowToRecordsUpdatedSince() {
        var since = Instant.parse("2026-02-01T00:00:00Z");

        var window = RecordWindow.between(FROM, TO).withUpdatedSince(since);

        assertThat(window.updatedSince()).contains(since);
        assertThat(window.from()).isEqualTo(FROM);
    }

    @Test
    void shouldRejectInvertedRange() {
        assertThatThrownBy(() -> RecordWindow.between(TO, FROM)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMarkLastPage() {
        var last = RecordPage.last(List.of());
        var notLast = RecordPage.of(List.of(), "200");

        assertThat(last.isLast()).isTrue();
        assertThat(notLast.isLast()).isFalse();
        assertThat(notLast.nextCursor()).isEqualTo("200");
    }

    @Test
    void shouldCreateSecretAndPlainCredentialFields() {
        assertThat(CredentialField.secret("token").secret()).isTrue();
        assertThat(CredentialField.plain("accountId").secret()).isFalse();
    }
}
