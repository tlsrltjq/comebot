package com.giseop.comebot.scheduler.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "scheduler_control_setting")
public class SchedulerControlSettingEntity {

    public static final String DEFAULT_ID = "default";

    @Id
    @Column(nullable = false, length = 50)
    private String id;

    @Column(name = "auto_trading_enabled", nullable = false)
    private boolean autoTradingEnabled;

    @Column(name = "candidate_fixed_delay_ms", nullable = false)
    private long candidateFixedDelayMs;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SchedulerControlSettingEntity() {
    }

    public SchedulerControlSettingEntity(boolean autoTradingEnabled, long candidateFixedDelayMs, Instant updatedAt) {
        this.id = DEFAULT_ID;
        this.autoTradingEnabled = autoTradingEnabled;
        this.candidateFixedDelayMs = candidateFixedDelayMs;
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public boolean isAutoTradingEnabled() {
        return autoTradingEnabled;
    }

    public long getCandidateFixedDelayMs() {
        return candidateFixedDelayMs;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
