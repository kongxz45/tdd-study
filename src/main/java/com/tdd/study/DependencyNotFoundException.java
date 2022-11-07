package com.tdd.study;

public class DependencyNotFoundException extends RuntimeException {

  private Class<?> component;
  private Class<?> dependency;

  public Class<?> getDependency() {
    return dependency;
  }

  public DependencyNotFoundException(Class<?> component, Class<?> dependency) {
    this.component = component;
    this.dependency = dependency;
  }

  public Class<?> getComponent() {
    return component;
  }
}
