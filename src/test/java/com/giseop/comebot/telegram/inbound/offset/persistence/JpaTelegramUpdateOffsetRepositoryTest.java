package com.giseop.comebot.telegram.inbound.offset.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JpaTelegramUpdateOffsetRepositoryTest {

    private SpringDataTelegramUpdateOffsetJpaRepository springDataRepository;
    private JpaTelegramUpdateOffsetRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataTelegramUpdateOffsetJpaRepository.class);
        repository = new JpaTelegramUpdateOffsetRepository(springDataRepository);
    }

    @Test
    void getNextOffsetReturnsZeroWhenOffsetDoesNotExist() {
        when(springDataRepository.findById("telegram-get-updates")).thenReturn(Optional.empty());

        assertThat(repository.getNextOffset()).isZero();
    }

    @Test
    void getNextOffsetReturnsStoredOffset() {
        when(springDataRepository.findById("telegram-get-updates"))
                .thenReturn(Optional.of(new TelegramUpdateOffsetEntity(
                        "telegram-get-updates",
                        42,
                        Instant.parse("2026-04-29T00:00:00Z")
                )));

        assertThat(repository.getNextOffset()).isEqualTo(42);
    }

    @Test
    void saveNextOffsetStoresOffset() {
        when(springDataRepository.findById("telegram-get-updates")).thenReturn(Optional.empty());
        when(springDataRepository.save(any(TelegramUpdateOffsetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<TelegramUpdateOffsetEntity> captor = ArgumentCaptor.forClass(TelegramUpdateOffsetEntity.class);

        repository.saveNextOffset(11);

        verify(springDataRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("telegram-get-updates");
        assertThat(captor.getValue().getLastUpdateOffset()).isEqualTo(11);
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void saveNextOffsetDoesNotDecreaseStoredOffset() {
        when(springDataRepository.findById("telegram-get-updates"))
                .thenReturn(Optional.of(new TelegramUpdateOffsetEntity(
                        "telegram-get-updates",
                        42,
                        Instant.parse("2026-04-29T00:00:00Z")
                )));
        ArgumentCaptor<TelegramUpdateOffsetEntity> captor = ArgumentCaptor.forClass(TelegramUpdateOffsetEntity.class);

        repository.saveNextOffset(11);

        verify(springDataRepository).save(captor.capture());
        assertThat(captor.getValue().getLastUpdateOffset()).isEqualTo(42);
    }
}
