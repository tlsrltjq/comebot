package com.giseop.comebot.database;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DatabaseHealthController.class)
class DatabaseHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatabaseHealthService databaseHealthService;

    @Test
    void getStatusReturnsConnectedTrue() throws Exception {
        when(databaseHealthService.check()).thenReturn(new DatabaseHealthResult(true, "PostgreSQL"));

        mockMvc.perform(get("/api/database/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.database").value("PostgreSQL"));
    }

    @Test
    void getStatusReturnsConnectedFalse() throws Exception {
        when(databaseHealthService.check()).thenReturn(new DatabaseHealthResult(false, "PostgreSQL"));

        mockMvc.perform(get("/api/database/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.database").value("PostgreSQL"));
    }

    @Test
    void getStatusDoesNotExposeDatasourceSecrets() throws Exception {
        when(databaseHealthService.check()).thenReturn(new DatabaseHealthResult(false, "PostgreSQL"));

        mockMvc.perform(get("/api/database/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.url").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist());
    }
}
