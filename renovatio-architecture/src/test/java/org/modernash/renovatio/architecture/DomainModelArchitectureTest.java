package org.modernash.renovatio.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainModelArchitectureTest {
    @Test
    void domainModelHasNoSpringOrFilesystemDependencies() {
        var classes = new ClassFileImporter().importPackages("org.modernash.renovatio.domain.model");
        noClasses().that().resideInAPackage("org.modernash.renovatio.domain.model..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "com.fasterxml.jackson..",
                        "java.io..",
                        "java.nio.file..")
                .check(classes);
    }

    @Test
    void architectureModelHasNoSpringDependencies() {
        var classes = new ClassFileImporter().importPackages("org.modernash.renovatio.architecture");
        noClasses().that().resideInAPackage("org.modernash.renovatio.architecture..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..")
                .check(classes);
    }

    @Test
    void artifactManifestHasNoSpringDependencies() {
        var classes = new ClassFileImporter().importPackages("org.modernash.renovatio.architecture");
        noClasses().that().resideInAPackage("org.modernash.renovatio.architecture..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..")
                .check(classes);
    }
}
