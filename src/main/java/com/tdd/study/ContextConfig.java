package com.tdd.study;

import static java.util.Arrays.stream;

import com.tdd.study.exception.CyclicDependenciesFoundException;
import com.tdd.study.exception.DependencyNotFoundException;
import com.tdd.study.exception.IllegalComponentException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;


public class ContextConfig {

  Map<Class<?>, ComponentProvider<?>> componentProviderMap = new HashMap<>();

  public <type> void bind(Class<type> type, type instance) {
    componentProviderMap.put(type, (ComponentProvider) context -> instance);
  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation) {
    List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
        .filter(constructor1 -> constructor1.isAnnotationPresent(
            Inject.class)).toList();
    if (injectConstructors.size() > 1) {
      throw new IllegalComponentException();
    }
    componentProviderMap.put(type, new InjectionProvider<>(implementation));

  }

  public Context getContext() {
    componentProviderMap.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
    return new Context() {
      private <Type> Optional<Type> getComponent(Class<Type> type) {
        return Optional.ofNullable(componentProviderMap.get(type))
            .map(provider -> (Type) provider.get(this));
      }

      private <Type> Optional<Type> getContainer(ParameterizedType type) {
        if (type.getRawType() != Provider.class) return Optional.empty();
        Class<?> componentType = getContainerType(type);
        return (Optional<Type>) Optional.ofNullable(componentProviderMap.get(componentType))
            .map(componentProvider -> (Provider<Type>) () -> (Type) componentProvider.get(this));

      }

      @Override
      public Optional getType(Type type) {
        if (isContainerType(type))
          return getContainer((ParameterizedType) type);
        return getComponent((Class<?>) type);
      }
    };
  }

  private static Class<?> getContainerType(Type type) {
    return (Class<?>) ((ParameterizedType)type).getActualTypeArguments()[0];
  }

  private static boolean isContainerType(Type type) {
    return type instanceof ParameterizedType;
  }

  private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
    for (Type dependency : componentProviderMap.get(component).getDependencies()) {
      if (dependency instanceof Class) {
        checkComponentDependency(component, visiting, (Class<?>) dependency);
      } else {
        checkContainerDependency(component, dependency);
      }

    }

  }

  private void checkContainerDependency(Class<?> component, Type dependency) {
    Class<?> type = getContainerType(dependency);
    if (!componentProviderMap.containsKey(type)) {
      throw new DependencyNotFoundException(component, type);
    }
  }

  private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
    if (!componentProviderMap.containsKey(dependency)) {
      throw new DependencyNotFoundException(component, dependency);
    }
    if (visiting.contains(dependency)) {
      throw new CyclicDependenciesFoundException(visiting);
    }
    visiting.push(dependency);
    checkDependencies(dependency, visiting);
    visiting.pop();
  }

}
