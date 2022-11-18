package com.tdd.study;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

import jakarta.inject.Inject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
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
    List<Method> injectMethods = traverse(component, (methods, current) -> injectable(
        current.getDeclaredMethods())
            .filter(method -> isOverrideByInjectMethod(methods, method))
            .filter(method -> isOverrideByNoInjectMethod(component, method))
            .collect(Collectors.toList()));
    Collections.reverse(injectMethods);
    return injectMethods;
  }


  private static <Type> List<Field> getInjectFields(Class<Type> component) {
    return traverse(component, (fields, current) -> injectable(
        current.getDeclaredFields()).collect(Collectors.toList()));
  }

  private static <T> List<T> traverse(Class<?> component,
      BiFunction<List<T>, Class<?>, List<T>> finder) {
    List<T> members = new ArrayList<>();
    Class<?> current = component;
    while (current != Object.class) {
      members.addAll(finder.apply(members, current));
      current = current.getSuperclass();
    }
    return members;
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
      T instance = injectConstructor.newInstance(toDependencies(context, injectConstructor));
      for (Field field : injectFields) {
        field.set(instance, toDependency(context, field));
      }
      for (Method method : injectMethods) {
        method.invoke(instance, toDependencies(context, method));
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

  private static Object[] toDependencies(Context context, Executable executable) {
    return stream(executable.getParameters()).map(parameter -> {
      Type type = parameter.getParameterizedType();
      if (type instanceof ParameterizedType) return context.get((ParameterizedType) type).get();
      return context.get((Class<?>) type).get();
    }).toArray(Object[]::new);

  }

  private static Object toDependency(Context context, Field field) {
    Type type = field.getGenericType();
    if (type instanceof ParameterizedType) return context.get((ParameterizedType) type).get();
    return context.get((Class<?>)field.getType()).get();
  }
}
