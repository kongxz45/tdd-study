package com.tdd.study;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tdd.study.ContextTest.TypeBinding.ConstructorInject;
import com.tdd.study.ContextTest.TypeBinding.ConstructorInjectProvider;
import com.tdd.study.ContextTest.TypeBinding.FieldInject;
import com.tdd.study.ContextTest.TypeBinding.FieldInjectProvider;
import com.tdd.study.ContextTest.TypeBinding.MethodInject;
import com.tdd.study.ContextTest.TypeBinding.MethodInjectProvider;
import com.tdd.study.exception.CyclicDependenciesFoundException;
import com.tdd.study.exception.DependencyNotFoundException;
import com.tdd.study.exception.IllegalComponentException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Nested
public class ContextTest {

  ContextConfig config;

  @BeforeEach
  public void setUp() {
    config = new ContextConfig();
  }

  @Nested
  class TypeBinding {

    @Test
    public void should_bind_a_type_to_a_specified_instance() {
      TestComponent instance = new TestComponent() {
      };
      config.bind(TestComponent.class, instance);

      assertSame(instance, config.getContext().get(ComponentRef.of(TestComponent.class)).get());

    }

    @Test
    public void should_retrieve_empty_if_component_is_undefined() {
      Optional<TestComponent> optionalComponent = config.getContext().get(ComponentRef.of(
          TestComponent.class));
      assertTrue(optionalComponent.isEmpty());
    }


    static class ConstructorInject implements TestComponent {

      @Inject
      public ConstructorInject(Dependency dependency) {
        this.dependency = dependency;
      }

      private final Dependency dependency;

      @Override
      public Dependency getDependency() {
        return dependency;
      }
    }

    static class FieldInject implements TestComponent {

      @Inject
      Dependency dependency;

      @Override
      public Dependency getDependency() {
        return dependency;
      }
    }

    static class MethodInject implements TestComponent {

      Dependency dependency;

      @Inject
      public void setDependency(Dependency dependency) {
        this.dependency = dependency;
      }

      @Override
      public Dependency getDependency() {
        return dependency;
      }
    }

    static class ConstructorInjectProvider implements TestComponent {

      @Inject
      public ConstructorInjectProvider(Provider<Dependency> dependency) {
        this.dependency = dependency;
      }

      private final Provider<Dependency> dependency;

      @Override
      public Provider<Dependency> getDependency() {
        return dependency;
      }
    }

    static class FieldInjectProvider implements TestComponent {

      @Inject
      Provider<Dependency> dependency;

      @Override
      public Provider<Dependency> getDependency() {
        return dependency;
      }
    }

    static class MethodInjectProvider implements TestComponent {

      Provider<Dependency> dependency;

      @Inject
      public void setDependency(Provider<Dependency> dependency) {
        this.dependency = dependency;
      }

      @Override
      public Provider<Dependency> getDependency() {
        return dependency;
      }
    }

    static Stream<Arguments> componentWithDependencyClassProvider() {
      return Stream.of(Arguments.of(Named.of("Constructor Inject", ConstructorInject.class)),
          Arguments.of(Named.of("Field Inject", FieldInject.class)),
          Arguments.of(Named.of("Method Inject", MethodInject.class)),
          Arguments.of(Named.of("Constructor Inject Provider", ConstructorInjectProvider.class)),
          Arguments.of(Named.of("Field Inject Provider", FieldInjectProvider.class)),
          Arguments.of(Named.of("Method Inject Provider", MethodInjectProvider.class)));

    }

    @ParameterizedTest
    @MethodSource("componentWithDependencyClassProvider")
    public void should_bind_type_to_an_injectable_component(
        Class<? extends TestComponent> componentType) {
      Dependency dependency = new Dependency() {
      };
      config.bind(Dependency.class, dependency);
      config.bind(TestComponent.class, componentType);

      Optional<TestComponent> instance = config.getContext()
          .get(ComponentRef.of(TestComponent.class));

      assertTrue(instance.isPresent());
      Object dependencyObj = instance.get().getDependency();
      if (dependencyObj instanceof Provider<?>) {
        assertSame(dependency, ((Provider<?>) dependencyObj).get());
      } else {
        assertSame(dependency, instance.get().getDependency());
      }
    }

