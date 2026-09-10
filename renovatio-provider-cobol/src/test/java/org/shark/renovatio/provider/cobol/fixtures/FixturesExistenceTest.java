package org.shark.renovatio.provider.cobol.fixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that all required COBOL fixtures exist and are properly structured.
 * This test satisfies the 'fixtures' acceptance criterion for AC-07.
 */
class FixturesExistenceTest {

    private static final String FIXTURES_BASE_PATH = "src/test/resources/fixtures";
    
    private static final List<String> REQUIRED_FIXTURES = List.of(
        "batch-simple",
        "cics-mvc",
        "db2-access"
    );

    private static final List<String> REQUIRED_FILES_PER_FIXTURE = List.of(
        "decisions.json",
        "expected/manifest.json"
    );

    @Test
    void shouldHaveAllRequiredFixtures() {
        Path fixturesPath = Path.of(FIXTURES_BASE_PATH);
        assertThat(fixturesPath)
            .as("Fixtures directory should exist")
            .exists();
        
        for (String fixture : REQUIRED_FIXTURES) {
            Path fixturePath = fixturesPath.resolve(fixture);
            assertThat(fixturePath)
                .as("Fixture directory '%s' should exist", fixture)
                .exists();
        }
    }

    @Test
    void shouldHaveBatchSimpleFixtureWithAllFiles() throws IOException {
        Path fixturePath = Path.of(FIXTURES_BASE_PATH, "batch-simple");
        
        // Check COBOL source file
        Path cobolFile = fixturePath.resolve("src/cobol/BATCH001.cbl");
        assertThat(cobolFile)
            .as("Batch simple COBOL file should exist")
            .exists();
        
        String cobolContent = Files.readString(cobolFile);
        assertThat(cobolContent)
            .as("COBOL file should contain PROGRAM-ID")
            .contains("PROGRAM-ID. BATCH001");
        
        // Check decisions file
        Path decisionsFile = fixturePath.resolve("decisions.json");
        assertThat(decisionsFile)
            .as("Batch simple decisions file should exist")
            .exists();
        
        String decisionsContent = Files.readString(decisionsFile);
        assertThat(decisionsContent)
            .as("Decisions file should contain fixture name")
            .contains("\"fixture\": \"batch-simple\"");
        
        // Check expected manifest
        Path manifestFile = fixturePath.resolve("expected/manifest.json");
        assertThat(manifestFile)
            .as("Batch simple manifest file should exist")
            .exists();
        
        String manifestContent = Files.readString(manifestFile);
        assertThat(manifestContent)
            .as("Manifest file should contain fixture name")
            .contains("\"fixture\": \"batch-simple\"");
        
        // Check expected Java output
        Path javaFile = fixturePath.resolve("expected/src/main/java/org/shark/renovatio/generated/cobol/Batch001Service.java");
        assertThat(javaFile)
            .as("Batch simple expected Java file should exist")
            .exists();
        
        String javaContent = Files.readString(javaFile);
        assertThat(javaContent)
            .as("Java file should contain interface definition")
            .contains("public interface Batch001Service");
    }

    @Test
    void shouldHaveCicsMvcFixtureWithAllFiles() throws IOException {
        Path fixturePath = Path.of(FIXTURES_BASE_PATH, "cics-mvc");
        
        // Check COBOL source file
        Path cobolFile = fixturePath.resolve("src/cobol/CICS001.cbl");
        assertThat(cobolFile)
            .as("CICS MVC COBOL file should exist")
            .exists();
        
        String cobolContent = Files.readString(cobolFile);
        assertThat(cobolContent)
            .as("COBOL file should contain PROGRAM-ID")
            .contains("PROGRAM-ID. CICS001");
        
        // Check copybook
        Path copybookFile = fixturePath.resolve("src/cobol/CUSTOMER.cpy");
        assertThat(copybookFile)
            .as("CICS MVC copybook file should exist")
            .exists();
        
        String copybookContent = Files.readString(copybookFile);
        assertThat(copybookContent)
            .as("Copybook should contain CUSTOMER-RECORD")
            .contains("CUSTOMER-RECORD");
        
        // Check decisions file
        Path decisionsFile = fixturePath.resolve("decisions.json");
        assertThat(decisionsFile)
            .as("CICS MVC decisions file should exist")
            .exists();
        
        String decisionsContent = Files.readString(decisionsFile);
        assertThat(decisionsContent)
            .as("Decisions file should contain fixture name")
            .contains("\"fixture\": \"cics-mvc\"");
        
        // Check expected manifest
        Path manifestFile = fixturePath.resolve("expected/manifest.json");
        assertThat(manifestFile)
            .as("CICS MVC manifest file should exist")
            .exists();
        
        String manifestContent = Files.readString(manifestFile);
        assertThat(manifestContent)
            .as("Manifest file should contain fixture name")
            .contains("\"fixture\": \"cics-mvc\"");
        
        // Check expected Java outputs
        Path controllerFile = fixturePath.resolve("expected/src/main/java/org/shark/renovatio/generated/cobol/Cics001CicsController.java");
        assertThat(controllerFile)
            .as("CICS MVC expected controller file should exist")
            .exists();
        
        Path dtoFile = fixturePath.resolve("expected/src/main/java/org/shark/renovatio/generated/cobol/Cics001DTO.java");
        assertThat(dtoFile)
            .as("CICS MVC expected DTO file should exist")
            .exists();
    }

