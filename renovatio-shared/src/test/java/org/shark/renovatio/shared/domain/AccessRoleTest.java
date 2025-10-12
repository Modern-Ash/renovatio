package org.shark.renovatio.shared.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessRoleTest {

    @Test
    void fromString_parses_case_insensitive_and_handles_invalid() {
        assertEquals(AccessRole.ADMIN, AccessRole.fromString("admin"));
        assertEquals(AccessRole.MANAGER, AccessRole.fromString("Manager"));
        assertEquals(AccessRole.VIEWER, AccessRole.fromString("VIEWER"));
        assertNull(AccessRole.fromString("unknown"));
        assertNull(AccessRole.fromString(null));
    }
}