    @Test
    public void should_retrieve_bind_type_as_provider() {
      TestComponent instance = new TestComponent() {
      };
      config.bind(TestComponent.class, instance);

      Provider<TestComponent> provider = config.getContext()
          .get(new ComponentRef<Provider<TestComponent>>() {
          }).get();
      assertSame(instance, provider.get());

    }

    @Test
    public void should_not_retrieve_bind_type_as_unsupported_container() {
      TestComponent instance = new TestComponent() {
      };
      config.bind(TestComponent.class, instance);

      assertFalse(config.getContext().get(new ComponentRef<List<TestComponent>>() {
      }).isPresent());
    }

  }

  @Nested
  public class DependencyValidation {

    static class ComponentWithInjectConstructor implements TestComponent {

      private Dependency dependency;

      @Inject
      public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
      }

      public Dependency getDependency() {
        return dependency;
      }

    }

    // transitive_dependency also included
    static Stream<Arguments> componentMissDependencyClassProvider() {
      return Stream.of(Arguments.of(Named.of("Constructor Inject", ConstructorInject.class)),
          Arguments.of(Named.of("Field Inject", FieldInject.class)),
          Arguments.of(Named.of("Method Inject", MethodInject.class)),
          Arguments.of(Named.of("Constructor Inject Provider", ConstructorInjectProvider.class)),
          Arguments.of(Named.of("Field Inject Provider", FieldInjectProvider.class)),
          Arguments.of(Named.of("Method Inject Provider", MethodInjectProvider.class)));
    }

