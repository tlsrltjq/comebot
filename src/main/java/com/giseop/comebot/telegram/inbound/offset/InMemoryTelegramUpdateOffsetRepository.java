package com.giseop.comebot.telegram.inbound.offset;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
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
