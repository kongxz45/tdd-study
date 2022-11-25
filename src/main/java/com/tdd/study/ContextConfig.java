package com.tdd.study;

import com.tdd.study.exception.CyclicDependenciesFoundException;
import com.tdd.study.exception.DependencyNotFoundException;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;


public class ContextConfig {

  Map<Component, ComponentProvider<?>> components = new HashMap<>();

  public <T> void bind(Class<T> type, T instance) {
    components.put(new Component(type, null), context -> instance);
  }

  public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
    for (Annotation qualifier : qualifiers)
      components.put(new Component(type, qualifier), context -> instance);
  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation) {
    components.put(new Component(type, null), new InjectionProvider<>(implementation));

  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation, Annotation... qualifiers) {
    for (Annotation qualifier : qualifiers)
      components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
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

  private void checkDependencies(Component component, Stack<Class<?>> visiting) {
    for (ComponentRef dependency : components.get(component).getDependencies()) {
      if (!components.containsKey(dependency.component())) {
        throw new DependencyNotFoundException(component.type(), dependency.getComponentType());
      }
      if (!dependency.isContainer()) {
        if (visiting.contains(dependency.getComponentType())) {
          throw new CyclicDependenciesFoundException(visiting);
        }
        visiting.push(dependency.getComponentType());
        checkDependencies(dependency.component(), visiting);
        visiting.pop();
      }

    }

  }

}
