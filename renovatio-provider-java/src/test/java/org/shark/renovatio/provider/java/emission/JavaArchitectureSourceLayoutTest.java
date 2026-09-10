package org.shark.renovatio.provider.java.emission;

import org.shark.renovatio.architecture.java.JavaArchitectureSourceLayout;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaArchitectureSourceLayoutTest {

    @Test
    void preservesTransactionScriptBytes() {
        Map<String, String> sources = Map.of("PayDTO.java", "package legacy;\npublic class PayDTO {}\n");

        assertEquals(sources, JavaArchitectureSourceLayout.align(sources));
    }

    @Test
    void alignsHexagonalPackagesAndCrossLayerImports() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("modules/payments/domain/model/PayDTO.java",
                "package legacy;\npublic class PayDTO {}\n");
        sources.put("modules/payments/application/port/in/PayService.java",
                "package legacy;\npublic interface PayService { PayDTO process(PayDTO input); }\n");
        sources.put("modules/payments/application/service/PayServiceImpl.java",
                "package legacy;\npublic class PayServiceImpl implements PayService { "
                        + "public PayDTO process(PayDTO input) { return input; } }\n");

        Map<String, String> aligned = JavaArchitectureSourceLayout.align(sources);

        assertTrue(aligned.get("modules/payments/domain/model/PayDTO.java")
                .startsWith("package org.shark.renovatio.generated.modules.payments.domain.model;"));
        assertTrue(aligned.get("modules/payments/application/port/in/PayService.java").contains(
                "import org.shark.renovatio.generated.modules.payments.domain.model.PayDTO;"));
        assertTrue(aligned.get("modules/payments/application/service/PayServiceImpl.java").contains(
                "import org.shark.renovatio.generated.modules.payments.application.port.in.PayService;"));
        assertTrue(aligned.get("modules/payments/application/service/PayServiceImpl.java").contains(
                "import org.shark.renovatio.generated.modules.payments.domain.model.PayDTO;"));
    }

    @Test
    void alignsArchitectureCanvasPackageRootsAndImports() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("com/acme/domain/PayDTO.java", "package legacy;\npublic class PayDTO {}\n");
        sources.put("com/acme/application/PayService.java",
                "package legacy;\npublic interface PayService { PayDTO process(PayDTO input); }\n");

        Map<String, String> aligned = JavaArchitectureSourceLayout.align(sources);

        assertTrue(aligned.get("com/acme/domain/PayDTO.java").startsWith("package com.acme.domain;"));
        assertTrue(aligned.get("com/acme/application/PayService.java")
                .contains("import com.acme.domain.PayDTO;"));
    }


    @Test
    void rejectsSourcesThatCannotBeAligned() {
        assertThrows(IllegalArgumentException.class, () -> JavaArchitectureSourceLayout.align(Map.of(
                "modules/payments/domain/model/PayDTO.java", "public class PayDTO {}")));
    }
}
