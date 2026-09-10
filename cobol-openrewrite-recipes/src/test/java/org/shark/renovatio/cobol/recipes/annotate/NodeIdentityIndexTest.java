package org.shark.renovatio.cobol.recipes.annotate;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;

import static org.assertj.core.api.Assertions.assertThat;

class NodeIdentityIndexTest {

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

    private CobolIntermediateModel model() {
        return new SimpleCobolIrParser().parse(COBOL);
    }

    @Test
    void resolvesDataItemNodeIdToCobolName() {
        CobolIntermediateModel model = model();
        String dataItemNodeId = new CobolIrIdentityProjector().nodes(model).stream()
                .filter(n -> n.nodeKind() == AnnotatedNodeKind.DATA_ITEM)
                .findFirst().orElseThrow().nodeId();

        NodeIdentityIndex index = new NodeIdentityIndex(model);
        var resolved = index.resolve(dataItemNodeId, AnnotatedNodeKind.DATA_ITEM);

        assertThat(resolved).isPresent();
        assertThat(resolved.get().cobolName()).isEqualTo("CUSTOMER-NAME");
    }

    @Test
    void resolvesParagraphNodeIdToCobolName() {
        CobolIntermediateModel model = model();
        String paragraphNodeId = new CobolIrIdentityProjector().nodes(model).stream()
                .filter(n -> n.nodeKind() == AnnotatedNodeKind.PARAGRAPH)
                .findFirst().orElseThrow().nodeId();

        var resolved = new NodeIdentityIndex(model).resolve(paragraphNodeId, AnnotatedNodeKind.PARAGRAPH);

        assertThat(resolved).isPresent();
        assertThat(resolved.get().cobolName()).isEqualTo("MAIN-PARA");
    }

    @Test
    void returnsEmptyOnKindMismatchOrUnknownId() {
        NodeIdentityIndex index = new NodeIdentityIndex(model());
        assertThat(index.resolve("deadbeef".repeat(8), AnnotatedNodeKind.DATA_ITEM)).isEmpty();

        String dataItemNodeId = new CobolIrIdentityProjector().nodes(model()).stream()
                .filter(n -> n.nodeKind() == AnnotatedNodeKind.DATA_ITEM)
                .findFirst().orElseThrow().nodeId();
        assertThat(index.resolve(dataItemNodeId, AnnotatedNodeKind.PARAGRAPH)).isEmpty();
    }

    @Test
    void derivesJavaIdentifiers() {
        assertThat(NodeIdentityIndex.toJavaFieldName("CUSTOMER-NAME")).isEqualTo("customerName");
        assertThat(NodeIdentityIndex.toJavaAccessorStem("CUSTOMER-NAME")).isEqualTo("CustomerName");
        assertThat(NodeIdentityIndex.toJavaMethodName("MAIN-PARA")).isEqualTo("mainPara");
    }
}
