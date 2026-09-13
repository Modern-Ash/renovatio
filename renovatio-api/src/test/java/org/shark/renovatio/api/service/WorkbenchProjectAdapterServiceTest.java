package org.shark.renovatio.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.shared.security.WorkspaceRootPolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchProjectAdapterServiceTest {
    @Test
    void legacyAssetsRemainReadOnlyAndTargetsRequireDevelopmentMode(@TempDir Path root) throws Exception {
        WorkbenchProjectAdapterService adapter = new WorkbenchProjectAdapterService(WorkspaceRootPolicy.under(root));
        Files.writeString(root.resolve("PAYROLL.CBL"), "IDENTIFICATION DIVISION.");
        Files.createDirectories(root.resolve("generated"));
        Files.writeString(root.resolve("generated/App.java"), "class App {}");
        assertEquals(2, adapter.list(root, false).size());
        assertThrows(SecurityException.class, () -> adapter.write(root, "PAYROLL.CBL", "changed", true));
        assertThrows(SecurityException.class, () -> adapter.write(root, "generated/App.java", "changed", false));
        adapter.write(root, "generated/App.java", "changed", true);
        assertEquals("changed", adapter.read(root, "generated/App.java"));
    }

    @Test
    void generatedDataMigrationArtifactsAreWritableOnlyInDevelopmentMode(@TempDir Path root) throws Exception {
        WorkbenchProjectAdapterService adapter = new WorkbenchProjectAdapterService(WorkspaceRootPolicy.under(root));
        assertThrows(SecurityException.class, () -> adapter.write(root,
                "generated-data-migration/demo/staging-load.sql", "select 1;", false));

        adapter.write(root, "generated-data-migration/demo/README.md", "# Plan\n", true);
        adapter.write(root, "generated-data-migration/demo/staging-load.sql", "select 1;\n", true);
        adapter.write(root, "generated-data-migration/demo/dry-run-report.json", "{}\n", true);

        assertEquals("select 1;\n", adapter.read(root, "generated-data-migration/demo/staging-load.sql"));
        assertThrows(SecurityException.class, () -> adapter.write(root,
                "evidence/dry-run-report.json", "{}\n", true));
    }

    @Test
    void traversalIsRejected(@TempDir Path root) {
        WorkbenchProjectAdapterService adapter = new WorkbenchProjectAdapterService(WorkspaceRootPolicy.under(root));
        assertThrows(SecurityException.class, () -> adapter.read(root, "../outside.txt"));
    }
}
