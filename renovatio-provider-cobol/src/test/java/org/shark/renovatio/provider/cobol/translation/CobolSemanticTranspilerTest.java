package org.shark.renovatio.provider.cobol.translation;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.provider.java.OpenRewriteRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CobolSemanticTranspilerTest {

    private static final String COBOL_SAMPLE = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE1.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            01 CUSTOMER-RATING PIC 9(2).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'JOHN' TO CUSTOMER-NAME.
            END-PARA.
            """;

    private static final String JAVA_STUB = """
            package sample;
            public class SampleDto {
                private String customerName;
                public String getCustomerName() { return customerName; }
                public void setCustomerName(String customerName) { this.customerName = customerName; }
                public Integer getCustomerRating() { return 0; }
            }
            public class SampleService {
                public SampleDto process(SampleDto input) {
                    // TODO: Implement COBOL business logic
                    SampleDto output = new SampleDto();
                    return output;
                }
            }
            """;

    @Test
    void shouldEnrichServiceImplementation() {
        CobolIntermediateModelService modelService = new CobolIntermediateModelService();
        CobolIntermediateModel model = modelService.parse(COBOL_SAMPLE);
        CobolSemanticTranspiler transpiler = new CobolSemanticTranspiler(new OpenRewriteRunner());

        String enriched = transpiler.enrichServiceImplementation(JAVA_STUB, model);
        assertThat(enriched).contains("output.setCustomerName('JOHN');");
    }
}
