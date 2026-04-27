package com.giseop.comebot.telegram.inbound;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.telegram.TelegramProperties;
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
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.message(10, "/help")));

        runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService).poll();

        verify(updateClient).getUpdates("token", 0);
        verify(commandService).handle("/help");
    }

    @Test
    void commandHandlingFailureDoesNotPropagate() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.message(10, "/run KRW-BTC")));
        org.mockito.Mockito.doThrow(new IllegalStateException("failed"))
                .when(commandService)
                .handle("/run KRW-BTC");

        assertThatCode(() -> runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService).poll())
                .doesNotThrowAnyException();
    }

    @Test
    void callbackHandlingFailureDoesNotPropagate() {
        TelegramUpdateClient updateClient = mock(TelegramUpdateClient.class);
        TelegramCommandService commandService = mock(TelegramCommandService.class);
        when(updateClient.getUpdates("token", 0)).thenReturn(List.of(TelegramUpdate.callback(10, "RUN:KRW-BTC")));
        org.mockito.Mockito.doThrow(new IllegalStateException("failed"))
                .when(commandService)
                .handleCallback("RUN:KRW-BTC");

        assertThatCode(() -> runner(configuredTelegramProperties(), inboundProperties(true), updateClient, commandService).poll())
                .doesNotThrowAnyException();
    }

    private TelegramPollingRunner runner(
            TelegramProperties telegramProperties,
            TelegramInboundProperties inboundProperties,
            TelegramUpdateClient updateClient,
            TelegramCommandService commandService
    ) {
        return new TelegramPollingRunner(telegramProperties, inboundProperties, updateClient, commandService);
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
