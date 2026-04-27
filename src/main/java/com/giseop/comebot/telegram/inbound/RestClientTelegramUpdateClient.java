package com.giseop.comebot.telegram.inbound;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientTelegramUpdateClient implements TelegramUpdateClient {

    private static final String TELEGRAM_API_BASE_URL = "https://api.telegram.org";

    private final RestClient restClient;

    public RestClientTelegramUpdateClient() {
        this(RestClient.builder().baseUrl(TELEGRAM_API_BASE_URL).build());
    }

    RestClientTelegramUpdateClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<TelegramUpdate> getUpdates(String botToken, long offset) {
        GetUpdatesResponse response = restClient.get()
                .uri("/bot{botToken}/getUpdates?offset={offset}", botToken, offset)
                .retrieve()
                .body(GetUpdatesResponse.class);

        if (response == null || response.result() == null) {
            return List.of();
        }

        return response.result().stream()
                .map(result -> new TelegramUpdate(result.update_id(), textOf(result)))
                .filter(update -> update.text() != null && !update.text().isBlank())
                .toList();
    }

    private String textOf(UpdateResult result) {
        if (result.message() == null) {
            return null;
        }
        return result.message().text();
    }

    private record GetUpdatesResponse(
            boolean ok,
            List<UpdateResult> result
    ) {
    }

    private record UpdateResult(
            long update_id,
            Message message
    ) {
    }

    private record Message(
            String text
    ) {
    }
}
