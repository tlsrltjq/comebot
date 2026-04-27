package com.giseop.comebot.telegram.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramCallbackParserTest {

    private final TelegramCallbackParser parser = new TelegramCallbackParser();

    @Test
    void parseStatusCallback() {
        TelegramCallback callback = parser.parse("STATUS");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.STATUS);
        assertThat(callback.market()).isNull();
    }

    @Test
    void parseRunCallback() {
        TelegramCallback callback = parser.parse("RUN:KRW-BTC");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.RUN);
        assertThat(callback.market()).isEqualTo("KRW-BTC");
    }

    @Test
    void parseHistoryCallback() {
        TelegramCallback callback = parser.parse("HISTORY:KRW-BTC");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.HISTORY);
        assertThat(callback.market()).isEqualTo("KRW-BTC");
    }

    @Test
    void parseHelpCallback() {
        TelegramCallback callback = parser.parse("HELP");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.HELP);
        assertThat(callback.market()).isNull();
    }

    @Test
    void parseUnknownCallback() {
        TelegramCallback callback = parser.parse("UNKNOWN");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.UNKNOWN);
        assertThat(callback.market()).isNull();
    }
}
