package com.giseop.comebot.market.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class
})
class MarketPriceTradingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void updatedTestPriceIsUsedByTradingFlowRun() throws Exception {
        mockMvc.perform(put("/api/market-prices/KRW-BTC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":50000000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(50000000));

        mockMvc.perform(get("/api/trading-flow/run").param("market", "KRW-BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("KRW-BTC"))
                .andExpect(jsonPath("$.signalType").value("BUY"))
                .andExpect(jsonPath("$.orderStatus").value("FILLED"));
    }
}
