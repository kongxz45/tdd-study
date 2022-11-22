package com.tdd.study;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tdd.study.exception.CyclicDependenciesFoundException;
import com.tdd.study.exception.DependencyNotFoundException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    @Test
    public void should_throw_exception_if_dependency_not_found() {
      config.bind(Component.class, ComponentWithInjectConstructor.class);

      DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
          () -> config.getContext());
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
  }

}
