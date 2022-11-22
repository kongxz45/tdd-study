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
      @Override
      public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(componentProviderMap.get(type))
            .map(provider -> (Type) provider.get(this));
      }

      @Override
      public <Type> Optional<Type> get(ParameterizedType type) {
        if (type.getRawType() != Provider.class) return Optional.empty();
        Class<?> componentType = (Class<?>)type.getActualTypeArguments()[0];
        return (Optional<Type>) Optional.ofNullable(componentProviderMap.get(componentType))
            .map(componentProvider -> (Provider<Type>) () -> (Type) componentProvider.get(this));

      }
    };
  }

  private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
    for (Type dependency : componentProviderMap.get(component).getDependencyTypes()) {
      if (dependency instanceof Class) {
        extracted(component, visiting, (Class<?>) dependency);
      }
      if (dependency instanceof ParameterizedType) {
        Class<?> type = (Class<?>) ((ParameterizedType) dependency).getActualTypeArguments()[0];
        if (!componentProviderMap.containsKey(type)) {
          throw new DependencyNotFoundException(component, type);
        }
      }

    }

  }

  private void extracted(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
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
