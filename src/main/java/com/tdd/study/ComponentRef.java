package com.tdd.study;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<T> {
  private Component component;

  private Type containerType;

  public static <T> ComponentRef<T> of(Class<T> componentType) {
    return new ComponentRef(componentType);
  }

  public static <T> ComponentRef<T> of(Class<T> componentType, Annotation qualifier) {
    return new ComponentRef(componentType, qualifier);
  }

  public ComponentRef(ParameterizedType containerType) {
    init(containerType, null);
  }

  public ComponentRef(Class<T> componentType) {
    init(componentType, null);
  }

  public ComponentRef(Class<T> componentType, Annotation qualifier) {
    init(componentType, qualifier);
  }


  boolean isContainer() {
    return containerType != null;
  }

  protected ComponentRef() {
    Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    init(type, null);
  }

  private void init(Type type, Annotation qualifier) {
    if (type instanceof ParameterizedType) {
      this.containerType = ((ParameterizedType) type).getRawType();
      this.component = new Component((Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0], qualifier);
    } else {
      this.component = new Component((Class<?>) type, qualifier);
    }
  }

  public Component component() {
    return component;
  }

  static ComponentRef of(Type type) {
    if (type instanceof ParameterizedType) {
      return new ComponentRef((ParameterizedType) type);
    }
    return new ComponentRef((Class<?>) type);
  }

  public Class<?> getComponentType() {
    return component.type();
  }

  public Type getContainerType() {
    return containerType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentRef<?> that = (ComponentRef<?>) o;
    return component.equals(that.component) && Objects.equals(containerType,
        that.containerType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(component, containerType);
  }
}
