package com.tdd.study;

import static java.util.Arrays.stream;

import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class ConstructorInjectionProvider<T> implements ComponentProvider<T> {

  private Constructor<T> constructor;

  public ConstructorInjectionProvider(Class<T> implementation) {
    List<Constructor> injectConstructors = stream(implementation.getConstructors())
        .filter(constructor1 -> constructor1.isAnnotationPresent(
            Inject.class)).collect(Collectors.toList());
    if (injectConstructors.size() > 1) {
      throw new IllegalComponentException();
    }
    this.constructor = (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> {
      try {
        return implementation.getConstructor();
      } catch (NoSuchMethodException e) {
        throw new IllegalComponentException();
      }
    });
  }


  @Override
  public T get(Context context) {

    try {
      Object[] dependencies = stream(constructor.getParameters())
          .map(
              parameter -> context.get(parameter.getType())
                  .get())  // TODO： need to find out why .get() is needed
          .toArray(Object[]::new);
      return (T) ((Constructor<?>) constructor).newInstance(dependencies);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Class<?>> getDependencies() {
    return stream(constructor.getParameters()).map(parameter -> parameter.getType()).collect(
        Collectors.toList());
  }
}
