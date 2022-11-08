package com.tdd.study;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ContainerTest {


  @Nested
  public class ComponentConstruction {

    ContextConfig config;

    @BeforeEach
    public void setUp() {
      config = new ContextConfig();
    }

    @Test
    public void should_bind_a_type_to_a_specified_instance() {

      Component instance = new Component() {
      };
      config.bind(Component.class, instance);

      assertSame(instance, config.getContext().get(Component.class).get());

    }

    @Test
    public void should_return_empty_if_component_is_undefined() {
      Optional<Component> optionalComponent = config.getContext().get(Component.class);
      assertTrue(optionalComponent.isEmpty());
    }

    @Nested
    public class DependencyValidation {

      // transitive_dependency also included
      @Test
      public void should_throw_exception_if_dependency_not_found() {
        config.bind(Component.class, ComponentWithInjectConstructor.class);

        DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
            () -> config.getContext());
        assertEquals(Dependency.class, exception.getDependency());
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

  @Nested
  public class DependenciesSelection {


  }

  @Nested
  public class LifeCycleManagement {


  }

}

interface Component {

}

interface Dependency {

}


class ComponentWithDefaultConstructor implements Component {

  public ComponentWithDefaultConstructor() {
  }
}

class ComponentWithInjectConstructor implements Component {

  private Dependency dependency;

  @Inject
  public ComponentWithInjectConstructor(Dependency dependency) {
    this.dependency = dependency;
  }

  public Dependency getDependency() {
    return dependency;
  }

}

class DependencyWithInjectConstructor implements Dependency {

  private String dependency;

  @Inject
  public DependencyWithInjectConstructor(String dependency) {
    this.dependency = dependency;
  }

  public String getDependency() {
    return dependency;
  }
}

class ComponentWithMultiInjectConstructors implements Component {

  @Inject
  public ComponentWithMultiInjectConstructors(String name, Integer age) {
  }

  @Inject
  public ComponentWithMultiInjectConstructors(String name) {
  }
}

class ComponentWithNoInjectNorDefaultConstructor implements Component {

  public ComponentWithNoInjectNorDefaultConstructor(String name) {
  }
}

class DependencyDependOnComponent implements Dependency {

  private Component component;

  @Inject
  public DependencyDependOnComponent(Component component) {
    this.component = component;
  }
}

class DependencyDependOnAnotherDependency implements Dependency {

  private AnotherDependency anotherDependency;

  @Inject
  public DependencyDependOnAnotherDependency(AnotherDependency anotherDependency) {
    this.anotherDependency = anotherDependency;
  }
}

interface AnotherDependency {

}

class AnotherDependencyDependOnDependency implements AnotherDependency {

  private Dependency dependency;

  @Inject
  public AnotherDependencyDependOnDependency(Dependency dependency) {
    this.dependency = dependency;
  }
}

class AnotherDependencyDependOnComponent implements AnotherDependency {

  private Component component;

  @Inject
  public AnotherDependencyDependOnComponent(Component component) {
    this.component = component;
  }
}


