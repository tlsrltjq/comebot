package com.giseop.comebot.scheduler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.scheduler.persistence.SchedulerControlSettingEntity;
import com.giseop.comebot.scheduler.persistence.SpringDataSchedulerControlSettingJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulerControlServiceTest {

    private CandidateSchedulerProperties candidateSchedulerProperties;
    private PositionExitSchedulerProperties positionExitSchedulerProperties;
    private SpringDataSchedulerControlSettingJpaRepository settingRepository;
    private SchedulerControlService service;

    @BeforeEach
    void setUp() {
        candidateSchedulerProperties = new CandidateSchedulerProperties();
        positionExitSchedulerProperties = new PositionExitSchedulerProperties();
        settingRepository = org.mockito.Mockito.mock(SpringDataSchedulerControlSettingJpaRepository.class);
        service = new SchedulerControlService(candidateSchedulerProperties, positionExitSchedulerProperties, settingRepository);
    }

    @Test
    void restorePersistedSettingAppliesSavedValuesOnStartup() {
        when(settingRepository.findById(SchedulerControlSettingEntity.DEFAULT_ID))
                .thenReturn(Optional.of(new SchedulerControlSettingEntity(false, 30000, Instant.now())));
        candidateSchedulerProperties.setEnabled(true);
        positionExitSchedulerProperties.setEnabled(true);
        candidateSchedulerProperties.setFixedDelayMs(60000);

        service.restorePersistedSetting();

        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.isEnabled()).isFalse();
        org.assertj.core.api.Assertions.assertThat(positionExitSchedulerProperties.isEnabled()).isFalse();
        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.getFixedDelayMs()).isEqualTo(30000);
    }

    @Test
    void restorePersistedSettingKeepsEnvironmentDefaultsWhenNoSettingExists() {
        when(settingRepository.findById(SchedulerControlSettingEntity.DEFAULT_ID)).thenReturn(Optional.empty());
        candidateSchedulerProperties.setEnabled(true);
        positionExitSchedulerProperties.setEnabled(true);
        candidateSchedulerProperties.setFixedDelayMs(60000);

        service.restorePersistedSetting();

        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.isEnabled()).isTrue();
        org.assertj.core.api.Assertions.assertThat(positionExitSchedulerProperties.isEnabled()).isTrue();
        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.getFixedDelayMs()).isEqualTo(60000);
    }

    @Test
    void restorePersistedSettingCanBeDisabledForSafeObservationStartup() {
        SchedulerControlService disabledRestoreService = new SchedulerControlService(
                candidateSchedulerProperties,
                positionExitSchedulerProperties,
                settingRepository,
                false
        );
        when(settingRepository.findById(SchedulerControlSettingEntity.DEFAULT_ID))
                .thenReturn(Optional.of(new SchedulerControlSettingEntity(true, 30000, Instant.now())));
        candidateSchedulerProperties.setEnabled(false);
        positionExitSchedulerProperties.setEnabled(false);
        candidateSchedulerProperties.setFixedDelayMs(60000);

        disabledRestoreService.restorePersistedSetting();

        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.isEnabled()).isFalse();
        org.assertj.core.api.Assertions.assertThat(positionExitSchedulerProperties.isEnabled()).isFalse();
        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.getFixedDelayMs()).isEqualTo(60000);
    }

    @Test
    void updateAppliesAndPersistsValues() {
        service.update(false, 30000L);

        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.isEnabled()).isFalse();
        org.assertj.core.api.Assertions.assertThat(positionExitSchedulerProperties.isEnabled()).isFalse();
        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.getFixedDelayMs()).isEqualTo(30000);
        verify(settingRepository).save(org.mockito.ArgumentMatchers.any(SchedulerControlSettingEntity.class));
    }

    @Test
    void updateCanChangeOnlyDelayAndPreserveAutoTradingState() {
        candidateSchedulerProperties.setEnabled(true);
        positionExitSchedulerProperties.setEnabled(true);

        service.update(null, 30000L);

        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.isEnabled()).isTrue();
        org.assertj.core.api.Assertions.assertThat(positionExitSchedulerProperties.isEnabled()).isTrue();
        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.getFixedDelayMs()).isEqualTo(30000);
    }

    @Test
    void updateRejectsUnsupportedDelay() {
        assertThatThrownBy(() -> service.update(true, 45000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restorePersistedSettingIgnoresInvalidDelayAndKeepsCurrentSettings() {
        // Simulate a stale DB row with an unsupported delay value (e.g. 15000)
        when(settingRepository.findById(SchedulerControlSettingEntity.DEFAULT_ID))
                .thenReturn(Optional.of(new SchedulerControlSettingEntity(false, 15000, Instant.now())));
        candidateSchedulerProperties.setEnabled(true);
        candidateSchedulerProperties.setFixedDelayMs(60000);

        // Should not throw; invalid value must be silently ignored
        service.restorePersistedSetting();

        // Settings must remain unchanged because the persisted value was invalid
        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.isEnabled()).isTrue();
        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.getFixedDelayMs()).isEqualTo(60000);
    }

    @Test
    void restorePersistedSettingDoesNothingWhenRepositoryIsNull() {
        SchedulerControlService serviceWithoutRepo = new SchedulerControlService(
                candidateSchedulerProperties, positionExitSchedulerProperties, (SpringDataSchedulerControlSettingJpaRepository) null
        );
        candidateSchedulerProperties.setEnabled(true);
        candidateSchedulerProperties.setFixedDelayMs(60000);

        // Must not throw
        serviceWithoutRepo.restorePersistedSetting();

        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.isEnabled()).isTrue();
        org.assertj.core.api.Assertions.assertThat(candidateSchedulerProperties.getFixedDelayMs()).isEqualTo(60000);
    }
}
