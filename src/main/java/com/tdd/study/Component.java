package com.tdd.study;

import java.lang.annotation.Annotation;
import java.util.Objects;

public record Component(Class<?> type, Annotation qualifier) {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Component component = (Component) o;
    return type.equals(component.type) && Objects.equals(qualifier, component.qualifier);
  }

}
