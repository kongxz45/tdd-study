package com.tdd.study;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

import com.tdd.study.exception.IllegalComponentException;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InjectionProvider<T> implements ComponentProvider<T> {

  private List<Injectable<Field>> injectFields;

  private Injectable<Constructor<T>> injectConstructor;

  private List<Injectable<Method>> injectMethods;


  public InjectionProvider(Class<T> component) {
    if (Modifier.isAbstract(component.getModifiers())) {
      throw new IllegalComponentException();
    }
    this.injectConstructor = getInjectConstructor(component);

    this.injectMethods = getInjectMethods(component);

    this.injectFields = getInjectFields(component);

    if (injectFields.stream().map(fieldInjectable -> fieldInjectable.element)
        .anyMatch(field -> Modifier.isFinal(field.getModifiers()))) {
      throw new IllegalComponentException();
    }
    if (injectMethods.stream().map(methodInjectable -> methodInjectable.element)
        .anyMatch(method -> method.getTypeParameters().length != 0)) {
      throw new IllegalComponentException();
    }
  }

  @Override
  public T get(Context context) {
    try {

      T instance = injectConstructor.element.newInstance(injectConstructor.toDependencies(context));
      for (Injectable<Field> field : injectFields) {
        field.element.set(instance, field.toDependencies(context)[0]);
      }
      for (Injectable<Method> method : injectMethods) {
        method.element.invoke(instance, method.toDependencies(context));
      }
      return instance;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<ComponentRef<?>> getDependencies() {
    return concat(concat(Stream.of(injectConstructor), injectFields.stream()),
        injectMethods.stream()).flatMap(injectable -> stream(injectable.required)).toList();
  }




  private static <T> Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
    List<Constructor> injectConstructors = injectable(component.getConstructors()).collect(
        Collectors.toList());
    if (injectConstructors.size() > 1) {
      throw new IllegalComponentException();
    }
    return Injectable.of((Constructor<T>) injectConstructors.stream().findFirst()
        .orElseGet(() -> getDefaultConstructor(component)));
  }

  private static List<Injectable<Method>> getInjectMethods(Class<?> component) {
    List<Method> injectMethods = traverse(component, (methods, current) -> injectable(
        current.getDeclaredMethods())
        .filter(method1 -> isOverrideByInjectMethod(methods, method1))
        .filter(method1 -> isOverrideByNoInjectMethod(component, method1))
        .collect(Collectors.toList()));
    Collections.reverse(injectMethods);
    return injectMethods.stream().map(method -> Injectable.of(method)).toList();
  }

  private static List<Injectable<Field>> getInjectFields(Class<?> component) {
    return InjectionProvider.<Field>traverse(component, (fields, current) -> injectable(
            current.getDeclaredFields()).collect(Collectors.toList())).stream()
        .map(field -> Injectable.of(field)).toList();
  }

  private static <T extends AnnotatedElement> Stream<T> injectable(T[] elements) {
    return stream(elements)
        .filter(element -> element.isAnnotationPresent(Inject.class));
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


  static record Injectable<Element extends AccessibleObject>(Element element,
                                                             ComponentRef<?>[] required) {
    static <T extends Executable> Injectable<T> of(T constructor) {
      return new Injectable<>(constructor, stream(constructor.getParameters()).map(
          Injectable::toComponentRef).toArray(ComponentRef<?>[]::new));
    }

    static Injectable<Field> of(Field field) {
      return new Injectable<>(field, new ComponentRef<?>[]{toComponentRef(field)});
    }

    Object[] toDependencies(Context context) {
      return stream(required).map(context::get).map(Optional::get).toArray();
    }

    private static ComponentRef toComponentRef(Field field) {
      return ComponentRef.of(field.getGenericType(), getQualifier(field));
    }

    private static ComponentRef toComponentRef(Parameter parameter) {
      return ComponentRef.of(parameter.getParameterizedType(), getQualifier(parameter));
    }

    private static Annotation getQualifier(AnnotatedElement element) {
      List<Annotation> annotations = stream(element.getAnnotations()).filter(
          annotation -> annotation.annotationType().isAnnotationPresent(
              Qualifier.class)).collect(Collectors.toList());
      if (annotations.size() > 1) {
        throw new IllegalComponentException();
      }
      return annotations.stream().findFirst().orElse(null);
    }

  }

}
