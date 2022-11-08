package com.tdd.study;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConstructorInjectionProvider<T> implements ComponentProvider<T> {

  private Constructor<T> injectConstructor;

  private List<Field> injectFields;

  private List<Method> injectMethods;

  public ConstructorInjectionProvider(Class<T> implementation) {
    if (Modifier.isAbstract(implementation.getModifiers())) throw new IllegalComponentException();
    this.injectConstructor = getInjectConstructor(implementation);
    this.injectFields = getInjectFields(implementation);
    this.injectMethods = getInjectMethods(implementation);
    if (injectFields.stream().anyMatch(field -> Modifier.isFinal(field.getModifiers()))) {
      throw new IllegalComponentException();
    }
    if (injectMethods.stream().anyMatch(method -> method.getTypeParameters().length != 0)) {
      throw new IllegalComponentException();
    }
  }

  private static <Type> List<Method> getInjectMethods(Class<Type> implementation) {
    List<Method> injectMethods = new ArrayList<>();
    Class<?> current = implementation;
    while (current != Object.class) {
      injectMethods.addAll(stream(current.getDeclaredMethods()).filter(
              method -> method.isAnnotationPresent(Inject.class))
              .filter(method -> injectMethods.stream().noneMatch(method1 -> method1.getName().equals(method.getName()) && Arrays.equals(method1.getParameterTypes(), method.getParameterTypes())))
              .filter(method -> stream(implementation.getDeclaredMethods()).filter(method1 -> !method1.isAnnotationPresent(Inject.class)).noneMatch(method1 -> method1.getName().equals(method.getName()) && Arrays.equals(method1.getParameterTypes(), method.getParameterTypes())))
          .collect(Collectors.toList()));
      current = current.getSuperclass();
    }
    Collections.reverse(injectMethods);
    return injectMethods;
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
      for (Method method : injectMethods) {
        method.invoke(instance, stream(method.getParameterTypes()).map(t -> context.get(t).get())
            .toArray(Object[]::new));
      }
      return instance;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Class<?>> getDependencies() {

    return concat(
        concat(stream(injectConstructor.getParameters()).map(parameter -> parameter.getType()),
            injectFields.stream().map(field -> field.getType())),
        injectMethods.stream().flatMap(method -> stream(method.getParameterTypes())))
        .collect(Collectors.toList());
  }
}
