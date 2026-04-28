package com.giseop.comebot.telegram.inbound;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.inbound.offset.TelegramUpdateOffsetRepository;
import com.giseop.comebot.telegram.sender.TelegramApiClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class TelegramPollingRunnerTest {

    @Test
    void inboundDisabledDoesNotCallGetUpdates() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);

        runner(configuredTelegramProperties(), inboundProperties(false), updateClient, mock(TelegramCommandService.class))
                .poll();

        verify(updateClient, never()).getUpdates("token", 0);
    }

    @Test
    void telegramNotConfiguredDoesNotCallGetUpdates() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramProperties telegramProperties = new TelegramProperties();
        telegramProperties.setEnabled(true);

        runner(telegramProperties, inboundProperties(true), updateClient, mock(TelegramCommandService.class))
                .poll();

        verify(updateClient, never()).getUpdates("token", 0);
    }

    @Test
    void enabledAndConfiguredCallsGetUpdatesAndHandlesCommand() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.message(10, "/status", "chat-id")));

        runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService).poll();

        verify(updateClient).getUpdates("token", 0);
        verify(commandService).handle("/status");
    }

    @Test
    void storedOffsetIsUsedForGetUpdates() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramUpdateOffsetRepository offsetRepository = offsetRepository(42);

        runner(
                configuredTelegramProperties(),
                inboundProperties(true),
                updateClient,
                mock(TelegramCommandService.class),
                mock(TelegramApiClient.class),
                offsetRepository
        ).poll();

        verify(updateClient).getUpdates("token", 42);
    }

    @Test
    void successfulUpdateStoresNextOffset() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        TelegramUpdateOffsetRepository offsetRepository = offsetRepository(0);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.message(10, "/status", "chat-id")));

        runner(
                configuredTelegramProperties(),
                inboundProperties(true),
                updateClient,
                commandService,
                mock(TelegramApiClient.class),
                offsetRepository
        ).poll();

        verify(offsetRepository).saveNextOffset(11);
    }

    @Test
    void unauthorizedMessageDoesNotHandleCommand() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.message(10, "/status", "other-chat")));

        runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService).poll();

        verify(commandService, never()).handle("/status");
    }

    @Test
    void commandHandlingFailureDoesNotPropagate() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.message(10, "/run KRW-BTC", "chat-id")));
        org.mockito.Mockito.doThrow(new IllegalStateException("failed"))
                .when(commandService)
                .handle("/run KRW-BTC");

        assertThatCode(() -> runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService).poll())
                .doesNotThrowAnyException();
    }

    @Test
    void commandHandlingFailureDoesNotStoreNextOffset() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        TelegramUpdateOffsetRepository offsetRepository = offsetRepository(0);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.message(10, "/run KRW-BTC", "chat-id")));
        org.mockito.Mockito.doThrow(new IllegalStateException("failed"))
                .when(commandService)
                .handle("/run KRW-BTC");

        runner(
                configuredTelegramProperties(),
                inboundProperties(true),
                updateClient,
                commandService,
                mock(TelegramApiClient.class),
                offsetRepository
        ).poll();

        verify(offsetRepository, never()).saveNextOffset(11);
    }

    @Test
    void pollingFailureDoesNotStoreNextOffset() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramUpdateOffsetRepository offsetRepository = offsetRepository(10);
        when(updateClient.getUpdates("token", 10)).thenThrow(new IllegalStateException("failed"));

        runner(
                configuredTelegramProperties(),
                inboundProperties(true),
                updateClient,
                mock(TelegramCommandService.class),
                mock(TelegramApiClient.class),
                offsetRepository
        ).poll();

        verify(offsetRepository, never()).saveNextOffset(org.mockito.Mockito.anyLong());
    }

    @Test
    void callbackHandlingFailureDoesNotPropagate() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.callback(10, "RUN:KRW-BTC", "chat-id", "callback-1")));
        org.mockito.Mockito.doThrow(new IllegalStateException("failed"))
                .when(commandService)
                .handleCallback("RUN:KRW-BTC");

        assertThatCode(() -> runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService).poll())
                .doesNotThrowAnyException();
    }

    @Test
    void allowedRunCallbackHandlesCommandAndAnswersCallbackQuery() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        TelegramApiClient apiClient = mock(TelegramApiClient.class);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.callback(10, "RUN:KRW-BTC", "chat-id", "callback-1")));

        runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService, apiClient).poll();

        verify(commandService).handleCallback("RUN:KRW-BTC");
        verify(apiClient).answerCallbackQuery("token", "callback-1");
    }

    @Test
    void unauthorizedRunCallbackDoesNotHandleCommandButAnswersCallbackQuery() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        TelegramApiClient apiClient = mock(TelegramApiClient.class);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.callback(10, "RUN:KRW-BTC", "other-chat", "callback-1")));

        runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService, apiClient).poll();

        verify(commandService, never()).handleCallback("RUN:KRW-BTC");
        verify(apiClient).answerCallbackQuery("token", "callback-1");
    }

    @Test
    void unknownCallbackAnswersCallbackQuery() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        TelegramApiClient apiClient = mock(TelegramApiClient.class);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.callback(10, "UNKNOWN", "chat-id", "callback-1")));

        runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService, apiClient).poll();

        verify(commandService).handleCallback("UNKNOWN");
        verify(apiClient).answerCallbackQuery("token", "callback-1");
    }

    private TelegramPollingRunner runner(
            TelegramProperties telegramProperties,
            TelegramInboundProperties inboundProperties,
            TelegramUpdateClient updateClient,
            TelegramCommandService commandService
    ) {
        return runner(
                telegramProperties,
                inboundProperties,
                updateClient,
                commandService,
                mock(TelegramApiClient.class),
                offsetRepository(0)
        );
    }

    private TelegramPollingRunner runner(
            TelegramProperties telegramProperties,
            TelegramInboundProperties inboundProperties,
            TelegramUpdateClient updateClient,
            TelegramCommandService commandService,
            TelegramApiClient telegramApiClient
    ) {
        return runner(
                telegramProperties,
                inboundProperties,
                updateClient,
                commandService,
                telegramApiClient,
                offsetRepository(0)
        );
    }

    private TelegramPollingRunner runner(
            TelegramProperties telegramProperties,
            TelegramInboundProperties inboundProperties,
            TelegramUpdateClient updateClient,
            TelegramCommandService commandService,
            TelegramApiClient telegramApiClient,
            TelegramUpdateOffsetRepository offsetRepository
    ) {
        return new TelegramPollingRunner(
                telegramProperties,
                inboundProperties,
                updateClient,
                commandService,
                telegramApiClient,
                offsetRepository
        );
    }

    private TelegramUpdateOffsetRepository offsetRepository(long offset) {
        TelegramUpdateOffsetRepository repository = mock(TelegramUpdateOffsetRepository.class);
        when(repository.getNextOffset()).thenReturn(offset);
        return repository;
    }

    private TelegramProperties configuredTelegramProperties() {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setBotToken("token");
        properties.setChatId("chat-id");
        return properties;
    }

    private TelegramInboundProperties inboundProperties(boolean enabled) {
        TelegramInboundProperties properties = new TelegramInboundProperties();
        properties.setEnabled(enabled);
        return properties;
    }
}
