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

import com.tdd.study.ContextTest.WithQualifier.NamedLiteral;
import com.tdd.study.InjectionTest.ConstructorInjection.WithQualifier.InjectConstructorWithQualifier;
import com.tdd.study.InjectionTest.FieldInjection.WithQualifier.InjectFieldWithQualifier;
import com.tdd.study.exception.IllegalComponentException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Nested
public class InjectionTest {

  private Dependency dependency = mock(Dependency.class);

  private Provider<Dependency> dependencyProvider = mock(Provider.class);

  private ParameterizedType dependencyProviderType;

  private Context context = mock(Context.class);

  @BeforeEach
  public void setUp() throws NoSuchFieldException {
    dependencyProviderType = (ParameterizedType)InjectionTest.class.getDeclaredField("dependencyProvider")
        .getGenericType();
    when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(dependency));
    when(context.get(eq(ComponentRef.of(dependencyProviderType)))).thenReturn(Optional.of(dependencyProvider));
  }


  @Nested
  public class ConstructorInjection {

    @Nested
    class Injection {

      static class ComponentWithDefaultConstructor implements TestComponent {

        public ComponentWithDefaultConstructor() {
        }
      }
      @Test
      public void should_inject_dependency_with_default_constructor() {
        TestComponent instance = new InjectionProvider<>(
            ComponentWithDefaultConstructor.class).get(context);

        assertNotNull(instance);
        assertTrue(instance instanceof ComponentWithDefaultConstructor);

      }


      static class ComponentInjectDependencyWithConstructor implements TestComponent {

        private Dependency dependency;

        @Inject
        public ComponentInjectDependencyWithConstructor(Dependency dependency) {
          this.dependency = dependency;
        }

        public Dependency getDependency() {
          return dependency;
        }

      }

      // transitive dependency also included
      @Test
      public void should_inject_dependency_with_inject_constructor() {

        ComponentInjectDependencyWithConstructor instance = new InjectionProvider<>(
            ComponentInjectDependencyWithConstructor.class).get(context);
        assertNotNull(instance);
        assertSame(dependency, instance.getDependency());
      }

      @Test
      public void should_include_dependency_from_inject_constructor() {
        InjectionProvider provider = new InjectionProvider<>(
            ComponentInjectDependencyWithConstructor.class);
        assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)},
            provider.getDependencies().toArray(ComponentRef[]::new));
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

      @Test
      public void should_include_dependency_type_from_inject_constructor() {
        InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(
            ProviderInjectConstructor.class);

        assertArrayEquals(new ComponentRef[] {ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray(
            ComponentRef[]::new));
      }



    }

    @Nested
    class WithQualifier {
      @BeforeEach
      public void beforeEach() {
        Mockito.reset(context);
        when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))))).thenReturn(Optional.of(dependency));
      }

      @Test
      public void should_inject_dependency_with_qualifier_via_constructor() {
        InjectionProvider<InjectConstructorWithQualifier> provider = new InjectionProvider<>(
            InjectConstructorWithQualifier.class);

        InjectConstructorWithQualifier component = provider.get(context);

        assertSame(dependency, component.dependency);
      }

      @Test
      public void should_include_dependency_with_qualifier() {
        InjectionProvider<InjectConstructorWithQualifier> provider = new InjectionProvider<>(
            InjectConstructorWithQualifier.class);
        assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))}, provider.getDependencies().toArray() );


      }

      static class InjectConstructorWithQualifier {

        Dependency dependency;

        @Inject
        public InjectConstructorWithQualifier(@Named("ChosenOne") Dependency dependency) {
          this.dependency = dependency;
        }

      }
    }

    @Nested
    class IllegalConstructionInjection {


      class ComponentWithMultiInjectConstructors implements TestComponent {

        @Inject
        public ComponentWithMultiInjectConstructors(String name, Integer age) {
        }

        @Inject
        public ComponentWithMultiInjectConstructors(String name) {
        }
      }
      @Test
      public void should_throw_exception_if_multi_inject_constructors_found() {
        assertThrows(IllegalComponentException.class,
            () -> new InjectionProvider<>(
                ComponentWithMultiInjectConstructors.class));
      }

      static class ComponentWithNoInjectNorDefaultConstructor implements TestComponent {

        public ComponentWithNoInjectNorDefaultConstructor(String name) {
        }
      }

      @Test
      public void should_throw_exception_if_no_inject_constructor_nor_default_method_found() {
        assertThrows(IllegalComponentException.class,
            () -> new InjectionProvider<>(
                ComponentWithNoInjectNorDefaultConstructor.class));

      }

      static abstract class AbstractComponent implements TestComponent {

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
            TestComponent.class));

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
        assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)},
            provider.getDependencies().toArray(ComponentRef[]::new));
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

      @Test
      public void should_include_dependency_type_from_inject_field() {
        InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(
            ProviderInjectField.class);

        assertArrayEquals(new ComponentRef[] {ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray(
            ComponentRef[]::new));
      }

    }

    @Nested
    class WithQualifier {
      @BeforeEach
      public void beforeEach() {
        Mockito.reset(context);
        when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))))).thenReturn(Optional.of(dependency));
      }

      @Test
      public void should_inject_dependency_with_qualifier_via_field() {
        InjectionProvider<InjectFieldWithQualifier> provider = new InjectionProvider<>(
            InjectFieldWithQualifier.class);

        InjectFieldWithQualifier component = provider.get(context);

        assertSame(dependency, component.dependency);
      }

      @Test
      public void should_include_dependency_with_qualifier() {
        InjectionProvider<InjectFieldWithQualifier> provider = new InjectionProvider<>(
            InjectFieldWithQualifier.class);
        assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))}, provider.getDependencies().toArray() );

      }

      static class InjectFieldWithQualifier {

        @Inject
        @Named("ChosenOne")
        Dependency dependency;
      }
      //TODO include qualifier with dependency
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

        assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)},
            provider.getDependencies().toArray(ComponentRef[]::new));
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

      @Test
      public void should_include_dependency_type_from_inject_method() {
        InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(
            ProviderInjectMethod.class);

        assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray(
            ComponentRef[]::new));
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
    class WithQualifier {
      @BeforeEach
      public void beforeEach() {
        Mockito.reset(context);
        when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))))).thenReturn(Optional.of(dependency));
      }

      @Test
      public void should_inject_dependency_with_qualifier_via_method() {
        InjectionProvider<InjectMethodWithQualifier> provider = new InjectionProvider<>(
            InjectMethodWithQualifier.class);

        InjectMethodWithQualifier component = provider.get(context);

        assertSame(dependency, component.dependency);
      }

      @Test
      public void should_include_dependency_with_qualifier() {
        InjectionProvider<InjectMethodWithQualifier> provider = new InjectionProvider<>(
            InjectMethodWithQualifier.class);
        assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))}, provider.getDependencies().toArray() );

      }

      static class InjectMethodWithQualifier {

        Dependency dependency;

        @Inject
        void install(@Named("ChosenOne") Dependency dependency) {
          this.dependency = dependency;
        }
      }
      //TODO include qualifier with dependency
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
