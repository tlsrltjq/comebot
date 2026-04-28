package com.giseop.comebot.telegram.inbound.offset.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "telegram_update_offset")
public class TelegramUpdateOffsetEntity {

    @Id
    @Column(nullable = false, length = 50)
    private String id;

    @Column(name = "last_update_offset", nullable = false)
    private long lastUpdateOffset;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TelegramUpdateOffsetEntity() {
    }

    public TelegramUpdateOffsetEntity(String id, long lastUpdateOffset, Instant updatedAt) {
        this.id = id;
        this.lastUpdateOffset = lastUpdateOffset;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public long getLastUpdateOffset() {
        return lastUpdateOffset;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
