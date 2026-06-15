package com.giseop.comebot.scheduler;

import com.giseop.comebot.scheduler.persistence.SchedulerControlSettingEntity;
import com.giseop.comebot.scheduler.persistence.SpringDataSchedulerControlSettingJpaRepository;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SchedulerControlService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerControlService.class);
    private static final long THIRTY_SECONDS = 30000;
    private static final long SIXTY_SECONDS = 60000;

    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final PositionExitSchedulerProperties positionExitSchedulerProperties;
    private final SpringDataSchedulerControlSettingJpaRepository settingRepository;
    private final boolean restoreEnabled;

    @Autowired
    public SchedulerControlService(
            CandidateSchedulerProperties candidateSchedulerProperties,
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            ObjectProvider<SpringDataSchedulerControlSettingJpaRepository> settingRepository,
            @Value("${scheduler.control.restore-enabled:true}") boolean restoreEnabled
    ) {
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.settingRepository = settingRepository.getIfAvailable();
        this.restoreEnabled = restoreEnabled;
    }

    SchedulerControlService(
            CandidateSchedulerProperties candidateSchedulerProperties,
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            SpringDataSchedulerControlSettingJpaRepository settingRepository
    ) {
        this(candidateSchedulerProperties, positionExitSchedulerProperties, settingRepository, true);
    }

    SchedulerControlService(
            CandidateSchedulerProperties candidateSchedulerProperties,
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            SpringDataSchedulerControlSettingJpaRepository settingRepository,
            boolean restoreEnabled
    ) {
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.settingRepository = settingRepository;
        this.restoreEnabled = restoreEnabled;
    }

    @PostConstruct
    public void restorePersistedSetting() {
        if (!restoreEnabled) {
            return;
        }
        if (settingRepository == null) {
            return;
        }
        settingRepository.findById(SchedulerControlSettingEntity.DEFAULT_ID)
                .ifPresent(setting -> {
                    try {
                        apply(setting.isAutoTradingEnabled(), setting.getCandidateFixedDelayMs());
                    } catch (IllegalArgumentException e) {
                        log.warn("Ignoring invalid persisted candidateFixedDelayMs={}: {}", setting.getCandidateFixedDelayMs(), e.getMessage());
                    }
                });
    }

    public void update(Boolean autoTradingEnabled, Long candidateFixedDelayMs) {
        boolean nextAutoTradingEnabled = autoTradingEnabled == null
                ? isAutoTradingEnabled()
                : autoTradingEnabled;
        long nextCandidateFixedDelayMs = candidateFixedDelayMs == null
                ? candidateSchedulerProperties.getFixedDelayMs()
                : candidateFixedDelayMs;

        validateCandidateFixedDelayMs(nextCandidateFixedDelayMs);
        apply(nextAutoTradingEnabled, nextCandidateFixedDelayMs);
        if (settingRepository != null) {
            settingRepository.save(new SchedulerControlSettingEntity(
                    nextAutoTradingEnabled,
                    nextCandidateFixedDelayMs,
                    Instant.now()
            ));
        }
    }

    public void validateCandidateFixedDelayMs(long candidateFixedDelayMs) {
        if (candidateFixedDelayMs != THIRTY_SECONDS && candidateFixedDelayMs != SIXTY_SECONDS) {
            throw new IllegalArgumentException("candidateFixedDelayMs must be 30000 or 60000");
        }
    }

    private void apply(boolean autoTradingEnabled, long candidateFixedDelayMs) {
        validateCandidateFixedDelayMs(candidateFixedDelayMs);
        candidateSchedulerProperties.setEnabled(autoTradingEnabled);
        positionExitSchedulerProperties.setEnabled(autoTradingEnabled);
        candidateSchedulerProperties.setFixedDelayMs(candidateFixedDelayMs);
    }

    private boolean isAutoTradingEnabled() {
        return candidateSchedulerProperties.isEnabled() && positionExitSchedulerProperties.isEnabled();
    }
}
