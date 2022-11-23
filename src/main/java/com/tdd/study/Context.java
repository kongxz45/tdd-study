package com.tdd.study;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {

  Optional getType(Ref ref);

  class Ref {

    private Class<?> componentType;
    private Type containerType;

    public Ref(ParameterizedType container) {
      this.containerType = container.getRawType();
      this.componentType = (Class<?>) container.getActualTypeArguments()[0];
    }

    public Ref(Class<?> component) {
      this.componentType = component;
    }

    boolean isContainer() {
      return containerType != null;
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
