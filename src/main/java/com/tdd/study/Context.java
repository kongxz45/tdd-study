package com.tdd.study;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {

  <T> Optional<T> getType(Ref<T> ref);

  class Ref<T> {

    private Class<?> componentType;
    private Type containerType;

    public static <T> Ref<T> of(Class<T> component) {
      return new Ref(component);
    }

    public Ref(ParameterizedType container) {
      init(container);
    }

    public Ref(Class<T> component) {
      init(component);
    }

    boolean isContainer() {
      return containerType != null;
    }

    protected Ref() {
      Type type = ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
      init(type);
    }

    private void init(Type type) {
      if (type instanceof ParameterizedType) {
        this.containerType = ((ParameterizedType) type).getRawType();
        this.componentType = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
      } else {
        this.componentType = (Class<?>) type;
      }
    }

    static Ref of(Type type) {
      if (type instanceof ParameterizedType) {
        return new Ref((ParameterizedType) type);
      }
      return new Ref((Class<?>) type);
    }

    public Class<?> getComponentType() {
      return componentType;
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
      Ref ref = (Ref) o;
      return componentType.equals(ref.componentType) && Objects.equals(containerType,
          ref.containerType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(componentType, containerType);
    }
  }
}
