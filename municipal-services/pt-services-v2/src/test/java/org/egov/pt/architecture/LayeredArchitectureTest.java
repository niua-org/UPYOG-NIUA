package org.egov.pt.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.egov.pt", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    private static final DescribedPredicate<JavaClass> EXCLUDING_LEGACY_CONTROLLER =
            DescribedPredicate.describe(
                    "excluding DemandBasedController (direct repository access)",
                    jc -> !"DemandBasedController".equals(jc.getSimpleName()));

    @ArchTest
    static final ArchRule controllersDoNotAccessRepositories = noClasses()
            .that().resideInAnyPackage("..web..", "..controller..")
            .and(EXCLUDING_LEGACY_CONTROLLER)
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");
}
