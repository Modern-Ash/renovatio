package org.modernash.renovatio.persistence.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PersistenceArchitectureTest {
    @Test
    void persistenceHasNoForbiddenDependencies() {
        var classes = new ClassFileImporter().importPackages("org.modernash.renovatio.persistence");
        noClasses().that().resideInAPackage("org.modernash.renovatio.persistence..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.modernash.renovatio.cobol..",
                        "org.modernash.renovatio.provider..",
                        "org.openrewrite..",
                        "com.squareup.javapoet..",
                        "freemarker..",
                        "com.github.mustachejava..",
                        "org.springframework.web..",
                        "jakarta.persistence..",
                        "javax.lang.model..",
                        "com.fasterxml.jackson..")
                .check(classes);
    }
}
