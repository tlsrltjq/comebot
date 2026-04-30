package com.giseop.comebot.strategy.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.StrategySelectionProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StrategyStatusController.class)
class StrategyStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StrategyProperties strategyProperties;

    @MockitoBean
    private StrategySelectionProperties strategySelectionProperties;

    @Test
    void statusReturnsSimpleThresholdStrategyProperties() throws Exception {
        org.mockito.Mockito.when(strategyProperties.getBuyPrice()).thenReturn(new BigDecimal("90000000"));
        org.mockito.Mockito.when(strategyProperties.getSellPrice()).thenReturn(new BigDecimal("110000000"));
        org.mockito.Mockito.when(strategyProperties.getOrderQuantity()).thenReturn(new BigDecimal("0.001"));
        org.mockito.Mockito.when(strategySelectionProperties.getStrategyName()).thenReturn("SimpleThresholdStrategy");

        mockMvc.perform(get("/api/strategy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyName").value("SimpleThresholdStrategy"))
                .andExpect(jsonPath("$.buyPrice").value(90000000))
                .andExpect(jsonPath("$.sellPrice").value(110000000))
                .andExpect(jsonPath("$.orderQuantity").value(0.001));
    }
}
