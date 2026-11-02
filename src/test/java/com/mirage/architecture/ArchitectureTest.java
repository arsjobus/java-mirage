package com.mirage.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final String ROOT = "com.mirage";
    private static final String CORE = ROOT + ".core..";
    private static final String[] TOOLKIT = {"javafx.."};
    private static final String[] IMPLEMENTATION_PACKAGES = {
        ROOT + ".commands..",
        ROOT + ".icon..",
        ROOT + ".io..",
        ROOT + ".mcp..",
        ROOT + ".rendering..",
        ROOT + ".tools..",
        ROOT + ".ui.."
    };

    private static JavaClasses classes;

    @BeforeAll
    static void importProductionClasses() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT);
        assertTrue(classes.stream().anyMatch(c -> c.getName().equals(ROOT + ".App")),
                "production classes were not found; run the tests through Maven");
    }

    private static final DescribedPredicate<JavaClass> BOOTSTRAP =
            new DescribedPredicate<>("App or Launcher") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return javaClass.getName().equals(ROOT + ".App")
                            || javaClass.getName().equals(ROOT + ".Launcher");
                }
            };

    private static final DescribedPredicate<JavaClass> JAVAFX_USERS =
            new DescribedPredicate<>("UI, renderer, MCP adapter, or bootstrap") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return javaClass.getPackageName().equals(ROOT + ".ui")
                            || javaClass.getPackageName().startsWith(ROOT + ".ui.")
                            || javaClass.getPackageName().equals(ROOT + ".rendering")
                            || javaClass.getPackageName().startsWith(ROOT + ".rendering.")
                            || javaClass.getName().equals(ROOT + ".mcp.MirageMcpServer")
                            || BOOTSTRAP.test(javaClass);
                }
            };

    @Test
    void coreDoesNotDependOnApplicationPackagesOrJavaFx() {
        noClasses().that().resideInAnyPackage(CORE)
                .should().dependOnClassesThat().resideInAnyPackage(IMPLEMENTATION_PACKAGES)
                .because("the image and document model must remain independent of application layers")
                .check(classes);
        noClasses().that().resideInAnyPackage(CORE)
                .should().dependOnClassesThat().resideInAnyPackage(TOOLKIT)
                .because("the core model must remain usable without JavaFX")
                .check(classes);
    }

    @Test
    void onlyUiRenderingMcpAdapterAndBootstrapUseJavaFx() {
        noClasses().that(DescribedPredicate.not(JAVAFX_USERS))
                .should().dependOnClassesThat().resideInAnyPackage(TOOLKIT)
                .because("JavaFX belongs to the UI, renderer, and MCP thread adapter")
                .check(classes);
    }

    @Test
    void commandsDoNotDependOnOuterLayers() {
        noClasses().that().resideInAPackage(ROOT + ".commands..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".icon..", ROOT + ".io..", ROOT + ".mcp..",
                        ROOT + ".rendering..", ROOT + ".tools..", ROOT + ".ui..")
                .check(classes);
    }

    @Test
    void toolsDoNotDependOnPresentationOrInfrastructure() {
        noClasses().that().resideInAPackage(ROOT + ".tools..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".icon..", ROOT + ".io..", ROOT + ".mcp..",
                        ROOT + ".rendering..", ROOT + ".ui..")
                .check(classes);
    }

    @Test
    void renderingDoesNotDependOnEditingOrCoordination() {
        noClasses().that().resideInAPackage(ROOT + ".rendering..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".commands..", ROOT + ".icon..", ROOT + ".io..",
                        ROOT + ".mcp..", ROOT + ".tools..", ROOT + ".ui..")
                .check(classes);
    }

    @Test
    void imageIoDoesNotDependOnEditingOrPresentation() {
        noClasses().that().resideInAPackage(ROOT + ".io..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".commands..", ROOT + ".icon..", ROOT + ".mcp..",
                        ROOT + ".rendering..", ROOT + ".tools..", ROOT + ".ui..")
                .check(classes);
    }

    @Test
    void nothingDependsOnTheBootstrap() {
        noClasses().that(DescribedPredicate.not(BOOTSTRAP))
                .should().dependOnClassesThat(BOOTSTRAP)
                .because("App and Launcher are entry points, not shared services")
                .check(classes);
    }
}
