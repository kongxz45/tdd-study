package com.tdd.study;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

import jakarta.inject.Inject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InjectionProvider<T> implements ComponentProvider<T> {

  private Constructor<T> injectConstructor;

  private List<Field> injectFields;

  private List<Method> injectMethods;

  public InjectionProvider(Class<T> component) {
    if (Modifier.isAbstract(component.getModifiers())) {
      throw new IllegalComponentException();
    }
    this.injectConstructor = getInjectConstructor(component);
    this.injectFields = getInjectFields(component);
    this.injectMethods = getInjectMethods(component);
    if (injectFields.stream().anyMatch(field -> Modifier.isFinal(field.getModifiers()))) {
      throw new IllegalComponentException();
    }
    if (injectMethods.stream().anyMatch(method -> method.getTypeParameters().length != 0)) {
      throw new IllegalComponentException();
    }
  }

  private static <Type> List<Method> getInjectMethods(Class<Type> component) {
    List<Method> injectMethods = new ArrayList<>();
    Class<?> current = component;
    while (current != Object.class) {
      injectMethods.addAll(injectable(current.getDeclaredMethods())
          .filter(method -> isOverrideByInjectMethod(injectMethods, method))
          .filter(method -> isOverrideByNoInjectMethod(component, method))
          .collect(Collectors.toList()));
      current = current.getSuperclass();
    }
    Collections.reverse(injectMethods);
    return injectMethods;
  }


  private static <Type> List<Field> getInjectFields(Class<Type> component) {
    List<Field> injectFields = new ArrayList<>();
    Class<?> current = component;
    while (current != Object.class) {
      injectFields.addAll(injectable(current.getDeclaredFields()).collect(Collectors.toList()));
      current = current.getSuperclass();
    }
    return injectFields;
  }

  private static <Type> Constructor<Type> getInjectConstructor(Class<Type> component) {
    List<Constructor> injectConstructors = injectable(component.getConstructors()).collect(Collectors.toList());
    if (injectConstructors.size() > 1) {
      throw new IllegalComponentException();
    }
    return (Constructor<Type>) injectConstructors.stream().findFirst()
        .orElseGet(() -> getDefaultConstructor(component));
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

  private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredMethods) {
    return stream(declaredMethods)
        .filter(method -> method.isAnnotationPresent(Inject.class));
  }

  private static <Type> Constructor<Type> getDefaultConstructor(Class<Type> component) {
    try {
      return component.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new IllegalComponentException();
    }
  }

  private static Predicate<Method> isOverride(Method method) {
    return method1 -> method1.getName().equals(method.getName()) && Arrays.equals(
        method1.getParameterTypes(), method.getParameterTypes());
  }

  private static <Type> boolean isOverrideByNoInjectMethod(Class<Type> component, Method method) {
    return stream(component.getDeclaredMethods())
        .filter(method1 -> !method1.isAnnotationPresent(Inject.class))
        .noneMatch(isOverride(method));
  }

  private static boolean isOverrideByInjectMethod(List<Method> injectMethods, Method method) {
    return injectMethods.stream().noneMatch(isOverride(method));
  }

}
