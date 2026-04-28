package com.giseop.comebot.telegram.inbound.offset;

public interface TelegramUpdateOffsetRepository {

    long getNextOffset();

    void saveNextOffset(long nextOffset);
}
