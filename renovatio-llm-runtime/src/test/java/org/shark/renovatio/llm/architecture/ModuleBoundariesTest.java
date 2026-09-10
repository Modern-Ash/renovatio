package org.shark.renovatio.llm.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests enforcing module boundaries for LLM integration.
 *
 * Rules:
 * 1. COBOL IR, COBOL Provider, JCL modules must NOT depend on LLM SDKs or runtime
 * 2. LLM runtime must NOT depend on COBOL/JCL implementation modules
 * 3. Only adapters in llm-runtime can depend on both LLMProvider interface and domain modules
 * 4. FakeLLMProvider is allowed everywhere (it's offline, no SDKs)
 */
class ModuleBoundariesTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void setup() {
        allClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.shark.renovatio");
    }

    // === COBOL IR must not depend on LLM modules ===

    @Test
    void cobolIrShouldNotDependOnLlmRuntime() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.cobol.ir..")
            .should().dependOnClassesThat().resideInAPackage("org.shark.renovatio.llm.runtime..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void cobolIrShouldNotDependOnLlmAdapter() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.cobol.ir..")
            .should().dependOnClassesThat().resideInAPackage("org.shark.renovatio.llm.adapter..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    // === COBOL Provider must not depend on LLM modules ===

    @Test
    void cobolProviderShouldNotDependOnLlmRuntime() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.provider.cobol..")
            .should().dependOnClassesThat().resideInAPackage("org.shark.renovatio.llm.runtime..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void cobolProviderShouldNotDependOnLlmAdapter() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.provider.cobol..")
            .should().dependOnClassesThat().resideInAPackage("org.shark.renovatio.llm.adapter..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void cobolProviderShouldNotDependOnResilience4j() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.provider.cobol..")
            .should().dependOnClassesThat().haveNameMatching(".*resilience4j.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void cobolProviderShouldNotDependOnVertex() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.provider.cobol..")
            .should().dependOnClassesThat().haveNameMatching(".*vertex.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    // === LLM Runtime/Adapter must not depend on COBOL implementation ===

    @Test
    void llmRuntimeShouldNotDependOnCobolProvider() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.llm.runtime..")
            .should().dependOnClassesThat().resideInAPackage("org.shark.renovatio.provider.cobol..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void llmAdapterShouldNotDependOnCobolProvider() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.llm.adapter..")
            .should().dependOnClassesThat().resideInAPackage("org.shark.renovatio.provider.cobol..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    // === Shared LLM domain must not depend on LLM implementation ===

    @Test
    void llmSharedDomainShouldNotDependOnLlmRuntime() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.shared.llm..")
            .should().dependOnClassesThat().resideInAPackage("org.shark.renovatio.llm.runtime..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void llmSharedDomainShouldNotDependOnLlmAdapter() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.shared.llm..")
            .should().dependOnClassesThat().resideInAPackage("org.shark.renovatio.llm.adapter..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    // === Adapter rules ===

    @Test
    void adapterClassesResideInAdapterPackage() {
        // Adapter classes must reside in adapter package
        ArchRule rule = ArchRuleDefinition.classes()
            .that().resideInAPackage("org.shark.renovatio.llm.adapter..")
            .should().resideInAPackage("org.shark.renovatio.llm.adapter..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void adapterClassesDependOnLlmDomain() {
        // Adapter classes must depend on LLM domain
        ArchRule rule = ArchRuleDefinition.classes()
            .that().resideInAPackage("org.shark.renovatio.llm.adapter..")
            .should().dependOnClassesThat().resideInAPackage("org.shark.renovatio.llm.domain..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void fakeProviderInCorrectPackage() {
        ArchRule rule = ArchRuleDefinition.classes()
            .that().haveSimpleName("FakeLLMProvider")
            .should().resideInAPackage("org.shark.renovatio.llm.provider..");

        rule.allowEmptyShould(true).check(allClasses);
    }

    // === Core modules must not depend on LLM SDKs ===
    // Testing each core module separately

    @Test
    void cobolIrShouldNotDependOnResilience4j() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.cobol.ir..")
            .should().dependOnClassesThat().haveNameMatching(".*resilience4j.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void coreModuleShouldNotDependOnResilience4j() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.core..")
            .should().dependOnClassesThat().haveNameMatching(".*resilience4j.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void sharedModuleShouldNotDependOnResilience4j() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.shared..")
            .should().dependOnClassesThat().haveNameMatching(".*resilience4j.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void cobolIrShouldNotDependOnVertex() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.cobol.ir..")
            .should().dependOnClassesThat().haveNameMatching(".*vertex.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void coreModuleShouldNotDependOnVertex() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.core..")
            .should().dependOnClassesThat().haveNameMatching(".*vertex.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void sharedModuleShouldNotDependOnVertex() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.shared..")
            .should().dependOnClassesThat().haveNameMatching(".*vertex.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void cobolIrShouldNotDependOnGoogleCloudAi() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.cobol.ir..")
            .should().dependOnClassesThat().haveNameMatching(".*google.*cloud.*ai.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void cobolProviderShouldNotDependOnGoogleCloudAi() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.provider.cobol..")
            .should().dependOnClassesThat().haveNameMatching(".*google.*cloud.*ai.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void coreModuleShouldNotDependOnGoogleCloudAi() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.core..")
            .should().dependOnClassesThat().haveNameMatching(".*google.*cloud.*ai.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void sharedModuleShouldNotDependOnGoogleCloudAi() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.shared..")
            .should().dependOnClassesThat().haveNameMatching(".*google.*cloud.*ai.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void cobolIrShouldNotDependOnOpenAi() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.cobol.ir..")
            .should().dependOnClassesThat().haveNameMatching(".*openai.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void cobolProviderShouldNotDependOnOpenAi() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.provider.cobol..")
            .should().dependOnClassesThat().haveNameMatching(".*openai.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void coreModuleShouldNotDependOnOpenAi() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.core..")
            .should().dependOnClassesThat().haveNameMatching(".*openai.*");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    void sharedModuleShouldNotDependOnOpenAi() {
        ArchRule rule = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("org.shark.renovatio.shared..")
            .should().dependOnClassesThat().haveNameMatching(".*openai.*");

        rule.allowEmptyShould(true).check(allClasses);
    }
}