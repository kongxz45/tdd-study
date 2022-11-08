package com.tdd.study;

import static java.util.Arrays.compare;
import static java.util.Arrays.stream;

import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConstructorInjectionProvider<T> implements ComponentProvider<T> {

  private Constructor<T> injectConstructor;

  private List<Field> injectFields;

  public ConstructorInjectionProvider(Class<T> implementation) {
    this.injectConstructor = getInjectConstructor(implementation);
    this.injectFields = getInjectFields(implementation);
  }

  private static <Type> List<Field> getInjectFields(Class<Type> implementation) {
    List<Field> injectFields = new ArrayList<>();
    Class<?> current = implementation;
    while (current != Object.class) {
      injectFields.addAll(stream(current.getDeclaredFields()).filter(
          field -> field.isAnnotationPresent(Inject.class)).collect(
          Collectors.toList()));
      current = current.getSuperclass();
    }
    return injectFields;
  }

  private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
    List<Constructor> injectConstructors = stream(implementation.getConstructors())
        .filter(constructor1 -> constructor1.isAnnotationPresent(
            Inject.class)).collect(Collectors.toList());
    if (injectConstructors.size() > 1) {
      throw new IllegalComponentException();
    }
    return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
      try {
        return implementation.getDeclaredConstructor(); //TODO need to know the difference between getConstructor() and getDeclaredConstructor()
      } catch (NoSuchMethodException e) {
        throw new IllegalComponentException();
      }
    });
  }


  @Override
  public T get(Context context) {

    try {
      Object[] dependencies = stream(injectConstructor.getParameters())
          .map(
              parameter -> context.get(parameter.getType())
                  .get())  // TODO： need to find out why .get() is needed
          .toArray(Object[]::new);
      T instance = injectConstructor.newInstance(dependencies);
      for (Field field : injectFields) {
        field.set(instance, context.get(field.getType()).get());
      }
      return instance;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Class<?>> getDependencies() {

    return Stream.concat(stream(injectConstructor.getParameters()).map(parameter -> parameter.getType()),
        injectFields.stream().map(field -> field.getType())).collect(
        Collectors.toList());
  }
}
