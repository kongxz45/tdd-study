package com.tdd.study;

import static java.util.Arrays.stream;

import com.tdd.study.exception.CyclicDependenciesFoundException;
import com.tdd.study.exception.DependencyNotFoundException;
import com.tdd.study.exception.IllegalComponentException;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ContextConfig {

  private Map<Component, ComponentProvider<?>> components = new HashMap<>();

  private Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

  public ContextConfig() {
    scope(Singleton.class, SingletonProvider::new);
  }

  public <T> void bind(Class<T> type, T instance) {
    components.put(new Component(type, null), context -> instance);
  }

  public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
    if (stream(qualifiers).anyMatch(
        q -> !q.annotationType().isAnnotationPresent(Qualifier.class))) {
      throw new IllegalComponentException();
    }
    for (Annotation qualifier : qualifiers) {
      components.put(new Component(type, qualifier), context -> instance);
    }
  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation) {
    bind(type, implementation, implementation.getAnnotations());

  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation, Annotation... annotations) {

    Map<? extends Class<?>, List<Annotation>> annotationGroups = stream(annotations).collect(
        Collectors.groupingBy(this::typeOf, Collectors.toList()));

    if (annotationGroups.containsKey(Illegal.class)) {
      throw new IllegalComponentException();
    }

    bind(type, annotationGroups.getOrDefault(Qualifier.class, List.of()),
        createScopedProvider(implementation,
            annotationGroups.getOrDefault(Scope.class, List.of())));

  }

  private <Type, Implementation extends Type> ComponentProvider<?> createScopedProvider(
      Class<Implementation> implementation, List<Annotation> scopes) {
    if (scopes.size() > 1) {
      throw new IllegalComponentException();
    }
    Optional<Annotation> scope = scopes.stream().findFirst().or(() -> getScopeFromType(
        implementation));
    InjectionProvider<?> injectionProvider = new InjectionProvider<>(implementation);

    ComponentProvider<?> provider = scope.<ComponentProvider<?>>map(
            s -> getScopeProvider(s, injectionProvider))
        .orElse(injectionProvider);
    return provider;
  }

  private <Type> void bind(Class<Type> type, List<Annotation> qualifiers,
      ComponentProvider<?> provider) {
    if (qualifiers.isEmpty()) {
      components.put(new Component(type, null), provider);
      return;
    }
    for (Annotation qualifier : qualifiers) {
      components.put(new Component(type, qualifier), provider);
    }
  }

  private static <Type, Implementation extends Type> Optional<Annotation> getScopeFromType(
      Class<Implementation> implementation) {
    return stream(implementation.getAnnotations()).filter(
        annotation -> annotation.annotationType().isAnnotationPresent(
            Scope.class)).findFirst();
  }

  private Class<?> typeOf(Annotation annotation) {
    Class<? extends Annotation> type = annotation.annotationType();
    return Stream.of(Qualifier.class, Scope.class).filter(type::isAnnotationPresent).findFirst()
        .orElse(Illegal.class);
  }

  private @interface Illegal {

  }

  private ComponentProvider<?> getScopeProvider(Annotation scope, InjectionProvider<?> provider) {
    if (!scopes.containsKey(scope.annotationType())) {
      throw new IllegalComponentException();
    }
    return scopes.get(scope.annotationType()).create(provider);
  }

  public <T extends Annotation> void scope(Class<T> scope, ScopeProvider provider) {
    scopes.put(scope, provider);
  }

  public Context getContext() {
    components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
    return new Context() {

      @Override
      public <T> Optional<T> get(ComponentRef<T> ref) {
        if (ref.isContainer()) {
          if (ref.getContainerType() != Provider.class) {
            return Optional.empty();
          }

          return (Optional<T>) Optional.ofNullable(
                  components.get(ref.component()))
              .map(componentProvider -> (Provider<T>) () -> (T) componentProvider.get(this));

        }
        return Optional.ofNullable(
                components.get(ref.component()))
            .map(provider -> (T) provider.get(this));
      }
    };
  }

  private void checkDependencies(Component component, Stack<Component> visiting) {
    for (ComponentRef dependency : components.get(component).getDependencies()) {
      if (!components.containsKey(dependency.component())) {
        throw new DependencyNotFoundException(component, dependency.component());
      }
      if (!dependency.isContainer()) {
        if (visiting.contains(dependency.component())) {
          throw new CyclicDependenciesFoundException(visiting);
        }
        visiting.push(dependency.component());
        checkDependencies(dependency.component(), visiting);
        visiting.pop();
      }

    }

  }

}
