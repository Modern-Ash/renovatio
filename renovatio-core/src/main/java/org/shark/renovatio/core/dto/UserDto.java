package org.shark.renovatio.core.dto;

import lombok.Builder;
import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String nombre;
    private String email;

    public UserDto() {
    }

    @Builder
    public UserDto(Long id, String nombre, String email) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
    }
}
