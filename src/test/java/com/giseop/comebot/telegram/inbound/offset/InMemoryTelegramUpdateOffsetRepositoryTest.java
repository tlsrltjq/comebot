package com.giseop.comebot.telegram.inbound.offset;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryTelegramUpdateOffsetRepositoryTest {

    @Test
    void saveNextOffsetStoresLargestOffset() {
        InMemoryTelegramUpdateOffsetRepository repository = new InMemoryTelegramUpdateOffsetRepository();

        repository.saveNextOffset(11);
        repository.saveNextOffset(5);

        assertThat(repository.getNextOffset()).isEqualTo(11);
    }
}
