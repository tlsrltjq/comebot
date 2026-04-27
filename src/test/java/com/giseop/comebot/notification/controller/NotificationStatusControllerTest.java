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
        when(notificationProperties.isEnabled()).thenReturn(false);

        mockMvc.perform(get("/api/notifications/status"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatusReturnsEnabledField() throws Exception {
        when(notificationProperties.isEnabled()).thenReturn(true);

        mockMvc.perform(get("/api/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void getStatusReturnsDefaultDisabledValue() throws Exception {
        when(notificationProperties.isEnabled()).thenReturn(false);

        mockMvc.perform(get("/api/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
