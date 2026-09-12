package com.luminary.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ModuleBoundaryTest {

    private static final List<String> DOMAIN_MODULES = List.of(
            "access", "institution", "student", "curriculum", "practice",
            "tasks", "exams", "content", "analytics", "recommendation",
            "entitlement", "platform");

    private static final JavaClasses CLASSES =
            new ClassFileImporter().importPackages("com.luminary");

    @Test
    void domain_modules_must_not_import_each_others_internal_layers() {
        for (String module : DOMAIN_MODULES) {
            List<String> otherInternalLayers = DOMAIN_MODULES.stream()
                    .filter(other -> !other.equals(module))
                    .flatMap(other -> List.of(
                            "com.luminary." + other + ".domain..",
                            "com.luminary." + other + ".infrastructure..").stream())
                    .toList();

            ArchRule rule = noClasses()
                    .that().resideInAPackage("com.luminary." + module + "..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(otherInternalLayers.toArray(String[]::new))
                    .because("module " + module
                            + " must only use API DTOs, application ports or " +
                            "versioned events of other modules")
                    .allowEmptyShould(true);

            rule.check(CLASSES);
        }
    }

    @Test
    void modules_must_be_free_of_cycles() {
        ArchRule rule = slices().matching("com.luminary.(*)..")
                .should().beFreeOfCycles()
                .because("domain modules communicate through events and " +
                        "read-only ports, not circular service calls");

        rule.check(CLASSES);
    }

    @Test
    void shared_package_must_not_depend_on_any_domain_module() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.luminary.shared..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "javax..", "jakarta..",
                        "org.springframework..", "org.slf4j..",
                        "com.fasterxml.jackson..",
                        "com.luminary.shared..")
                .because("shared contains only identity types, time, errors " +
                        "and the event envelope; it may use Spring platform " +
                        "types but never a domain module")
                .allowEmptyShould(true);

        rule.check(CLASSES);
    }
}