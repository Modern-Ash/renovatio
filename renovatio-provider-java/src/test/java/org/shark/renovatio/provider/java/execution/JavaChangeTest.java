package org.shark.renovatio.provider.java.execution;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JavaChangeTest {

    @Test
    void record_fields_areAccessible() {
        JavaChange ch = new JavaChange("/f.java", "diff");
        assertEquals("/f.java", ch.file());
        assertEquals("diff", ch.diff());
    }
}

