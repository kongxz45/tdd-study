package com.tdd.study;

import static java.util.Arrays.stream;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Context {

  Map<Class<?>, Provider<?>> providers = new HashMap<>();

  public <type> void bind(Class<type> type, type instance) {
    providers.put(type, (Provider<type>) () -> instance);
  }

  public <Type, Implementation extends Type> void bind(Class<Type> type,
      Class<Implementation> implementation) {
    Constructor<?> constructor = getConstructor(implementation);
    providers.put(type, (Provider<Type>) () -> {
      Object[] dependencies = stream(constructor.getParameters())
          .map(
              parameter -> get(parameter.getType()).orElseThrow(() ->new DependencyNotFoundException())).toArray(Object[]::new);
      try {
        return (Type) constructor.newInstance(dependencies);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private static <Type> Constructor<Type> getConstructor(Class<Type> implementation) {
    List<Constructor> injectConstructors = stream(implementation.getConstructors())
        .filter(constructor -> constructor.isAnnotationPresent(
            Inject.class)).collect(Collectors.toList());
    if (injectConstructors.size() > 1) throw new IllegalComponentException();
    return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
      try {
        return implementation.getConstructor();
      } catch (NoSuchMethodException e) {
        throw new IllegalComponentException();
      }
    });

  }

  public <Type> Optional<Type> get(Class<Type> type) {
    return Optional.ofNullable(providers.get(type)).map(provider -> (Type)provider.get());
  }
}
