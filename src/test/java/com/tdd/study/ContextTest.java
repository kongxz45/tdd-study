package com.tdd.study;

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
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.util.List;
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
      Component instance = new Component() {
      };
      config.bind(Component.class, instance);

      assertSame(instance, config.getContext().get(Component.class).get());

    }

    @Test
    public void should_retrieve_empty_if_component_is_undefined() {
      Optional<Component> optionalComponent = config.getContext().get(Component.class);
      assertTrue(optionalComponent.isEmpty());
    }



    static class ConstructorInject implements Component {
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

    static class FieldInject implements Component {
      @Inject
      Dependency dependency;

      @Override
      public Dependency getDependency() {
        return dependency;
      }
    }

    static class MethodInject implements Component {
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

    static class ConstructorInjectProvider implements Component {
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

    static class FieldInjectProvider implements Component {
      @Inject
      Provider<Dependency> dependency;

      @Override
      public Provider<Dependency> getDependency() {
        return dependency;
      }
    }

    static class MethodInjectProvider implements Component {
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
          Arguments.of(Named.of("Method Inject", MethodInject.class)));
//          Arguments.of(Named.of("Constructor Inject Provider", ConstructorInjectProvider.class)),
//          Arguments.of(Named.of("Field Inject Provider", FieldInjectProvider.class)),
//          Arguments.of(Named.of("Method Inject Provider", MethodInjectProvider.class)));
    }
    @ParameterizedTest
    @MethodSource("componentWithDependencyClassProvider")
    public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
      Dependency dependency = new Dependency() {
      };
      config.bind(Dependency.class, dependency);
      config.bind(Component.class, componentType);

      Optional<Component> instance = config.getContext().get(Component.class);

      assertSame(dependency, instance.get().getDependency());
    }

    @Test
    public void should_retrieve_bind_type_as_provider() {
      Component instance = new Component() {};
      config.bind(Component.class, instance);

      ParameterizedType type = new TypeLiteral<Provider<Component>>() {}.getType();

      Provider<Component> provider = (Provider<Component>) config.getContext().get(type)
          .get();
      assertSame(instance, provider.get());

    }

    @Test
    public void should_not_retrieve_bind_type_as_unsupported_container() {
      Component instance = new Component() {};
      config.bind(Component.class, instance);

      ParameterizedType type = new TypeLiteral<List<Component>>() {}.getType();

      assertFalse(config.getContext().get(type).isPresent());
    }

    // java范型的实现方式
    static abstract class TypeLiteral<T> {
      public ParameterizedType getType() {
        return (ParameterizedType) ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
      }
    }
  }

  @Nested
  public class DependencyValidation {

     static class ComponentWithInjectConstructor implements Component {

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
    public void should_throw_exception_if_dependency_not_found(Class<? extends Component> componentType) {
      config.bind(Component.class, componentType);

      DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
          () -> config.getContext());
      assertEquals(Component.class, exception.getComponent());
      assertEquals(Dependency.class, exception.getDependency());

    }

    static class DependencyDependOnComponent implements Dependency {

      private Component component;

      @Inject
      public DependencyDependOnComponent(Component component) {
        this.component = component;
      }
    }

    // A->B, B->A
    @Test
    public void should_throw_exception_if_cyclic_dependencies_found() {
      config.bind(Component.class, ComponentWithInjectConstructor.class);
      config.bind(Dependency.class, DependencyDependOnComponent.class);

      CyclicDependenciesFoundException exception = assertThrows(
          CyclicDependenciesFoundException.class,
          () -> config.getContext());

      List<Class<?>> components = exception.getComponents();

      assertTrue(components.contains(Component.class));
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

      private Component component;

      @Inject
      public AnotherDependencyDependOnComponent(Component component) {
        this.component = component;
      }
    }

    // A->B, B->C, C->A
    @Test
    public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
      config.bind(Component.class, ComponentWithInjectConstructor.class);
      config.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
      config.bind(AnotherDependency.class, AnotherDependencyDependOnComponent.class);

      CyclicDependenciesFoundException exception = assertThrows(
          CyclicDependenciesFoundException.class,
          () -> config.getContext());

      List<Class<?>> components = exception.getComponents();
      assertTrue(components.contains(Component.class));
      assertTrue(components.contains(AnotherDependency.class));
      assertTrue(components.contains(Dependency.class));

    }

    static class CyclicDependencyProviderConstructor implements Dependency {

       @Inject
      public CyclicDependencyProviderConstructor(Provider<Component> component) {
      }
    }

    @Test
    public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
       config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
       config.bind(Component.class, ComponentWithInjectConstructor.class);

       assertTrue(config.getContext().get(Component.class).isPresent());


    }
  }

}
