package org.upyog.dashboard.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Phase 3 pilot: lightweight layering checks for dashboard-data-engine (library module).
 */
class LayeringArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.upyog.dashboard");

    @Test
    void transformersDoNotDependOnApiLayer() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("org.upyog.dashboard.transformer..")
                .should().dependOnClassesThat().resideInAnyPackage("org.upyog.dashboard.api..");
        rule.check(CLASSES);
    }

    @Test
    void validatorsDoNotDependOnApiLayer() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("org.upyog.dashboard.validator..")
                .should().dependOnClassesThat().resideInAnyPackage("org.upyog.dashboard.api..");
        rule.check(CLASSES);
    }

    @Test
    void utilDoesNotDependOnApiOrServiceLayers() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("org.upyog.dashboard.util..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.upyog.dashboard.api..", "org.upyog.dashboard.service..");
        rule.check(CLASSES);
    }
}
