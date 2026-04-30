package com.giseop.comebot.scheduler;

import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.NotificationSender;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CandidateSchedulerNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CandidateSchedulerNotificationService.class);

    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final NotificationProperties notificationProperties;
    private final NotificationSender notificationSender;

    public CandidateSchedulerNotificationService(
            CandidateSchedulerProperties candidateSchedulerProperties,
            NotificationProperties notificationProperties,
            NotificationSender notificationSender
    ) {
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.notificationProperties = notificationProperties;
        this.notificationSender = notificationSender;
    }

    public void notifySummary(CandidateSchedulerRunSummary summary) {
        if (!candidateSchedulerProperties.isNotifySummary() || !notificationProperties.isEnabled()) {
            return;
        }
        if (summary.requestedMarkets() <= 0) {
            return;
        }

        try {
            notificationSender.send(toMessage(summary));
        } catch (RuntimeException exception) {
            log.warn("Candidate scheduler summary notification failed: {}", exception.getClass().getSimpleName());
        }
    }

    public NotificationMessage toMessage(CandidateSchedulerRunSummary summary) {
        String body = """
                후보 자동 실행 요약
                요청 market: %d
                실행 market: %d
                체결: %d
                거절: %d
                보류: %d
                실패: %d
                """.formatted(
                summary.requestedMarkets(),
                summary.executedMarkets(),
                summary.filledCount(),
                summary.rejectedCount(),
                summary.holdCount(),
                summary.failedCount()
        ).strip();
        return new NotificationMessage("Candidate scheduler summary", body, Instant.now());
    }
}
