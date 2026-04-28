package com.giseop.comebot.portfolio.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PortfolioStatusController.class)
class PortfolioStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaperPortfolioService paperPortfolioService;

    @Test
    void statusReturnsPortfolioStatus() throws Exception {
        when(paperPortfolioService.getPortfolio()).thenReturn(new PaperPortfolio(
                new BigDecimal("999900"),
                new BigDecimal("20"),
                List.of()
        ));

        mockMvc.perform(get("/api/portfolio/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(999900))
                .andExpect(jsonPath("$.realizedProfit").value(20));
    }

    @Test
    void positionsReturnsPositions() throws Exception {
        when(paperPortfolioService.findPositions()).thenReturn(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1.5"), new BigDecimal("100"))
        ));

        mockMvc.perform(get("/api/portfolio/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].market").value("KRW-BTC"))
                .andExpect(jsonPath("$[0].quantity").value(1.5))
                .andExpect(jsonPath("$[0].averageBuyPrice").value(100));
    }
}
