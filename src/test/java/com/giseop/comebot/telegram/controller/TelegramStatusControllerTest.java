package com.giseop.comebot.telegram.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.telegram.TelegramProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TelegramStatusController.class)
class TelegramStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelegramProperties telegramProperties;

    @Test
    void getStatusReturnsOk() throws Exception {
        when(telegramProperties.isEnabled()).thenReturn(false);
        when(telegramProperties.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/api/telegram/status"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatusReturnsDefaultDisabledAndUnconfigured() throws Exception {
        when(telegramProperties.isEnabled()).thenReturn(false);
        when(telegramProperties.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/api/telegram/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void getStatusDoesNotExposeTokenOrChatId() throws Exception {
        when(telegramProperties.isEnabled()).thenReturn(true);
        when(telegramProperties.isConfigured()).thenReturn(true);
        when(telegramProperties.getBotToken()).thenReturn("secret-token");
        when(telegramProperties.getChatId()).thenReturn("secret-chat-id");

        mockMvc.perform(get("/api/telegram/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.botToken").doesNotExist())
                .andExpect(jsonPath("$.chatId").doesNotExist());
    }
}
