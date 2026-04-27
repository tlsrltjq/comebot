package com.giseop.comebot.telegram.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.telegram.service.TelegramTestMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TelegramTestMessageController.class)
class TelegramTestMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelegramTestMessageService telegramTestMessageService;

    @Test
    void sendTestMessageCallsServiceForValidMessage() throws Exception {
        when(telegramTestMessageService.sendTestMessage("hello")).thenReturn(true);

        mockMvc.perform(post("/api/telegram/test-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.message").value("hello"));

        verify(telegramTestMessageService).sendTestMessage("hello");
    }

    @Test
    void sendTestMessageReturnsBadRequestWhenMessageIsBlank() throws Exception {
        mockMvc.perform(post("/api/telegram/test-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(telegramTestMessageService);
    }

    @Test
    void sendTestMessageReturnsSentFalseWhenServiceReturnsFalse() throws Exception {
        when(telegramTestMessageService.sendTestMessage("hello")).thenReturn(false);

        mockMvc.perform(post("/api/telegram/test-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(false))
                .andExpect(jsonPath("$.message").value("hello"));
    }

    @Test
    void sendTestMessageDoesNotExposeTokenOrChatId() throws Exception {
        when(telegramTestMessageService.sendTestMessage("hello")).thenReturn(false);

        mockMvc.perform(post("/api/telegram/test-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botToken").doesNotExist())
                .andExpect(jsonPath("$.chatId").doesNotExist());
    }
}
