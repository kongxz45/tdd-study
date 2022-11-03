package com.tdd.study;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ContainerTest {

  Context context;

  @BeforeEach
  public void setUp() {
    context = new Context();
  }

  @Nested
  public class ComponentConstruction {

    @Test
    public void should_bind_a_type_to_a_specified_instance() {

      Component instance = new Component() {
      };
      context.bind(Component.class, instance);

      assertSame(instance, context.get(Component.class).get());

    }

    @Nested
    public class ConstructionInjection {

      public static final String INDIRECT_DEPENDENCY = "indirect dependency";

      // default constructor
      @Test
      public void should_bind_a_type_to_a_class_with_default_constructor() {
        Context context = new Context();
        context.bind(Component.class, ComponentWithDefaultConstructor.class);

        Component instance = context.get(Component.class)
            .orElseThrow(() -> new DependencyNotFoundException());

        assertNotNull(instance);
        assertTrue(instance instanceof ComponentWithDefaultConstructor);

      }

      @Test
      public void should_bind_type_to_a_class_with_inject_constructor() {
        Dependency dependency = new Dependency() {};

        context.bind(Component.class, ComponentWithInjectConstructor.class);
        context.bind(Dependency.class, dependency);

        Component instance = context.get(Component.class)
            .orElseThrow(() -> new DependencyNotFoundException());
        assertNotNull(instance);
        assertSame(dependency, ((ComponentWithInjectConstructor)instance).getDependency());
      }

      @Test
      public void should_bind_type_to_a_class_with_transitive_dependency() {
        context.bind(Component.class, ComponentWithInjectConstructor.class);
        context.bind(Dependency.class, DependencyWithInjectConstructor.class);
        context.bind(String.class, INDIRECT_DEPENDENCY);

        Component instance = context.get(Component.class).get();
        assertNotNull(instance);

        Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
        assertNotNull(dependency);

        assertEquals(INDIRECT_DEPENDENCY, ((DependencyWithInjectConstructor)dependency).getDependency());

      }

      @Test
      public void should_throw_exception_if_multi_inject_constructors_found() {
        assertThrows(IllegalComponentException.class, () -> context.bind(Component.class, ComponentWithMultiInjectConstructors.class));
      }

      @Test
      public void should_throw_exception_if_no_inject_constructor_nor_default_method_found() {
        assertThrows(IllegalComponentException.class, () ->
            context.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class));

      }

      @Test
      public void should_throw_exception_if_no_dependency_provided() {
        context.bind(Component.class, ComponentWithInjectConstructor.class);
        assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class).get());
      }

      @Test
      public void should_return_empty_if_component_is_undefined() {
        Optional<Component> optionalComponent = context.get(Component.class);
        assertTrue(optionalComponent.isEmpty());
      }

      @Test
      public void should_throw_exception_if_cyclic_dependencies_found() {
        context.bind(Component.class, ComponentWithInjectConstructor.class);
        context.bind(Dependency.class, DependencyDependOnComponent.class);

        assertThrows(CyclicDependenciesFoundException.class, () -> context.get(Dependency.class));
      }

      // A->B-, B->C, C->A
      @Test
      public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
        context.bind(Component.class, ComponentWithInjectConstructor.class);
        context.bind(AnotherDependency.class, AnotherDependencyDependOnDependency.class);
        context.bind(Dependency.class, DependencyDependOnComponent.class);

        assertThrows(CyclicDependenciesFoundException.class, () -> context.get(Dependency.class));

      }



      //TODO abstract class
      //TODO interface
    }
    @Nested
    public class FieldInjection {


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
interface Component {}

interface Dependency {}


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

class ComponentWithMultiInjectConstructors implements Component{

  @Inject
  public ComponentWithMultiInjectConstructors(String name, Integer age) {
  }

  @Inject
  public ComponentWithMultiInjectConstructors(String name) {
  }
}

class ComponentWithNoInjectNorDefaultConstructor implements Component{

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

interface AnotherDependency {}

class AnotherDependencyDependOnDependency implements AnotherDependency{
  private Dependency dependency;

  @Inject
  public AnotherDependencyDependOnDependency(Dependency dependency) {
    this.dependency = dependency;
  }
}


