package com.tdd.study.exception;

import java.util.ArrayList;
import java.util.List;

public class CyclicDependenciesFoundException extends RuntimeException {

  private List<Class<?>> components = new ArrayList<>();

  public CyclicDependenciesFoundException(Class<?> component) {
    this.components.add(component);
  }

  public CyclicDependenciesFoundException(Class<?> componentType, CyclicDependenciesFoundException e) {
    this.components.add(componentType);
    this.components.addAll(e.getComponents());
  }

  public CyclicDependenciesFoundException(List<Class<?>> visiting) {
    this.components.addAll(visiting);
  }

  public List<Class<?>> getComponents() {
    return components;
  }
}