package org.shark.renovatio.provider.cobol.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItemWriter;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaGenerationServiceAnnotatedTest {

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

    @Test
    void usesCommittedSidecarAndWritesDroppedAnnotationReport(@TempDir Path workspacePath) throws Exception {
        Path cobolPath = workspacePath.resolve("sample.cob");
        Files.writeString(cobolPath, COBOL);
        CobolIntermediateModelService modelService = new CobolIntermediateModelService();
        CobolIntermediateModel model = modelService.parse(COBOL);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writeValue(workspacePath.resolve("sample.annotated.json").toFile(), sidecar(model));
        JavaGenerationService service = new JavaGenerationService(
                new CobolParsingService(CobolParsingService.Dialect.IBM),
                new TemplateCodeGenerationService(), modelService,
                new CobolSemanticTranspiler(new OpenRewriteRunner()), mapper);
        Workspace workspace = new Workspace("annotated", workspacePath.toString(), "main");

        StubResult result = service.generateInterfaceStubs(new NqlQuery(), workspace);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGeneratedCode().get("SampleDTO.java"))
                .contains("private String clientFullName")
                .contains("getClientFullName()")
                .contains("setClientFullName(");
        assertThat(result.getGeneratedCode().get("SampleServiceImpl.java"))
                .contains("setClientFullName(\"A\")");
        Path report = workspacePath.resolve(ManualActionItemWriter.DEFAULT_REPORT);
        assertThat(report).exists();
        assertThat(Files.readString(report)).contains("COBOL-ANNOTATION-REJECTED");
    }

    @Test
    void staleSidecarFallsBackAndWritesDiagnosticReport(@TempDir Path workspacePath) throws Exception {
        Path cobolPath = workspacePath.resolve("sample.cob");
        Files.writeString(cobolPath, COBOL);
        CobolIntermediateModelService modelService = new CobolIntermediateModelService();
        CobolIntermediateModel model = modelService.parse(COBOL);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        AnnotatedCobolModel current = sidecar(model);
        AnnotatedCobolModel stale = new AnnotatedCobolModel(current.schemaVersion(),
                current.baseIrVersion(), "0".repeat(64), current.annotations());
        mapper.writeValue(workspacePath.resolve("sample.annotated.json").toFile(), stale);
        JavaGenerationService service = new JavaGenerationService(
                new CobolParsingService(CobolParsingService.Dialect.IBM),
                new TemplateCodeGenerationService(), modelService,
                new CobolSemanticTranspiler(new OpenRewriteRunner()), mapper);
        Workspace workspace = new Workspace("annotated", workspacePath.toString(), "main");

        StubResult result = service.generateInterfaceStubs(new NqlQuery(), workspace);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGeneratedCode().get("SampleDTO.java"))
                .contains("private String customerName")
                .doesNotContain("clientFullName");
        Path report = workspacePath.resolve(ManualActionItemWriter.DEFAULT_REPORT);
        assertThat(report).exists();
        assertThat(Files.readString(report)).contains("COBOL-ANNOTATION-STALE");
    }

    @Test
    void cleanRunReplacesStaleActionItemReport(@TempDir Path workspacePath) throws Exception {
        Path cobolPath = workspacePath.resolve("sample.cob");
        Files.writeString(cobolPath, COBOL);
        Path report = workspacePath.resolve(ManualActionItemWriter.DEFAULT_REPORT);
        Files.createDirectories(report.getParent());
        Files.writeString(report,
                "{\"schemaVersion\":\"manual-action-item.v1\",\"items\":[{\"id\":\"stale\"}]}");
        CobolIntermediateModelService modelService = new CobolIntermediateModelService();
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        JavaGenerationService service = new JavaGenerationService(
                new CobolParsingService(CobolParsingService.Dialect.IBM),
                new TemplateCodeGenerationService(), modelService,
                new CobolSemanticTranspiler(new OpenRewriteRunner()), mapper);
        Workspace workspace = new Workspace("annotated", workspacePath.toString(), "main");

        StubResult result = service.generateInterfaceStubs(new NqlQuery(), workspace);

        assertThat(result.isSuccess()).isTrue();
        assertThat(mapper.readTree(report.toFile()).path("items")).isEmpty();
    }

    private AnnotatedCobolModel sidecar(CobolIntermediateModel model) {
        CobolIrIdentityProjector projector = new CobolIrIdentityProjector();
        String nodeId = projector.nodes(model).stream()
                .filter(node -> node.nodeKind() == AnnotatedNodeKind.DATA_ITEM)
                .findFirst().orElseThrow().nodeId();
        CobolAnnotation accepted = annotation(nodeId, "clientFullName", AnnotationReview.ReviewState.ACCEPTED,
                "1".repeat(64));
        CobolAnnotation rejected = annotation(nodeId, "ignoredName", AnnotationReview.ReviewState.REJECTED,
                "2".repeat(64));
        return new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                CobolIrIdentityProjector.BASE_IR_VERSION, projector.baseIrHash(model),
                List.of(rejected, accepted));
    }

    private CobolAnnotation annotation(String nodeId, String suggestedName,
                                       AnnotationReview.ReviewState reviewState, String inputHash) {
        DomainNamingPayload payload = new DomainNamingPayload(suggestedName, "Customers", "clearer name");
        double confidence = 0.9;
        AnnotationProvenance provenance = new AnnotationProvenance("offline", "fake",
                "cobol.domain.naming", "v1", "domain-naming.v1", inputHash,
                AnnotatedIdentity.outputHash(AnnotationFamily.DOMAIN_NAMING, payload, confidence),
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.HIT);
        return new CobolAnnotation(
                AnnotatedIdentity.annotationId(nodeId, AnnotationFamily.DOMAIN_NAMING, provenance),
                nodeId, AnnotatedNodeKind.DATA_ITEM, AnnotationFamily.DOMAIN_NAMING, payload,
                confidence, provenance,
                new AnnotationReview(reviewState, null, "reviewer",
                        Instant.parse("2026-01-01T00:00:00Z")));
    }
}
