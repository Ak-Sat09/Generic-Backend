package com.example.emailoutreach;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ReplySyncServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T20:30:00Z");
    private static final Instant MORNING = Instant.parse("2026-11-25T04:00:00Z");
    private static final Instant EVENING = Instant.parse("2026-11-25T13:00:00Z");

    private final MailboxReader mailbox = mock(MailboxReader.class);
    private final MailboxReader.Session session = mock(MailboxReader.Session.class);
    private final OutreachStore store = mock(OutreachStore.class);
    private ReplySyncService service;

    @BeforeEach
    void setUp() {
        service = new ReplySyncService(mailbox, store, Clock.fixed(NOW, ZoneOffset.UTC));
        when(mailbox.open()).thenReturn(session);
        when(store.lastSuccessAt()).thenReturn(Optional.empty());
        when(store.knownEmails(anyCollection())).thenAnswer(call -> {
            Set<String> asked = Set.copyOf(call.getArgument(0));
            return asked.stream().filter(Set.of("hr@tcs.com", "jobs@infosys.com")::contains)
                    .collect(java.util.stream.Collectors.toSet());
        });
    }

    @Test
    void marksKnownSendersWithTheirFirstReply() {
        when(session.receivedSince(any())).thenReturn(List.of(
                new IncomingMail("hr@tcs.com", EVENING, null, false),
                new IncomingMail("hr@tcs.com", MORNING, null, false),
                new IncomingMail("newsletter@shop.com", MORNING, null, false)));

        service.run();

        assertThat(markedReplies()).containsExactly(Map.entry("hr@tcs.com", MORNING));
    }

    @Test
    void ignoresAutoRepliesAndBounces() {
        when(session.receivedSince(any())).thenReturn(List.of(
                new IncomingMail("hr@tcs.com", MORNING, null, true),
                new IncomingMail("mailer-daemon@googlemail.com", MORNING, "<sent-1@gmail>", false)));

        service.run();

        assertThat(markedReplies()).isEmpty();
        verify(session, never()).recipientsOf(anyString());
    }

    @Test
    void creditsTheOriginalRecipientWhenSomeoneElseReplies() {
        when(session.receivedSince(any())).thenReturn(List.of(
                new IncomingMail("priya@infosys.com", EVENING, "<sent-1@gmail>", false)));
        when(session.recipientsOf("<sent-1@gmail>")).thenReturn(Set.of("jobs@infosys.com"));

        service.run();

        assertThat(markedReplies()).containsExactly(Map.entry("jobs@infosys.com", EVENING));
    }

    @Test
    void firstRunLooksBackOneDayPlusOverlap() {
        when(session.receivedSince(any())).thenReturn(List.of());

        service.run();

        verify(session).receivedSince(NOW.minus(Duration.ofHours(24)).minus(Duration.ofDays(1)));
    }

    @Test
    void laterRunsStartFromTheLastSuccess() {
        Instant lastSuccess = NOW.minus(Duration.ofDays(3));
        when(store.lastSuccessAt()).thenReturn(Optional.of(lastSuccess));
        when(session.receivedSince(any())).thenReturn(List.of());

        service.run();

        verify(session).receivedSince(lastSuccess.minus(Duration.ofDays(1)));
        verify(store).recordSuccess(eq(NOW), eq(0), anyInt());
    }

    @Test
    void waitsWhileTheBackfillIsRunning() {
        when(store.backfillRunning()).thenReturn(true);

        assertThat(service.run()).isEqualTo(ReplySyncService.Result.SKIPPED);
        verify(mailbox, never()).open();
    }

    @Test
    void recordsFailuresAndClosesTheConnection() {
        when(session.receivedSince(any())).thenThrow(new MailboxException("Gmail down", null));

        assertThatThrownBy(() -> service.run()).isInstanceOf(MailboxException.class);

        verify(store).recordFailure(NOW, "Gmail down");
        verify(store, never()).recordSuccess(any(), anyInt(), anyInt());
        verify(session).close();
    }

    private Map<String, Instant> markedReplies() {
        ArgumentCaptor<Map<String, Instant>> captor = ArgumentCaptor.forClass(Map.class);
        verify(store).markResponded(captor.capture(), eq(NOW));
        return captor.getValue();
    }
}
