package com.tdd.study.exception;

import com.tdd.study.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CyclicDependenciesFoundException extends RuntimeException {

  private List<Component> components = new ArrayList<>();

  public CyclicDependenciesFoundException(List<Component> visiting) {
    this.components.addAll(visiting);
  }

  public List<Class<?>> getComponents() {
    return components.stream().map(component -> component.type()).collect(Collectors.toList());
  }
}
