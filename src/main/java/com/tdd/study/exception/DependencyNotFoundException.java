package com.tdd.study.exception;

import com.tdd.study.Component;

public class DependencyNotFoundException extends RuntimeException {

  private Component dependency;

  private Component component;


  public DependencyNotFoundException( Component component, Component dependency) {
    this.dependency = dependency;
    this.component = component;
  }

  public Component getDependency() {
    return dependency;
  }

  public Component getComponent() {
    return component;
  }
}
