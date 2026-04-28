package com.giseop.comebot.telegram.inbound.offset;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
        name = "telegram.inbound.offset-storage-type",
        havingValue = "IN_MEMORY",
        matchIfMissing = true
)
public class InMemoryTelegramUpdateOffsetRepository implements TelegramUpdateOffsetRepository {

    private final AtomicLong nextOffset = new AtomicLong(0);

    @Override
    public long getNextOffset() {
        return nextOffset.get();
    }

    @Override
    public void saveNextOffset(long nextOffset) {
        this.nextOffset.updateAndGet(current -> Math.max(current, nextOffset));
    }
}
