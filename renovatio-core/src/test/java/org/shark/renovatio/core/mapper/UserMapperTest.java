package org.shark.renovatio.core.mapper;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.core.dto.UserDto;
import org.shark.renovatio.core.entity.UserEntity;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    @Test
    void toDto_and_toEntity_basicMapping() {
        UserEntity e = new UserEntity();
        e.setId(42L);
        e.setName("Alice");
        e.setEmail("a@example.com");

        UserDto dto = UserMapper.INSTANCE.toDto(e);
        assertEquals(42L, dto.getId());
        // name -> nombre doesn't have explicit mapping, so it may be null
        assertNull(dto.getNombre());
        assertEquals("a@example.com", dto.getEmail());

        UserDto src = new UserDto(7L, "Bob", "b@example.com");
        UserEntity back = UserMapper.INSTANCE.toEntity(src);
        assertEquals(7L, back.getId());
        // nombre -> name also doesn't have mapping; expect null
        assertNull(back.getName());
        assertEquals("b@example.com", back.getEmail());
    }
}

