package com.giseop.comebot.telegram.inbound;

import org.springframework.stereotype.Component;

@Component
public class TelegramCommandParser {

    public TelegramCommand parse(String text) {
        if (text == null || text.isBlank()) {
            return new TelegramCommand(TelegramCommandType.UNKNOWN, null);
        }

        String[] parts = text.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        String market = parts.length > 1 ? parts[1] : null;

        return switch (command) {
            case "/help" -> new TelegramCommand(TelegramCommandType.HELP, null);
            case "/status" -> new TelegramCommand(TelegramCommandType.STATUS, null);
            case "/run" -> new TelegramCommand(TelegramCommandType.RUN, market);
            case "/history" -> new TelegramCommand(TelegramCommandType.HISTORY, market);
            default -> new TelegramCommand(TelegramCommandType.UNKNOWN, null);
        };
    }
}
