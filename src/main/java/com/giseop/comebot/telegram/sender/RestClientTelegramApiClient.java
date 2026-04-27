package com.giseop.comebot.telegram.sender;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientTelegramApiClient implements TelegramApiClient {

    private static final String TELEGRAM_API_BASE_URL = "https://api.telegram.org";

    private final RestClient restClient;

    public RestClientTelegramApiClient() {
        this(RestClient.builder().baseUrl(TELEGRAM_API_BASE_URL).build());
    }

    RestClientTelegramApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void sendMessage(String botToken, String chatId, String text) {
        sendMessage(botToken, chatId, text, null);
    }

    @Override
    public void sendMessage(String botToken, String chatId, String text, Object replyMarkup) {
        restClient.post()
                .uri("/bot{botToken}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SendMessageRequest(chatId, text, replyMarkup))
                .retrieve()
                .toBodilessEntity();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record SendMessageRequest(
            String chat_id,
            String text,
            Object reply_markup
    ) {
    }
}
