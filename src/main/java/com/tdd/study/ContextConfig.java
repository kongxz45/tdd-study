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
import java.util.function.Function;


public class ContextConfig {

  private Map<Component, ComponentProvider<?>> components = new HashMap<>();

  private Map<Class<?>, Function<ComponentProvider<?>, ComponentProvider<?>>> scopes = new HashMap<>();

  public ContextConfig() {
    scope(Singleton.class, SingletonProvider::new);
  }

  public <T> void bind(Class<T> type, T instance) {
    components.put(new Component(type, null), context -> instance);
  }

  public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
    if (stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class)))
      throw new IllegalComponentException();
    for (Annotation qualifier : qualifiers)
      components.put(new Component(type, qualifier), context -> instance);
  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation) {
    bind(type, implementation, implementation.getAnnotations());

  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation, Annotation... annotations) {
    if (stream(annotations).map(annotation -> annotation.annotationType()).anyMatch(t -> !t.isAnnotationPresent(
            Qualifier.class) && !t.isAnnotationPresent(Scope.class)))
      throw new IllegalComponentException();

    List<Annotation> qualifiers = stream(annotations)
        .filter(annotation -> annotation.annotationType().isAnnotationPresent(
            Qualifier.class)).toList();

    InjectionProvider<?> injectionProvider = new InjectionProvider<>(implementation);

    Optional<Annotation> scopeFromType = stream(implementation.getAnnotations()).filter(
        annotation -> annotation.annotationType().isAnnotationPresent(
            Scope.class)).findFirst();

    Optional<Annotation> scope = stream(annotations)
        .filter(annotation -> annotation.annotationType().isAnnotationPresent(
            Scope.class)).findFirst().or(() -> scopeFromType);
    ComponentProvider<?> provider = scope.<ComponentProvider<?>>map(
            s -> getScopeProvider(s, injectionProvider))
        .orElse(injectionProvider);
    if (qualifiers.isEmpty()) {
      components.put(new Component(type, null), provider);
      return;
    }
    for (Annotation qualifier : qualifiers)
      components.put(new Component(type, qualifier), provider);

  }

  private ComponentProvider<?> getScopeProvider(Annotation scope, InjectionProvider<?> provider) {
    return scopes.get(scope.annotationType()).apply(provider);
  }

  public <T extends Annotation> void scope(Class<T> scope, Function<ComponentProvider<?>, ComponentProvider<?>> provider) {
    scopes.put(scope, provider);
  }

  static class SingletonProvider<T> implements ComponentProvider<T> {

    private T singleton;

    private ComponentProvider<T> provider;

    public SingletonProvider(ComponentProvider<T> provider) {
      this.provider = provider;
    }

    @Override
    public T get(Context context) {
      if (singleton == null)
        singleton = provider.get(context);
      return singleton;
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
      return provider.getDependencies();
    }
  }

  public Context getContext() {
    components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
    return new Context() {

      @Override
      public <T> Optional<T> get(ComponentRef<T> ref) {
        if (ref.isContainer()) {
          if (ref.getContainerType() != Provider.class) return Optional.empty();

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
