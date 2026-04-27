package com.giseop.comebot.telegram.inbound;

import java.util.List;

public interface TelegramUpdateClient {

    List<TelegramUpdate> getUpdates(String botToken, long offset);
}
