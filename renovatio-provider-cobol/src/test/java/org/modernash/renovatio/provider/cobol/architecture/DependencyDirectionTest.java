package org.modernash.renovatio.provider.cobol.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests to verify dependency direction between COBOL and Java providers.
 * These tests ensure that renovatio-provider-cobol does not have inappropriate dependencies
 * on renovatio-provider-java.
 */
class DependencyDirectionTest {

    private static final String COBOL_PROVIDER_PACKAGE = "org.modernash.renovatio.provider.cobol..";
    private static final String JAVA_PROVIDER_PACKAGE = "org.modernash.renovatio.provider.java..";

    @Test
    void cobolProviderShouldNotDependOnJavaProvider() {
        var importedClasses = new ClassFileImporter()
                .importPackages(COBOL_PROVIDER_PACKAGE);

        ArchRule rule = noClasses()
                .that().resideInAPackage(COBOL_PROVIDER_PACKAGE)
                .should().dependOnClassesThat()
                .resideInAPackage(JAVA_PROVIDER_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    void cobolProviderShouldOnlyDependOnSharedAndCobolIr() {
        var importedClasses = new ClassFileImporter()
                .importPackages("org.modernash.renovatio");

        noClasses().that().resideInAPackage(COBOL_PROVIDER_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.modernash.renovatio.core..", "org.modernash.renovatio.web..")
                .check(importedClasses);
    }
}
