package org.shark.renovatio.core.entity;

import lombok.Data;

/**
 * Simple user entity for demonstrating MapStruct mapping.
 */
@Data
public class UserEntity {

    private Long id;
    private String name;
    private String email;
}
