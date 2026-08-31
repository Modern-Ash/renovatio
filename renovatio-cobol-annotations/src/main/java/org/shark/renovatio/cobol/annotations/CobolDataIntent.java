package org.shark.renovatio.cobol.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a generated Java field or type with reviewed COBOL data-layout intent.
 *
 * <p>Informational only: it changes no field type, initializer, accessor, or control flow. The
 * deterministic OpenRewrite pass attaches it when an {@code ACCEPTED} {@code DATA_INTENT} annotation
 * in the validated {@code cobol-annotated-ir.v1} sidecar resolves to the annotated node.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface CobolDataIntent {

    String nodeId();

    String annotationId();

    Construction construction();

    String interpretation();

    String[] assumptions();

    enum Construction { REDEFINES, OCCURS_DEPENDING_ON }
}
