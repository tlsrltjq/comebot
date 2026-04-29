package com.giseop.comebot.telegram.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramCommandParserTest {

    private final TelegramCommandParser parser = new TelegramCommandParser();

    @Test
    void parseHelpCommand() {
        TelegramCommand command = parser.parse("/help");

        assertThat(command.type()).isEqualTo(TelegramCommandType.HELP);
        assertThat(command.market()).isNull();
    }

    @Test
    void parseStatusCommand() {
        TelegramCommand command = parser.parse("/status");

        assertThat(command.type()).isEqualTo(TelegramCommandType.STATUS);
        assertThat(command.market()).isNull();
    }

    @Test
    void parseMenuCommand() {
        TelegramCommand command = parser.parse("/menu");

        assertThat(command.type()).isEqualTo(TelegramCommandType.MENU);
        assertThat(command.market()).isNull();
    }

    @Test
    void parseRunCommandWithMarket() {
        TelegramCommand command = parser.parse("/run KRW-BTC");

        assertThat(command.type()).isEqualTo(TelegramCommandType.RUN);
        assertThat(command.market()).isEqualTo("KRW-BTC");
    }

    @Test
    void parseHistoryCommandWithMarket() {
        TelegramCommand command = parser.parse("/history KRW-BTC");

        assertThat(command.type()).isEqualTo(TelegramCommandType.HISTORY);
        assertThat(command.market()).isEqualTo("KRW-BTC");
    }

    @Test
    void parsePortfolioCommand() {
        TelegramCommand command = parser.parse("/portfolio");

        assertThat(command.type()).isEqualTo(TelegramCommandType.PORTFOLIO);
        assertThat(command.market()).isNull();
    }

    @Test
    void parsePositionsCommand() {
        TelegramCommand command = parser.parse("/positions");

        assertThat(command.type()).isEqualTo(TelegramCommandType.POSITIONS);
        assertThat(command.market()).isNull();
    }

    @Test
    void parseRiskCommand() {
        TelegramCommand command = parser.parse("/risk");

        assertThat(command.type()).isEqualTo(TelegramCommandType.RISK);
        assertThat(command.market()).isNull();
    }

    @Test
    void parseSafetyCommand() {
        TelegramCommand command = parser.parse("/safety");

        assertThat(command.type()).isEqualTo(TelegramCommandType.SAFETY);
        assertThat(command.market()).isNull();
    }

    @Test
    void parseUnknownCommand() {
        TelegramCommand command = parser.parse("/unknown");

        assertThat(command.type()).isEqualTo(TelegramCommandType.UNKNOWN);
        assertThat(command.market()).isNull();
    }
}
