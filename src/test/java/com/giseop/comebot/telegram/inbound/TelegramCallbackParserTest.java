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
    void parseAutoCallback() {
        TelegramCallback callback = parser.parse("AUTO");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.AUTO);
        assertThat(callback.market()).isNull();
    }

    @Test
    void parsePnlCallback() {
        TelegramCallback callback = parser.parse("PNL");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.PNL);
        assertThat(callback.market()).isNull();
    }

    @Test
    void parseConditionsCallback() {
        TelegramCallback callback = parser.parse("CONDITIONS");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.CONDITIONS);
        assertThat(callback.market()).isNull();
    }

    @Test
    void parseRunCallback() {
        TelegramCallback callback = parser.parse("RUN:KRW-BTC");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.RUN);
        assertThat(callback.market()).isEqualTo("KRW-BTC");
    }

    @Test
    void parseCandidatesCallback() {
        TelegramCallback callback = parser.parse("CANDIDATES");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.CANDIDATES);
        assertThat(callback.market()).isNull();
    }

    @Test
    void parseCandidateRunCallback() {
        TelegramCallback callback = parser.parse("CANDIDATE_RUN:KRW-BTC");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.CANDIDATE_RUN);
        assertThat(callback.market()).isEqualTo("KRW-BTC");
    }

    @Test
    void parseHistoryCallback() {
        TelegramCallback callback = parser.parse("HISTORY:KRW-BTC");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.HISTORY);
        assertThat(callback.market()).isEqualTo("KRW-BTC");
    }

    @Test
    void parsePortfolioCallback() {
        TelegramCallback callback = parser.parse("PORTFOLIO");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.PORTFOLIO);
        assertThat(callback.market()).isNull();
    }

    @Test
    void parsePositionsCallback() {
        TelegramCallback callback = parser.parse("POSITIONS");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.POSITIONS);
        assertThat(callback.market()).isNull();
    }

    @Test
    void parseRiskCallback() {
        TelegramCallback callback = parser.parse("RISK");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.RISK);
        assertThat(callback.market()).isNull();
    }

    @Test
    void parseSafetyCallback() {
        TelegramCallback callback = parser.parse("SAFETY");

        assertThat(callback.type()).isEqualTo(TelegramCallbackType.SAFETY);
        assertThat(callback.market()).isNull();
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