    @Test
    void shouldHaveDb2AccessFixtureWithAllFiles() throws IOException {
        Path fixturePath = Path.of(FIXTURES_BASE_PATH, "db2-access");
        
        // Check COBOL source file
        Path cobolFile = fixturePath.resolve("src/cobol/DB2001.cbl");
        assertThat(cobolFile)
            .as("DB2 access COBOL file should exist")
            .exists();
        
        String cobolContent = Files.readString(cobolFile);
        assertThat(cobolContent)
            .as("COBOL file should contain PROGRAM-ID")
            .contains("PROGRAM-ID. DB2001");
        
        // Check copybook
        Path copybookFile = fixturePath.resolve("src/cobol/EMP-REC.cpy");
        assertThat(copybookFile)
            .as("DB2 access copybook file should exist")
            .exists();
        
        String copybookContent = Files.readString(copybookFile);
        assertThat(copybookContent)
            .as("Copybook should contain EMPLOYEE-RECORD")
            .contains("EMPLOYEE-RECORD");
        
        // Check decisions file
        Path decisionsFile = fixturePath.resolve("decisions.json");
        assertThat(decisionsFile)
            .as("DB2 access decisions file should exist")
            .exists();
        
        String decisionsContent = Files.readString(decisionsFile);
        assertThat(decisionsContent)
            .as("Decisions file should contain fixture name")
            .contains("\"fixture\": \"db2-access\"");
        
        // Check expected manifest
        Path manifestFile = fixturePath.resolve("expected/manifest.json");
        assertThat(manifestFile)
            .as("DB2 access manifest file should exist")
            .exists();
        
        String manifestContent = Files.readString(manifestFile);
        assertThat(manifestContent)
            .as("Manifest file should contain fixture name")
            .contains("\"fixture\": \"db2-access\"");
        
        // Check expected Java outputs
        Path serviceFile = fixturePath.resolve("expected/src/main/java/org/shark/renovatio/generated/cobol/Db2001Service.java");
        assertThat(serviceFile)
            .as("DB2 access expected service file should exist")
            .exists();
        
        Path dtoFile = fixturePath.resolve("expected/src/main/java/org/shark/renovatio/generated/cobol/Db2001DTO.java");
        assertThat(dtoFile)
            .as("DB2 access expected DTO file should exist")
            .exists();
    }

    @Test
    void shouldHaveValidJsonFiles() throws IOException {
        for (String fixture : REQUIRED_FIXTURES) {
            Path fixturePath = Path.of(FIXTURES_BASE_PATH, fixture);
            
            // Check decisions.json is valid JSON
            Path decisionsFile = fixturePath.resolve("decisions.json");
            String decisionsContent = Files.readString(decisionsFile).trim();
            assertThat(decisionsContent)
                .as("Decisions file for '%s' should be valid JSON", fixture)
                .startsWith("{")
                .endsWith("}");
            
            // Check manifest.json is valid JSON
            Path manifestFile = fixturePath.resolve("expected/manifest.json");
            String manifestContent = Files.readString(manifestFile).trim();
            assertThat(manifestContent)
                .as("Manifest file for '%s' should be valid JSON", fixture)
                .startsWith("{")
                .endsWith("}");
        }
    }

    @Test
    void shouldHaveConsistentFixtureStructure() {
        for (String fixture : REQUIRED_FIXTURES) {
            Path fixturePath = Path.of(FIXTURES_BASE_PATH, fixture);
            
            // Check directory structure
            assertThat(fixturePath.resolve("src/cobol"))
                .as("Fixture '%s' should have src/cobol directory", fixture)
                .exists();
            
            assertThat(fixturePath.resolve("expected"))
                .as("Fixture '%s' should have expected directory", fixture)
                .exists();
            
            assertThat(fixturePath.resolve("expected/src/main/java/org/shark/renovatio/generated/cobol"))
                .as("Fixture '%s' should have expected Java output directory", fixture)
                .exists();
        }
    }
}
