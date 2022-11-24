package com.tdd.study;

import static java.util.Arrays.stream;

import com.tdd.study.exception.CyclicDependenciesFoundException;
import com.tdd.study.exception.DependencyNotFoundException;
import com.tdd.study.exception.IllegalComponentException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;


public class ContextConfig {

  Map<Class<?>, ComponentProvider<?>> componentProviderMap = new HashMap<>();

  Map<Component, ComponentProvider<?>> components = new HashMap<>();

  record Component(Class<?> type, Annotation qualifier) {}

  public <T> void bind(Class<T> type, T instance) {
    componentProviderMap.put(type, (ComponentProvider) context -> instance);
  }

  public <T> void bind(Class<T> type, T instance, Annotation qualifier) {
    components.put(new Component(type, qualifier), context -> instance);
  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation) {
//    List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
//        .filter(constructor1 -> constructor1.isAnnotationPresent(
//            Inject.class)).toList();
//    if (injectConstructors.size() > 1) {
//      throw new IllegalComponentException();
//    }
    componentProviderMap.put(type, new InjectionProvider<>(implementation));

  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation, Annotation qualifier) {
    components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
  }

  public Context getContext() {
    componentProviderMap.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
    return new Context() {

      @Override
      public <T> Optional<T> get(Ref<T> ref) {
        if (ref.getQualifier() != null) {
          return Optional.ofNullable(components.get(new Component(ref.getComponentType(), ref.getQualifier())))
              .map(provider -> (T) provider.get(this));
        }
        if (ref.isContainer()) {
          if (ref.getContainerType() != Provider.class) return Optional.empty();

          return (Optional<T>) Optional.ofNullable(componentProviderMap.get(ref.getComponentType()))
              .map(componentProvider -> (Provider<Object>) () -> (Object) componentProvider.get(this));

        }
        return Optional.ofNullable(componentProviderMap.get(ref.getComponentType()))
            .map(provider -> (T) provider.get(this));
      }
    };
  }

  private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
    for (Context.Ref dependency : componentProviderMap.get(component).getDependencies()) {
      if (!componentProviderMap.containsKey(dependency.getComponentType())) {
        throw new DependencyNotFoundException(component, dependency.getComponentType());
      }
      if (!dependency.isContainer()) {
        if (visiting.contains(dependency.getComponentType())) {
          throw new CyclicDependenciesFoundException(visiting);
        }
        visiting.push(dependency.getComponentType());
        checkDependencies(dependency.getComponentType(), visiting);
        visiting.pop();
      }

    }

  }

}
