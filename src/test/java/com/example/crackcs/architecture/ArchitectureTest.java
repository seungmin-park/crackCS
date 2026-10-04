package com.example.crackcs.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {
    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.crackcs");
    private static final JavaClasses TEST_CLASSES = new ClassFileImporter()
            .importPath("build/classes/java/test");

    @Test
    @DisplayName("Controller는 저장소와 영속성 API에 직접 의존하지 않는다")
    void controllersDoNotDependOnPersistence() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..repository..", "org.springframework.data.repository..",
                        "org.springframework.data.jpa.repository..", "jakarta.persistence..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("도메인은 HTTP와 유스케이스 및 외부 구현에 의존하지 않는다")
    void domainsDoNotDependOnApplicationBoundaries() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..controller..", "..service..", "..repository..", "..adapter..",
                        "org.springframework.web..", "org.springframework.data..", "java.net.http..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("Service는 Controller의 HTTP 계약에 의존하지 않는다")
    void servicesDoNotDependOnControllers() {
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("엔티티는 공개 setter로 도메인 규칙을 우회하지 않는다")
    void entitiesDoNotExposePublicSetters() {
        methods().that().arePublic().and().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                .should().haveNameNotMatching("set[A-Z].*")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("Service 이름의 유스케이스 계약은 인터페이스로 제공한다")
    void serviceContractsAreInterfaces() {
        classes().that().haveSimpleNameEndingWith("Service")
                .and().haveSimpleNameNotStartingWith("Default")
                .should().beInterfaces().check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("Service 통합 테스트는 테스트 트랜잭션과 Service mock을 사용하지 않는다")
    void serviceIntegrationTestsUseRealTransactionsAndCollaborators() {
        noClasses().that().haveSimpleNameEndingWith("ServiceTest")
                .should().beAnnotatedWith(Transactional.class).check(TEST_CLASSES);
        noMethods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("ServiceTest")
                .should().beAnnotatedWith(Transactional.class).check(TEST_CLASSES);
        noFields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("ServiceTest")
                .should().beAnnotatedWith(MockitoBean.class).check(TEST_CLASSES);
        noFields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("ServiceTest")
                .should().beAnnotatedWith("org.mockito.Mock").check(TEST_CLASSES);
        noFields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("ServiceTest")
                .should().beAnnotatedWith("org.mockito.InjectMocks").check(TEST_CLASSES);
    }

    @Test
    @DisplayName("Service 유스케이스 통합 테스트는 실제 Spring 애플리케이션을 실행한다")
    void serviceIntegrationTestsLoadApplication() {
        classes().that().haveSimpleNameEndingWith("ServiceTest")
                .should().beAnnotatedWith(SpringBootTest.class).check(TEST_CLASSES);
    }

    @Test
    @DisplayName("테스트 메서드에는 한글 설명을 붙이고 클래스 설명은 두지 않는다")
    void testMethodsHaveKoreanDisplayNames() {
        List<Class<?>> testTypes = TEST_CLASSES.stream().<Class<?>>map(testClass -> testClass.reflect()).toList();
        assertThat(testTypes).isNotEmpty();
        for (Class<?> testType : testTypes) {
            assertThat(testType.isAnnotationPresent(DisplayName.class)).as(testType.getName()).isFalse();
            List<Method> testMethods = List.of(testType.getDeclaredMethods()).stream()
                    .filter(method -> method.isAnnotationPresent(Test.class)
                            || method.isAnnotationPresent(ParameterizedTest.class)).toList();
            for (Method testMethod : testMethods) {
                DisplayName displayName = testMethod.getAnnotation(DisplayName.class);
                assertThat(displayName).as(testType.getName() + "." + testMethod.getName()).isNotNull();
                assertThat(displayName.value()).containsPattern("[가-힣]");
            }
        }
    }
}
