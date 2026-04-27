package com.giseop.comebot.telegram.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.telegram.sender.TelegramSendReason;
import com.giseop.comebot.telegram.service.TelegramTestMessageService;
import com.giseop.comebot.telegram.service.TelegramTestMessageResult;
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
        when(telegramTestMessageService.sendTestMessage("hello"))
                .thenReturn(new TelegramTestMessageResult(true, TelegramSendReason.SENT));

        mockMvc.perform(post("/api/telegram/test-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.message").value("hello"))
                .andExpect(jsonPath("$.reason").value("SENT"));

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
        when(telegramTestMessageService.sendTestMessage("hello"))
                .thenReturn(new TelegramTestMessageResult(false, TelegramSendReason.TELEGRAM_DISABLED));

        mockMvc.perform(post("/api/telegram/test-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(false))
                .andExpect(jsonPath("$.message").value("hello"))
                .andExpect(jsonPath("$.reason").value("TELEGRAM_DISABLED"));
    }

    @Test
    void sendTestMessageDoesNotExposeTokenOrChatId() throws Exception {
        when(telegramTestMessageService.sendTestMessage("hello"))
                .thenReturn(new TelegramTestMessageResult(false, TelegramSendReason.TELEGRAM_NOT_CONFIGURED));

        mockMvc.perform(post("/api/telegram/test-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botToken").doesNotExist())
                .andExpect(jsonPath("$.chatId").doesNotExist());
    }

    @Test
    void sendTestMessageReturnsApiFailedReason() throws Exception {
        when(telegramTestMessageService.sendTestMessage("hello"))
                .thenReturn(new TelegramTestMessageResult(false, TelegramSendReason.TELEGRAM_API_FAILED));

        mockMvc.perform(post("/api/telegram/test-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(false))
                .andExpect(jsonPath("$.reason").value("TELEGRAM_API_FAILED"));
    }
}
