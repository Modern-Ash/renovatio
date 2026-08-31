package org.shark.renovatio.cobol.annotations;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobolDataIntentTest {

    @CobolDataIntent(nodeId = "n", annotationId = "a",
            construction = CobolDataIntent.Construction.REDEFINES,
            interpretation = "overlay", assumptions = {"x"})
    private String annotated;

    @Test
    void retainsAtRuntimeWithFieldAndTypeTargets() {
        Retention retention = CobolDataIntent.class.getAnnotation(Retention.class);
        Target target = CobolDataIntent.class.getAnnotation(Target.class);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertTrue(List.of(target.value()).containsAll(List.of(ElementType.FIELD, ElementType.TYPE)));
    }

    @Test
    void exposesPayloadThroughReflection() throws NoSuchFieldException {
        CobolDataIntent a = CobolDataIntentTest.class.getDeclaredField("annotated")
                .getAnnotation(CobolDataIntent.class);
        assertEquals("n", a.nodeId());
        assertEquals(CobolDataIntent.Construction.REDEFINES, a.construction());
        assertEquals(List.of("x"), List.of(a.assumptions()));
    }
}
