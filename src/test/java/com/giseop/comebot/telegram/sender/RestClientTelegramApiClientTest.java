package com.giseop.comebot.telegram.sender;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestClientTelegramApiClientTest {

    @Test
    void sendMessageCallsTelegramSendMessageApi() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(once(), requestTo("https://api.telegram.org/bottoken/sendMessage"))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json("{\"chat_id\":\"chat-id\",\"text\":\"body\"}"))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        new RestClientTelegramApiClient(builder.build()).sendMessage("token", "chat-id", "body");

        server.verify();
    }
}
