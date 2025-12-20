package org.dreamabout.sw.frp.be.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.dreamabout.sw.frp.be", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    // --- DTO Rules ---

    @ArchTest
    static final ArchRule dtos_must_be_records = classes()
            .that().haveSimpleNameEndingWith("Dto")
            .should().beRecords()
            .because("DTOs must be immutable records");

    @ArchTest
    static final ArchRule dtos_must_reside_in_dto_package = classes()
            .that().haveSimpleNameEndingWith("Dto")
            .should().resideInAPackage("..model.dto..")
            .because("DTOs must be located in model.dto package");

    @ArchTest
    static final ArchRule dtos_must_not_be_inner_classes = classes()
            .that().haveSimpleNameEndingWith("Dto")
            .should().beTopLevelClasses()
            .because("DTOs must not be inner classes");

    // --- Controller Rules ---

    @ArchTest
    static final ArchRule controllers_must_not_be_inner_classes = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().beTopLevelClasses()
            .because("Controllers must not be inner classes");

    @ArchTest
    static final ArchRule controllers_must_not_access_security_context_directly = noClasses()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().accessClassesThat().haveFullyQualifiedName("org.springframework.security.core.context.SecurityContextHolder")
            .because("Controllers should not access SecurityContextHolder directly (use a service or wrapper)");

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories = noClasses()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("Controllers must not access repositories directly");

    // --- Layering Rules ---

    @ArchTest
    static final ArchRule no_repository_access_from_controller_layer = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("Controllers must not access repositories directly (redundant but explicit for layering)");

    @ArchTest
    static final ArchRule no_service_access_from_repository_layer = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAPackage("..service..")
            .because("Repositories must not depend on services");

    @ArchTest
    static final ArchRule no_controller_access_from_service_layer = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("Services must not depend on controllers");

    // --- Service Rules ---

    @ArchTest
    static final ArchRule only_security_context_service_can_access_security_context = classes()
            .that().doNotHaveFullyQualifiedName("org.dreamabout.sw.frp.be.config.security.SecurityContextService")
            .should().onlyAccessClassesThat()
            .doNotHaveFullyQualifiedName("org.springframework.security.core.context.SecurityContextHolder");
}
