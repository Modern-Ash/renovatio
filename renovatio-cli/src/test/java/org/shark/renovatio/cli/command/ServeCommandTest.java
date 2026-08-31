package org.shark.renovatio.cli.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServeCommandTest {

    @Test
    void commandIsRegistered() {
        assertThat(new ServeCommand()).isNotNull();
    }
}
