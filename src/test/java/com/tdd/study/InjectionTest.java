package com.tdd.study;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Nested
public class InjectionTest {

  ContextConfig config;

  @BeforeEach
  public void setUp() {
    config = new ContextConfig();
  }

  @Nested
  public class ConstructorInjection {

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
          () -> new ConstructorInjectionProvider<>(
              ComponentWithMultiInjectConstructors.class));
    }

    @Test
    public void should_include_dependency_from_inject_constructor() {
      ConstructorInjectionProvider provider = new ConstructorInjectionProvider<>(
          ComponentWithInjectConstructor.class);
      assertArrayEquals(new Class<?>[]{Dependency.class},
          provider.getDependencies().toArray(Class<?>[]::new));
    }

    @Test
    public void should_throw_exception_if_no_inject_constructor_nor_default_method_found() {
      assertThrows(IllegalComponentException.class,
          () -> new ConstructorInjectionProvider<>(
              ComponentWithNoInjectNorDefaultConstructor.class));

    }

    static abstract class AbstractComponent implements Component {

      @Inject
      public AbstractComponent() {
      }
    }

    //abstract class
    @Test
    public void should_throw_exception_if_component_is_abstract() {

      assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(
          ConstructorInjection.AbstractComponent.class));
    }

    //interface
    @Test
    public void should_throw_exception_if_component_is_interface() {
      assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(
          Component.class));

    }
  }


  @Nested
  public class FieldInjection {

    static class ComponentWithFieldInjection {

      @Inject
      Dependency dependency;
    }

    static class SubClassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection {

    }

    @Test
    public void should_inject_dependency_via_field() {
      Dependency dependency = new Dependency() {
      };

      config.bind(Dependency.class, dependency);
      config.bind(
          FieldInjection.ComponentWithFieldInjection.class,
          FieldInjection.ComponentWithFieldInjection.class);

      FieldInjection.ComponentWithFieldInjection component = config.getContext()
          .get(FieldInjection.ComponentWithFieldInjection.class).get();

      assertSame(dependency, component.dependency);
    }

    @Test
    public void should_include_field_dependency_in_dependencies() {
      ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(
          FieldInjection.ComponentWithFieldInjection.class);
      assertArrayEquals(new Class<?>[]{Dependency.class},
          provider.getDependencies().toArray(Class<?>[]::new));
    }

    @Test
    public void should_inject_dependency_via_superclass_inject_field() {
      Dependency dependency = new Dependency() {
      };

      config.bind(Dependency.class, dependency);
      config.bind(
          FieldInjection.SubClassWithFieldInjection.class,
          FieldInjection.SubClassWithFieldInjection.class);

      FieldInjection.SubClassWithFieldInjection component = config.getContext()
          .get(FieldInjection.SubClassWithFieldInjection.class).get();

      assertSame(dependency, component.dependency);

    }

  }


  @Nested
  public class MethodInjection {

    static class InjectMethodWithNoDependency {

      boolean called = false;

      @Inject
      void install() {
        called = true;
      }
    }

    //inject method with no dependencies will be called
    @Test
    public void should_execute_inject_method_even_if_no_dependency() {
      config.bind(
          MethodInjection.InjectMethodWithNoDependency.class,
          MethodInjection.InjectMethodWithNoDependency.class);
      MethodInjection.InjectMethodWithNoDependency component = config.getContext()
          .get(MethodInjection.InjectMethodWithNoDependency.class).get();

      assertTrue(component.called);
    }

    //inject method with dependencies will be injected
    static class InjectMethodWithDependency {

      Dependency dependency;

      @Inject
      void install(Dependency dependency) {
        this.dependency = dependency;
      }
    }

    @Test
    public void should_inject_dependency_with_inject_method() {
      Dependency dependency = new Dependency() {
      };
      config.bind(Dependency.class, dependency);
      config.bind(
          MethodInjection.InjectMethodWithDependency.class,
          MethodInjection.InjectMethodWithDependency.class);

      MethodInjection.InjectMethodWithDependency component = config.getContext()
          .get(MethodInjection.InjectMethodWithDependency.class).get();

      assertSame(dependency, component.dependency);
    }

    //include dependencies from inject methods
    @Test
    public void should_include_dependencies_from_inject_method() {
      ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(
          MethodInjection.InjectMethodWithDependency.class);

      assertArrayEquals(new Class<?>[]{Dependency.class},
          provider.getDependencies().toArray(Object[]::new));
    }

    //override inject method from superclass
    static class SuperClassWithInjectMethod {

      int superCalled = 0;

      @Inject
      void install() {
        superCalled++;
      }
    }

    static class SubClassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {

      int subCalled = 0;

      @Inject
      void installAnother() {
        subCalled = superCalled + 1;
      }
    }

    @Test
    public void should_execute_method_via_inject_method_from_superclass() {
      config.bind(
          MethodInjection.SubClassWithInjectMethod.class,
          MethodInjection.SubClassWithInjectMethod.class);

      MethodInjection.SubClassWithInjectMethod component = config.getContext()
          .get(MethodInjection.SubClassWithInjectMethod.class).get();

      assertEquals(1, component.superCalled);
      assertEquals(2, component.subCalled);
    }

    static class SubClassOverrideSuperClass extends MethodInjection.SuperClassWithInjectMethod {

      @Inject
      void install() {
        super.install();
      }
    }

    @Test
    public void should_only_call_once_if_subclass_override_superclass_with_inject() {
      config.bind(
          MethodInjection.SubClassOverrideSuperClass.class,
          MethodInjection.SubClassOverrideSuperClass.class);

      MethodInjection.SubClassOverrideSuperClass component = config.getContext()
          .get(MethodInjection.SubClassOverrideSuperClass.class).get();

      assertEquals(1, component.superCalled);
    }

    static class SubClassOverrideSuperClassWithNoInject extends
        MethodInjection.SuperClassWithInjectMethod {

      void install() {
        super.install();
      }
    }

    @Test
    public void should_not_call_inject_method_if_subclass_override_superclass_with_inject() {
      config.bind(MethodInjection.SubClassOverrideSuperClassWithNoInject.class,
          MethodInjection.SubClassOverrideSuperClassWithNoInject.class);

      MethodInjection.SubClassOverrideSuperClassWithNoInject component = config.getContext()
          .get(MethodInjection.SubClassOverrideSuperClassWithNoInject.class).get();

      assertEquals(0, component.superCalled);
    }

    static class FinalInjectField {

      @Inject
      final Dependency dependency = null;
    }

    //throw exception if field is final
    @Test
    public void should_throw_exception_if_inject_field_is_final() {
      assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(
          MethodInjection.FinalInjectField.class));
    }

    //throw exception if type parameter defined
    static class InjectMethodWithTypeParameter {

      @Inject
      <T> void install() {
      }
    }

    @Test
    public void should_throw_exception_if_inject_method_has_type_parameter() {
      assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(
          MethodInjection.InjectMethodWithTypeParameter.class));
    }

  }
}
