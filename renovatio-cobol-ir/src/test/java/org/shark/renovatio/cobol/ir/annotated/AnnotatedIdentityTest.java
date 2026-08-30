package org.shark.renovatio.cobol.ir.annotated;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnnotatedIdentityTest {

    private static final String NODE_ID = "1".repeat(64);
    private static final String INPUT_HASH = "2".repeat(64);
    private static final String OUTPUT_HASH = "3".repeat(64);

    @Test
    void canonicalObjectsIgnoreInsertionOrderAndEscapeStrings() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("z", List.of("line\nfeed", true));
        first.put("a", 1d);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("a", 1);
        second.put("z", List.of("line\nfeed", true));

        assertEquals("{\"a\":1,\"z\":[\"line\\nfeed\",true]}", AnnotatedIdentity.canonical(first));
        assertEquals(AnnotatedIdentity.canonical(first), AnnotatedIdentity.canonical(second));
    }

    @Test
    void annotationAndCacheDomainsProduceStableDistinctHashes() {
        AnnotationProvenance provenance = provenance("v1");

        String annotation = AnnotatedIdentity.annotationId(NODE_ID, AnnotationFamily.DOMAIN_NAMING, provenance);
        String cache = AnnotatedIdentity.cacheKey(NODE_ID, AnnotationFamily.DOMAIN_NAMING, provenance);

        assertEquals(64, annotation.length());
        assertNotEquals(annotation, cache);
        assertEquals(annotation, AnnotatedIdentity.annotationId(NODE_ID, AnnotationFamily.DOMAIN_NAMING, provenance));
        assertNotEquals(annotation, AnnotatedIdentity.annotationId(NODE_ID, AnnotationFamily.DOMAIN_NAMING, provenance("v2")));
    }

    @Test
    void outputHashBindsTypedPayloadAndConfidenceOnly() {
        DomainNamingPayload payload = new DomainNamingPayload("calculateInterest", "Domain terminology", "collections");

        String first = AnnotatedIdentity.outputHash(AnnotationFamily.DOMAIN_NAMING, payload, 0.75);
        String repeated = AnnotatedIdentity.outputHash(AnnotationFamily.DOMAIN_NAMING, payload, 0.75);

        assertEquals(first, repeated);
        assertNotEquals(first, AnnotatedIdentity.outputHash(AnnotationFamily.DOMAIN_NAMING, payload, 0.76));
        assertNotEquals(first, AnnotatedIdentity.outputHash(AnnotationFamily.DOMAIN_NAMING,
                new DomainNamingPayload("calculateFee", "Domain terminology", "collections"), 0.75));
    }

    @Test
    void rejectsNonFiniteCanonicalNumbers() {
        assertThrows(IllegalArgumentException.class,
                () -> AnnotatedIdentity.canonical(Map.of("number", Double.NaN)));
    }

    @Test
    void canonicalNumbersUseEcmascriptThresholdsRequiredByRfc8785() {
        assertEquals("{\"high\":1e+21,\"low\":1e-7,\"plainHigh\":100000000000000000000,\"plainLow\":0.000001}",
                AnnotatedIdentity.canonical(Map.of(
                        "low", 1e-7,
                        "plainLow", 1e-6,
                        "plainHigh", 1e20,
                        "high", 1e21)));
    }

    private AnnotationProvenance provenance(String promptVersion) {
        return new AnnotationProvenance("offline", "fake", "cobol.domain.naming", promptVersion,
                "domain-naming.v1", INPUT_HASH, OUTPUT_HASH, "tool-20260830t12345678901234z",
                AnnotationProvenance.CacheDisposition.MISS);
    }
}
