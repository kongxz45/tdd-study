package com.tdd.study;

import static java.util.Arrays.stream;

import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
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
    };
  }

  private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
    for (Class<?> dependency : componentProviderMap.get(component).getDependencies()) {
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

}
