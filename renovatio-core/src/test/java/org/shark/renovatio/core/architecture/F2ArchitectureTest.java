package org.shark.renovatio.core.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class F2ArchitectureTest {
    @Test
    void semanticIrHasNoSourceTargetOrFrameworkDependencies() {
        var classes = new ClassFileImporter().importPackages("org.shark.renovatio.semantic.ir");
        noClasses().that().resideInAPackage("org.shark.renovatio.semantic.ir..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.shark.renovatio.cobol..", "org.shark.renovatio.provider..",
                        "org.openrewrite..", "com.squareup.javapoet..", "freemarker..",
                        "com.github.mustachejava..", "org.springframework..", "jakarta.persistence..",
                        "javax.lang.model..", "com.fasterxml.jackson..")
                .check(classes);
    }

    @Test
    void coreHasNoConcreteProviderDependency() {
        var classes = new ClassFileImporter().importPackages("org.shark.renovatio.core");
        noClasses().that().resideInAPackage("org.shark.renovatio.core..")
                .should().dependOnClassesThat().resideInAnyPackage("org.shark.renovatio.provider..")
                .check(classes);
    }
}
