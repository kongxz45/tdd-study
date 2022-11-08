package com.tdd.study;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

  ContextConfig config;

  @BeforeEach
  public void setUp() {
    config = new ContextConfig();
  }

  @Nested
  public class ComponentConstruction {

    @Nested
    public class ConstructorInjection {

      @Test
      public void should_bind_a_type_to_a_specified_instance() {

        Component instance = new Component() {
        };
        config.bind(Component.class, instance);

        assertSame(instance, config.getContext().get(Component.class).get());

      }

      @Nested
      public class ConstructionInjection {

        public static final String INDIRECT_DEPENDENCY = "indirect dependency";

        // default constructor
        @Test
        public void should_bind_a_type_to_a_class_with_default_constructor() {
          ContextConfig contextConfig = new ContextConfig();
          contextConfig.bind(Component.class, ComponentWithDefaultConstructor.class);

          Component instance = contextConfig.getContext().get(Component.class).get();

          assertNotNull(instance);
          assertTrue(instance instanceof ComponentWithDefaultConstructor);

        }

        @Test
        public void should_bind_type_to_a_class_with_inject_constructor() {
          Dependency dependency = new Dependency() {
          };

          config.bind(Component.class, ComponentWithInjectConstructor.class);
          config.bind(Dependency.class, dependency);

          Component instance = config.getContext().get(Component.class).get();
          assertNotNull(instance);
          assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
        }

        @Test
        public void should_bind_type_to_a_class_with_transitive_dependency() {
          config.bind(Component.class, ComponentWithInjectConstructor.class);
          config.bind(Dependency.class, DependencyWithInjectConstructor.class);
          config.bind(String.class, INDIRECT_DEPENDENCY);

          Component instance = config.getContext().get(Component.class).get();
          assertNotNull(instance);

          Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
          assertNotNull(dependency);

          assertEquals(INDIRECT_DEPENDENCY,
              ((DependencyWithInjectConstructor) dependency).getDependency());

        }

        @Test
        public void should_throw_exception_if_multi_inject_constructors_found() {
          assertThrows(IllegalComponentException.class,
              () -> config.bind(Component.class, ComponentWithMultiInjectConstructors.class));
        }

        @Test
        public void should_throw_exception_if_no_inject_constructor_nor_default_method_found() {
          assertThrows(IllegalComponentException.class, () ->
              config.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class));

        }

        @Test
        public void should_throw_exception_if_dependency_not_found() {
          config.bind(Component.class, ComponentWithInjectConstructor.class);

          DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
              () -> config.getContext());
          assertEquals(Dependency.class, exception.getDependency());
        }

        @Test
        public void should_throw_exception_if_transitive_dependency_not_found() {

          config.bind(AnotherDependency.class, AnotherDependencyDependOnDependency.class);
          config.bind(Dependency.class, DependencyDependOnComponent.class);

          DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
              () -> config.getContext());
          assertEquals(Component.class, exception.getDependency());
          assertEquals(Dependency.class, exception.getComponent());
        }

        @Test
        public void should_return_empty_if_component_is_undefined() {
          Optional<Component> optionalComponent = config.getContext().get(Component.class);
          assertTrue(optionalComponent.isEmpty());
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

      //TODO abstract class
      //TODO interface
    }

    @Nested
    public class FieldInjection {

      static class ComponentWithFieldInjection {

        @Inject
        Dependency dependency;
      }

      static class SubClassWithFieldInjection extends ComponentWithFieldInjection {

      }

      @Test
      public void should_inject_dependency_via_field() {
        Dependency dependency = new Dependency() {
        };

        config.bind(Dependency.class, dependency);
        config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

        ComponentWithFieldInjection component = config.getContext()
            .get(ComponentWithFieldInjection.class).get();

        assertSame(dependency, component.dependency);
      }

      @Test
      public void should_include_field_dependency_in_dependencies() {
        ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(
            ComponentWithFieldInjection.class);
        assertArrayEquals(new Class<?>[]{Dependency.class},
            provider.getDependencies().toArray(Class<?>[]::new));
      }

      @Test
      public void should_inject_dependency_via_superclass_inject_field() {
        Dependency dependency = new Dependency() {
        };

        config.bind(Dependency.class, dependency);
        config.bind(SubClassWithFieldInjection.class, SubClassWithFieldInjection.class);

        SubClassWithFieldInjection component = config.getContext()
            .get(SubClassWithFieldInjection.class).get();

        assertSame(dependency, component.dependency);

      }

    }

    @Nested
    public class MethodInjection {

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


