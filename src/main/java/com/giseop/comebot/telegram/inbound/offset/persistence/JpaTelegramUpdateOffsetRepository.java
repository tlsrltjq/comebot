package com.giseop.comebot.telegram.inbound.offset.persistence;

import com.giseop.comebot.telegram.inbound.offset.TelegramUpdateOffsetRepository;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "telegram.inbound.offset-storage-type", havingValue = "JPA")
public class JpaTelegramUpdateOffsetRepository implements TelegramUpdateOffsetRepository {

    private static final String OFFSET_ID = "telegram-get-updates";

    private final SpringDataTelegramUpdateOffsetJpaRepository jpaRepository;

    public JpaTelegramUpdateOffsetRepository(SpringDataTelegramUpdateOffsetJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public long getNextOffset() {
        return jpaRepository.findById(OFFSET_ID)
                .map(TelegramUpdateOffsetEntity::getLastUpdateOffset)
                .orElse(0L);
    }

    @Override
    public void saveNextOffset(long nextOffset) {
        long storedOffset = Math.max(getNextOffset(), nextOffset);
        jpaRepository.save(new TelegramUpdateOffsetEntity(OFFSET_ID, storedOffset, Instant.now()));
    }
}
