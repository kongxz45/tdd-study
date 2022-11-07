package com.tdd.study;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class ContextConfig {

  Map<Class<?>, ComponentProvider<?>> componentProviderMap = new HashMap<>();

  Map<Class<?>, List<Class<?>>> dependenciesMap = new HashMap<>();

  public <type> void bind(Class<type> type, type instance) {
    componentProviderMap.put(type, context -> instance);
    dependenciesMap.put(type, asList());
  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation) {
    Constructor<?> constructor = getConstructor(implementation);
    componentProviderMap.put(type, new ConstructorInjectionProvider<>(type, constructor));
    dependenciesMap.put(type,
        stream(constructor.getParameters()).map(parameter -> parameter.getType()).collect(
            Collectors.toList()));

  }

  public Context getContext() {
    for (Class<?> component : dependenciesMap.keySet()) {
      for (Class<?> dependency : dependenciesMap.get(component)) {
        if (!dependenciesMap.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
      }
    }
    return new Context() {
      @Override
      public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(componentProviderMap.get(type))
            .map(provider -> (Type) provider.get(this));
      }
    };
  }

  private static <Type> Constructor<Type> getConstructor(Class<Type> implementation) {
    List<Constructor> injectConstructors = stream(implementation.getConstructors())
        .filter(constructor -> constructor.isAnnotationPresent(
            Inject.class)).collect(Collectors.toList());
    if (injectConstructors.size() > 1) {
      throw new IllegalComponentException();
    }
    return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
      try {
        return implementation.getConstructor();
      } catch (NoSuchMethodException e) {
        throw new IllegalComponentException();
      }
    });
  }

  interface ComponentProvider<T> {

    T get(Context context);
  }

  class ConstructorInjectionProvider<T> implements ComponentProvider<T> {

    private boolean constructing = false;
    private Class<?> componentType;

    private Constructor<T> constructor;

    public ConstructorInjectionProvider(Class<?> componentType, Constructor<T> constructor) {
      this.constructor = constructor;
      this.componentType = componentType;
    }

    @Override
    public T get(Context context) {
      if (constructing) {
        throw new CyclicDependenciesFoundException(componentType);
      }
      constructing = true;
      try {
        Object[] dependencies = stream(constructor.getParameters())
            .map(
                parameter -> context.get(parameter.getType()).orElseThrow(
                    () -> new DependencyNotFoundException(componentType, parameter.getType())))
            .toArray(Object[]::new);
        return (T) ((Constructor<?>) constructor).newInstance(dependencies);
      } catch (CyclicDependenciesFoundException e) {
        throw new CyclicDependenciesFoundException(componentType, e);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      } finally {
        constructing = false;
      }
    }
  }
}
