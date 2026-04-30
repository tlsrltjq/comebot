package com.giseop.comebot.scheduler.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SchedulerStatusController.class)
class SchedulerStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradingSchedulerProperties tradingSchedulerProperties;

    @MockitoBean
    private CandidateSchedulerProperties candidateSchedulerProperties;

    @Test
    void getStatusReturnsOk() throws Exception {
        schedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));
        candidateSchedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatusReturnsSchedulerFields() throws Exception {
        schedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));
        candidateSchedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.fixedDelayMs").value(60000))
                .andExpect(jsonPath("$.markets[0]").value("KRW-BTC"))
                .andExpect(jsonPath("$.markets[1]").value("KRW-ETH"))
                .andExpect(jsonPath("$.candidateEnabled").value(false))
                .andExpect(jsonPath("$.candidateFixedDelayMs").value(60000))
                .andExpect(jsonPath("$.candidateMarkets[0]").value("KRW-BTC"))
                .andExpect(jsonPath("$.candidateMarkets[1]").value("KRW-ETH"))
                .andExpect(jsonPath("$.candidateNotifySummary").value(false));
    }

    @Test
    void getStatusReturnsDefaultDisabledValue() throws Exception {
        schedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));
        candidateSchedulerProperties(false, 60000, List.of("KRW-BTC", "KRW-ETH"));

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.candidateEnabled").value(false));
    }

    @Test
    void getStatusReturnsConfiguredCandidateSchedulerFields() throws Exception {
        schedulerProperties(true, 30000, List.of("KRW-BTC", "KRW-ETH"));
        candidateSchedulerProperties(true, 45000, List.of("KRW-XRP"));

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markets[0]").value("KRW-BTC"))
                .andExpect(jsonPath("$.candidateEnabled").value(true))
                .andExpect(jsonPath("$.candidateFixedDelayMs").value(45000))
                .andExpect(jsonPath("$.candidateMarkets[0]").value("KRW-XRP"));
    }

    private void schedulerProperties(boolean enabled, long fixedDelayMs, List<String> markets) {
        when(tradingSchedulerProperties.isEnabled()).thenReturn(enabled);
        when(tradingSchedulerProperties.getFixedDelayMs()).thenReturn(fixedDelayMs);
        when(tradingSchedulerProperties.getMarkets()).thenReturn(markets);
    }

    private void candidateSchedulerProperties(boolean enabled, long fixedDelayMs, List<String> markets) {
        when(candidateSchedulerProperties.isEnabled()).thenReturn(enabled);
        when(candidateSchedulerProperties.getFixedDelayMs()).thenReturn(fixedDelayMs);
        when(candidateSchedulerProperties.getMarkets()).thenReturn(markets);
        when(candidateSchedulerProperties.isNotifySummary()).thenReturn(false);
    }
}
