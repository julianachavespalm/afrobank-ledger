package tech.afrobank.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("tech.afrobank");
    }

    @Test
    @DisplayName("Domain layer must not depend on Spring Framework or Jakarta Persistence")
    void domainMustNotDependOnFrameworks() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.servlet.."
                );

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain layer must not depend on Outer Layers (application, adapters, infrastructure)")
    void domainMustNotDependOnOuterLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..application..",
                        "..adapter..",
                        "..controller..",
                        "..outbox..",
                        "..repository.."
                );

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Application layer must not depend on Adapters or Infrastructure")
    void applicationMustNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapter..",
                        "..controller.."
                );

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Value objects must be immutable and implement standard record or final types")
    void valueObjectsShouldBeRecordsOrFinal() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain.vo..")
                .should().beRecords()
                .orShould().haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL);

        rule.check(importedClasses);
    }
}
