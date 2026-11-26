package org.egov.architecture.inbox;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.egov", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    private static final DescribedPredicate<JavaClass> REPOSITORY_EXCLUDING_BUILDERS =
            DescribedPredicate.describe(
                    "types under ..repository.. but not query-builder packages",
                    jc -> jc.getPackageName().contains(".repository")
                            && !jc.getPackageName().contains(".builder"));

    @ArchTest
    static final ArchRule controllersDoNotAccessRepositories = noClasses()
            .that().resideInAnyPackage("..web..", "..controller..")
            .should().dependOnClassesThat(REPOSITORY_EXCLUDING_BUILDERS);
}
