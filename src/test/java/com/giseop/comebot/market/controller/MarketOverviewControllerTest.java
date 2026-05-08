package com.giseop.comebot.market.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.market.dto.BtcChangeChartResponse;
import com.giseop.comebot.market.dto.BtcChangePointResponse;
import com.giseop.comebot.market.service.BtcChangeChartService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketOverviewController.class)
class MarketOverviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BtcChangeChartService btcChangeChartService;

    @Test
    void btcChangeReturnsChartResponse() throws Exception {
        when(btcChangeChartService.chart(any(), any())).thenReturn(response());

        mockMvc.perform(get("/api/market/btc-change")
                        .param("exchange", "upbit")
                        .param("range", "24h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchange").value("UPBIT"))
                .andExpect(jsonPath("$.market").value("KRW-BTC"))
                .andExpect(jsonPath("$.range").value("24h"))
                .andExpect(jsonPath("$.points[0].changeRate").value(0.0));
    }

    @Test
    void btcChangeRejectsUnsupportedRange() throws Exception {
        mockMvc.perform(get("/api/market/btc-change")
                        .param("exchange", "upbit")
                        .param("range", "30d"))
                .andExpect(status().isBadRequest());
    }

    private BtcChangeChartResponse response() {
        return new BtcChangeChartResponse(
                "UPBIT",
                "KRW-BTC",
                "24h",
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("10.00000000"),
                new BigDecimal("120"),
                new BigDecimal("90"),
                List.of(new BtcChangePointResponse(Instant.parse("2026-05-08T00:00:00Z"), new BigDecimal("100"), BigDecimal.ZERO))
        );
    }
}
