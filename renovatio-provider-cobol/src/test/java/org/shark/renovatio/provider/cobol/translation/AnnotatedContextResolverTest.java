package org.shark.renovatio.provider.cobol.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedIdentity;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.annotated.DomainNamingPayload;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotatedContextResolverTest {

    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    void prefersInlineSidecarOverExplicitPath(@TempDir Path dir) throws Exception {
        CobolIntermediateModel model = model();
        AnnotatedCobolModel inline = sidecar(model, "inlineName");
        Path cobolPath = dir.resolve("SAMPLE.cob");
        Path explicitPath = dir.resolve("explicit.annotated.json");
        Files.writeString(cobolPath, COBOL);
        mapper.writeValue(explicitPath.toFile(), sidecar(model, "pathName"));

        AnnotatedContextResolver.Resolution resolution = new AnnotatedContextResolver(mapper).resolve(
                new AnnotatedContextResolver.Request(Optional.of(inline), Optional.of(explicitPath), cobolPath),
                model);

        assertThat(resolution.context()).isPresent();
        assertThat(resolution.context().orElseThrow().sidecar()).isSameAs(inline);
    }

    @Test
    void fallsThroughToLegacyOnStaleSiblingSidecar(@TempDir Path dir) throws Exception {
        CobolIntermediateModel model = model();
        Path cobolPath = dir.resolve("SAMPLE.cob");
        Files.writeString(cobolPath, COBOL);
        String stale = mapper.writeValueAsString(sidecar(model, "staleName"))
                .replace(new CobolIrIdentityProjector().baseIrHash(model), "0".repeat(64));
        Files.writeString(dir.resolve("SAMPLE.annotated.json"), stale);

        AnnotatedContextResolver.Resolution resolution = new AnnotatedContextResolver(mapper).resolve(
                new AnnotatedContextResolver.Request(Optional.empty(), Optional.empty(), cobolPath), model);

        assertThat(resolution.context()).isEmpty();
        assertThat(resolution.diagnostics()).anyMatch(diagnostic -> diagnostic.contains("baseIrHash"));
    }

    @Test
    void usesSiblingSidecarWhenValid(@TempDir Path dir) throws Exception {
        CobolIntermediateModel model = model();
        AnnotatedCobolModel sidecar = sidecar(model, "clientFullName");
        Path cobolPath = dir.resolve("SAMPLE.cob");
        Files.writeString(cobolPath, COBOL);
        mapper.writeValue(dir.resolve("SAMPLE.annotated.json").toFile(), sidecar);

        AnnotatedContextResolver.Resolution resolution = new AnnotatedContextResolver(mapper).resolve(
                new AnnotatedContextResolver.Request(Optional.empty(), Optional.empty(), cobolPath), model);

        assertThat(resolution.context()).isPresent();
        assertThat(resolution.diagnostics()).isEmpty();
    }

    private CobolIntermediateModel model() {
        return new SimpleCobolIrParser().parse(COBOL);
    }

    private AnnotatedCobolModel sidecar(CobolIntermediateModel model, String suggestedName) {
        CobolIrIdentityProjector projector = new CobolIrIdentityProjector();
        String nodeId = projector.nodes(model).stream()
                .filter(node -> node.nodeKind() == AnnotatedNodeKind.DATA_ITEM)
                .findFirst().orElseThrow().nodeId();
        DomainNamingPayload payload = new DomainNamingPayload(suggestedName, "Customers", "clearer name");
        double confidence = 0.9;
        AnnotationProvenance provenance = new AnnotationProvenance("offline", "fake",
                "cobol.domain.naming", "v1", "domain-naming.v1", "1".repeat(64),
                AnnotatedIdentity.outputHash(AnnotationFamily.DOMAIN_NAMING, payload, confidence),
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS);
        CobolAnnotation annotation = new CobolAnnotation(
                AnnotatedIdentity.annotationId(nodeId, AnnotationFamily.DOMAIN_NAMING, provenance),
                nodeId, AnnotatedNodeKind.DATA_ITEM, AnnotationFamily.DOMAIN_NAMING, payload,
                confidence, provenance,
                new AnnotationReview(AnnotationReview.ReviewState.PROPOSED, null, null, null));
        return new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                CobolIrIdentityProjector.BASE_IR_VERSION, projector.baseIrHash(model), List.of(annotation));
    }
}