    @ParameterizedTest
    @MethodSource("componentMissDependencyClassProvider")
    public void should_throw_exception_if_dependency_not_found(
        Class<? extends TestComponent> componentType) {
      config.bind(TestComponent.class, componentType);

      DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
          () -> config.getContext());
      assertEquals(TestComponent.class, exception.getComponent().type());
      assertEquals(Dependency.class, exception.getDependency().type());

    }

    static class DependencyDependOnComponent implements Dependency {

      private TestComponent component;

      @Inject
      public DependencyDependOnComponent(TestComponent component) {
        this.component = component;
      }
    }

    // A->B, B->A
    @Test
    public void should_throw_exception_if_cyclic_dependencies_found() {
      config.bind(TestComponent.class, ComponentWithInjectConstructor.class);
      config.bind(Dependency.class, DependencyDependOnComponent.class);

      CyclicDependenciesFoundException exception = assertThrows(
          CyclicDependenciesFoundException.class,
          () -> config.getContext());

      List<Class<?>> components = exception.getComponents();

      assertTrue(components.contains(TestComponent.class));
      assertTrue(components.contains(Dependency.class));

    }

    static class DependencyDependOnAnotherDependency implements Dependency {

      private AnotherDependency anotherDependency;

      @Inject
      public DependencyDependOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
      }
    }

    interface AnotherDependency {

    }

    static class AnotherDependencyDependOnComponent implements AnotherDependency {

      private TestComponent component;

      @Inject
      public AnotherDependencyDependOnComponent(TestComponent component) {
        this.component = component;
      }
    }

    // A->B, B->C, C->A
    @Test
    public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
      config.bind(TestComponent.class, ComponentWithInjectConstructor.class);
      config.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
      config.bind(AnotherDependency.class, AnotherDependencyDependOnComponent.class);

      CyclicDependenciesFoundException exception = assertThrows(
          CyclicDependenciesFoundException.class,
          () -> config.getContext());

      List<Class<?>> components = exception.getComponents();
      assertTrue(components.contains(TestComponent.class));
      assertTrue(components.contains(AnotherDependency.class));
      assertTrue(components.contains(Dependency.class));

    }

    static class CyclicDependencyProviderConstructor implements Dependency {

      @Inject
      public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {
      }
    }

    @Test
    public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
      config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
      config.bind(TestComponent.class, ComponentWithInjectConstructor.class);

      assertTrue(config.getContext().get(ComponentRef.of(TestComponent.class)).isPresent());

    }
  }

  @Nested
  public class WithQualifier {

    @Test
    public void should_bind_instance_with_multi_qualifier() {
      TestComponent instance = new TestComponent() {
      };

      config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"),
          new SkywalkerLiteral());
      Context context = config.getContext();
      TestComponent chosenOne = context.get(
              ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne")))
          .get();
      TestComponent skywalker = context.get(
              ComponentRef.of(TestComponent.class, new SkywalkerLiteral()))
          .get();

      assertSame(instance, chosenOne);
      assertSame(instance, skywalker);

    }

    @Test
    public void should_bind_component_with_multi_qualifier() {
      Dependency dependency = new Dependency() {
      };
      config.bind(Dependency.class, dependency);
      config.bind(ConstructorInject.class, ConstructorInject.class, new NamedLiteral("ChosenOne"),
          new SkywalkerLiteral());

      Context context = config.getContext();
      ConstructorInject skywalker = context.get(
              ComponentRef.of(ConstructorInject.class, new SkywalkerLiteral()))
          .get();
      ConstructorInject chosenOne = context.get(
              ComponentRef.of(ConstructorInject.class, new NamedLiteral("ChosenOne")))
          .get();

      assertSame(chosenOne.getDependency(), skywalker.getDependency());
    }

    @Test
    public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
      TestComponent instance = new TestComponent() {
      };

      assertThrows(IllegalComponentException.class,
          () -> config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"),
              new TestLiteral()));
    }

    record NamedLiteral(String value) implements jakarta.inject.Named {

      @Override
      public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
      }

      @Override
      public boolean equals(Object o) {
        if (o instanceof jakarta.inject.Named named) return Objects.equals(value, named.value());
        return false;
      }

      @Override
      public int hashCode() {
        return "value".hashCode() * 127 ^ value.hashCode();
      }
    }

    @java.lang.annotation.Documented
    @java.lang.annotation.Retention(RUNTIME)
    @jakarta.inject.Qualifier
    @interface Skywalker {

    }

    record SkywalkerLiteral() implements Skywalker {

      @Override
      public Class<? extends Annotation> annotationType() {
        return Skywalker.class;
      }

      @Override
      public boolean equals(Object obj) {
        return obj instanceof Skywalker;
      }
    }


    @Test
    public void should_throw_exception_if_dependency_with_qualifier_not_found() {
      config.bind(Dependency.class, new Dependency() {
      });
      config.bind(InjectConstructorWithQualifier.class, InjectConstructorWithQualifier.class, new NamedLiteral("owner"));

      DependencyNotFoundException exception = assertThrows(
          DependencyNotFoundException.class, () -> config.getContext());

      assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());
      assertEquals(new Component(InjectConstructorWithQualifier.class, new NamedLiteral("owner")), exception.getComponent());
    }

    static class SkywalkerDependency implements Dependency {

      @Inject
      public SkywalkerDependency(@jakarta.inject.Named("ChosenOne") Dependency dependency) {
      }
    }

    static class NotCyclicDependency implements Dependency {

      @Inject
      public NotCyclicDependency(@Skywalker Dependency dependency) {
      }
    }

    // A -> @Skywalker -> @Named A (instance)
    @Test
    public void should_not_throw_cyclic_exception_if_component_with_same_type_tagged_with_different_qualifier() {
      Dependency instance = new Dependency() {
      };

      config.bind(Dependency.class, instance, new NamedLiteral("ChosenOne"));
      config.bind(Dependency.class, SkywalkerDependency.class, new SkywalkerLiteral());
      config.bind(Dependency.class, NotCyclicDependency.class);

      assertDoesNotThrow(() -> config.getContext());

    }


    static class InjectConstructorWithQualifier {

      @Inject
      public InjectConstructorWithQualifier(@Skywalker Dependency dependency) {
      }
    }

    record TestLiteral() implements Test {

      @Override
      public Class<? extends Annotation> annotationType() {
        return Test.class;
      }
    }


  }




}
