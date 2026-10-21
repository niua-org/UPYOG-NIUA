package org.upyog.chb.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.upyog.chb", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule controllersDoNotAccessRepositoriesDirectly = noClasses()
            .that().resideInAPackage("..web.controllers..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");
}
