package com.giseop.comebot.telegram.inbound.offset.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTelegramUpdateOffsetJpaRepository
        extends JpaRepository<TelegramUpdateOffsetEntity, String> {
}
