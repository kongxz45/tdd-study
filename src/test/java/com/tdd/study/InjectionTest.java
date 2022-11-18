package com.tdd.study;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Nested
public class InjectionTest {

  private Dependency dependency = mock(Dependency.class);

  private Provider<Dependency> dependencyProvider = mock(Provider.class);

  private Context context = mock(Context.class);

  @BeforeEach
  public void setUp() throws NoSuchFieldException {
    ParameterizedType type = (ParameterizedType)InjectionTest.class.getDeclaredField("dependencyProvider")
        .getGenericType();
    when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    when(context.get(eq(type))).thenReturn(Optional.of(dependencyProvider));
  }


  @Nested
  public class ConstructorInjection {

    @Nested
    class Injection {

      // default constructor
      @Test
      public void should_inject_dependency_with_default_constructor() {
        Component instance = new InjectionProvider<>(
            ComponentWithDefaultConstructor.class).get(context);

        assertNotNull(instance);
        assertTrue(instance instanceof ComponentWithDefaultConstructor);

      }


      // transitive dependency also included
      @Test
      public void should_inject_dependency_with_inject_constructor() {

        ComponentWithInjectConstructor instance = new InjectionProvider<>(
            ComponentWithInjectConstructor.class).get(context);
        assertNotNull(instance);
        assertSame(dependency, instance.getDependency());
      }

      @Test
      public void should_include_dependency_from_inject_constructor() {
        InjectionProvider provider = new InjectionProvider<>(
            ComponentWithInjectConstructor.class);
        assertArrayEquals(new Class<?>[]{Dependency.class},
            provider.getDependencies().toArray(Class<?>[]::new));
      }

      @Test
      public void should_inject_provider_via_inject_constructor() {
        ProviderInjectConstructor instance = new InjectionProvider<>(
            ProviderInjectConstructor.class).get(context);

        assertSame(dependencyProvider, instance.dependency);

      }

      static class ProviderInjectConstructor {
        Provider<Dependency> dependency;

        @Inject
        public ProviderInjectConstructor(Provider<Dependency> dependency) {
          this.dependency = dependency;
        }

      }
    }

    @Nested
    class IllegalConstructionInjection {

      @Test
      public void should_throw_exception_if_multi_inject_constructors_found() {
        assertThrows(IllegalComponentException.class,
            () -> new InjectionProvider<>(
                ComponentWithMultiInjectConstructors.class));
      }

      @Test
      public void should_throw_exception_if_no_inject_constructor_nor_default_method_found() {
        assertThrows(IllegalComponentException.class,
            () -> new InjectionProvider<>(
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

        assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(
            AbstractComponent.class));
      }

      //interface
      @Test
      public void should_throw_exception_if_component_is_interface() {
        assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(
            Component.class));

      }
    }

  }


  @Nested
  public class FieldInjection {

    @Nested
    class Injection {

      static class ComponentWithFieldInjection {

        @Inject
        Dependency dependency;
      }

      static class SubClassWithFieldInjection extends ComponentWithFieldInjection {

      }

      @Test
      public void should_inject_dependency_via_field() {

        ComponentWithFieldInjection component = new InjectionProvider<>(
            ComponentWithFieldInjection.class).get(context);

        assertSame(dependency, component.dependency);
      }

      @Test
      public void should_include_field_dependency_in_dependencies() {
        InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(
            ComponentWithFieldInjection.class);
        assertArrayEquals(new Class<?>[]{Dependency.class},
            provider.getDependencies().toArray(Class<?>[]::new));
      }

      @Test
      public void should_inject_dependency_via_superclass_inject_field() {

        SubClassWithFieldInjection component = new InjectionProvider<>(
            SubClassWithFieldInjection.class).get(context);

        assertSame(dependency, component.dependency);

      }

      @Test
      public void should_inject_provider_via_inject_field() {
        ProviderInjectField instance = new InjectionProvider<>(ProviderInjectField.class).get(context);
        assertSame(dependencyProvider, instance.dependency);

      }

      static class ProviderInjectField {
        @Inject
        Provider<Dependency> dependency;

      }

    }

    @Nested
    class IllegalFieldInjection {

      static class FinalInjectField {

        @Inject
        final Dependency dependency = null;
      }

      //throw exception if field is final
      @Test
      public void should_throw_exception_if_inject_field_is_final() {
        assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(
            FinalInjectField.class));
      }
    }

  }


  @Nested
  public class MethodInjection {

    @Nested
    class Injection {

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

        InjectMethodWithNoDependency component = new InjectionProvider<>(
            InjectMethodWithNoDependency.class).get(context);

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

        InjectMethodWithDependency component = new InjectionProvider<>(
            InjectMethodWithDependency.class).get(context);

        assertSame(dependency, component.dependency);
      }

      //include dependencies from inject methods
      @Test
      public void should_include_dependencies_from_inject_method() {
        InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(
            InjectMethodWithDependency.class);

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

      static class SubClassWithInjectMethod extends SuperClassWithInjectMethod {

        int subCalled = 0;

        @Inject
        void installAnother() {
          subCalled = superCalled + 1;
        }
      }

      @Test
      public void should_execute_method_via_inject_method_from_superclass() {

        SubClassWithInjectMethod component = new InjectionProvider<>(
            SubClassWithInjectMethod.class).get(context);

        assertEquals(1, component.superCalled);
        assertEquals(2, component.subCalled);
      }

      static class SubClassOverrideSuperClass extends SuperClassWithInjectMethod {

        @Inject
        void install() {
          super.install();
        }
      }

      @Test
      public void should_only_call_once_if_subclass_override_superclass_with_inject() {

        SubClassOverrideSuperClass component = new InjectionProvider<>(
            SubClassOverrideSuperClass.class).get(context);

        assertEquals(1, component.superCalled);
      }

      static class SubClassOverrideSuperClassWithNoInject extends
          SuperClassWithInjectMethod {

        void install() {
          super.install();
        }
      }

      @Test
      public void should_not_call_inject_method_if_subclass_override_superclass_with_inject() {

        SubClassOverrideSuperClassWithNoInject component = new InjectionProvider<>(
            SubClassOverrideSuperClassWithNoInject.class).get(context);

        assertEquals(0, component.superCalled);
      }

      @Test
      public void should_inject_provider_via_inject_method() {
        ProviderInjectMethod instance = new InjectionProvider<>(
            ProviderInjectMethod.class).get(context);

        assertSame(dependencyProvider, instance.dependency);

      }

      static class ProviderInjectMethod {
        Provider<Dependency> dependency;

        @Inject
        public void install(Provider<Dependency> dependency) {
          this.dependency = dependency;
        }

      }
    }

    @Nested
    class IllegalMethodInjection {

      //throw exception if type parameter defined
      static class InjectMethodWithTypeParameter {

        @Inject
        <T> void install() {
        }
      }

      @Test
      public void should_throw_exception_if_inject_method_has_type_parameter() {
        assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(
            InjectMethodWithTypeParameter.class));
      }
    }

  }
}
