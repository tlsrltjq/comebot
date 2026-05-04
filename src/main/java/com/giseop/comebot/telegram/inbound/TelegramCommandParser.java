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
            case "/menu" -> new TelegramCommand(TelegramCommandType.MENU, null);
            case "/status" -> new TelegramCommand(TelegramCommandType.STATUS, null);
            case "/auto" -> new TelegramCommand(TelegramCommandType.AUTO, null);
            case "/pnl" -> new TelegramCommand(TelegramCommandType.PNL, null);
            case "/conditions" -> new TelegramCommand(TelegramCommandType.CONDITIONS, null);
            case "/candidates" -> new TelegramCommand(TelegramCommandType.CANDIDATES, null);
            case "/candidate-run" -> new TelegramCommand(TelegramCommandType.CANDIDATE_RUN, market);
            case "/run" -> new TelegramCommand(TelegramCommandType.RUN, market);
            case "/history" -> new TelegramCommand(TelegramCommandType.HISTORY, market);
            case "/portfolio" -> new TelegramCommand(TelegramCommandType.PORTFOLIO, null);
            case "/positions" -> new TelegramCommand(TelegramCommandType.POSITIONS, null);
            case "/risk" -> new TelegramCommand(TelegramCommandType.RISK, null);
            case "/safety" -> new TelegramCommand(TelegramCommandType.SAFETY, null);
            default -> new TelegramCommand(TelegramCommandType.UNKNOWN, null);
        };
    }
}
