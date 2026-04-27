package com.giseop.comebot.notification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.giseop.comebot.notification.NotificationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationStatusController.class)
class NotificationStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationProperties notificationProperties;

    @Test
    void getStatusReturnsOk() throws Exception {
        defaultProperties();

        mockMvc.perform(get("/api/notifications/status"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatusReturnsAllFields() throws Exception {
        when(notificationProperties.isEnabled()).thenReturn(true);
        when(notificationProperties.isSendHold()).thenReturn(true);
        when(notificationProperties.isSendFilled()).thenReturn(false);
        when(notificationProperties.isSendRejected()).thenReturn(true);

        mockMvc.perform(get("/api/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.sendHold").value(true))
                .andExpect(jsonPath("$.sendFilled").value(false))
                .andExpect(jsonPath("$.sendRejected").value(true));
    }

    @Test
    void getStatusReturnsDefaultValues() throws Exception {
        defaultProperties();

        mockMvc.perform(get("/api/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.sendHold").value(false))
                .andExpect(jsonPath("$.sendFilled").value(true))
                .andExpect(jsonPath("$.sendRejected").value(true));
    }

    private void defaultProperties() {
        when(notificationProperties.isEnabled()).thenReturn(false);
        when(notificationProperties.isSendHold()).thenReturn(false);
        when(notificationProperties.isSendFilled()).thenReturn(true);
        when(notificationProperties.isSendRejected()).thenReturn(true);
    }
}
