package org.shark.renovatio.core.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDtoTest {

    @Test
    void noArgsConstructor_and_setters_work() {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setNombre("Jane");
        dto.setEmail("jane@example.com");
        assertEquals(1L, dto.getId());
        assertEquals("Jane", dto.getNombre());
        assertEquals("jane@example.com", dto.getEmail());
    }
}

