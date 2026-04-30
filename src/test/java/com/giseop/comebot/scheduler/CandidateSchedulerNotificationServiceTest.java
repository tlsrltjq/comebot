package com.giseop.comebot.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.NotificationSender;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateSchedulerNotificationServiceTest {

    @Test
    void notifySummaryDoesNotSendWhenSchedulerSummaryNotificationDisabled() {
        CandidateSchedulerProperties schedulerProperties = new CandidateSchedulerProperties();
        schedulerProperties.setNotifySummary(false);
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setEnabled(true);
        RecordingNotificationSender sender = new RecordingNotificationSender();

        service(schedulerProperties, notificationProperties, sender)
                .notifySummary(new CandidateSchedulerRunSummary(1, 1, 1, 0, 0, 0));

        assertThat(sender.messages).isEmpty();
    }

    @Test
    void notifySummaryDoesNotSendWhenNotificationDisabled() {
        CandidateSchedulerProperties schedulerProperties = new CandidateSchedulerProperties();
        schedulerProperties.setNotifySummary(true);
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setEnabled(false);
        RecordingNotificationSender sender = new RecordingNotificationSender();

        service(schedulerProperties, notificationProperties, sender)
                .notifySummary(new CandidateSchedulerRunSummary(1, 1, 1, 0, 0, 0));

        assertThat(sender.messages).isEmpty();
    }

    @Test
    void notifySummarySendsWhenEnabled() {
        CandidateSchedulerProperties schedulerProperties = new CandidateSchedulerProperties();
        schedulerProperties.setNotifySummary(true);
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setEnabled(true);
        RecordingNotificationSender sender = new RecordingNotificationSender();

        service(schedulerProperties, notificationProperties, sender)
                .notifySummary(new CandidateSchedulerRunSummary(2, 2, 1, 1, 0, 0));

        assertThat(sender.messages).hasSize(1);
        assertThat(sender.messages.getFirst().body()).contains("후보 자동 실행 요약");
        assertThat(sender.messages.getFirst().body()).contains("체결: 1");
        assertThat(sender.messages.getFirst().body()).contains("거절: 1");
    }

    @Test
    void notifySummaryFailureDoesNotThrow() {
        CandidateSchedulerProperties schedulerProperties = new CandidateSchedulerProperties();
        schedulerProperties.setNotifySummary(true);
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setEnabled(true);

        service(schedulerProperties, notificationProperties, message -> {
            throw new IllegalStateException("failed");
        }).notifySummary(new CandidateSchedulerRunSummary(1, 1, 1, 0, 0, 0));
    }

    private CandidateSchedulerNotificationService service(
            CandidateSchedulerProperties schedulerProperties,
            NotificationProperties notificationProperties,
            NotificationSender notificationSender
    ) {
        return new CandidateSchedulerNotificationService(schedulerProperties, notificationProperties, notificationSender);
    }

    private static class RecordingNotificationSender implements NotificationSender {

        private final List<NotificationMessage> messages = new ArrayList<>();

        @Override
        public void send(NotificationMessage message) {
            messages.add(message);
        }
    }
}
