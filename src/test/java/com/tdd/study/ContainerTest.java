package com.tdd.study;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * TODO need to add all three type of injection and exception cases by @ParameterizedTest vs @ValueSource
 */

public class ContainerTest {

  @Nested
  public class DependenciesSelection {
  }

  @Nested
  public class LifeCycleManagement {
  }

}

interface Component {
}

interface Dependency {

}


class ComponentWithDefaultConstructor implements Component {

  public ComponentWithDefaultConstructor() {
  }
}

class ComponentWithInjectConstructor implements Component {

  private Dependency dependency;

  @Inject
  public ComponentWithInjectConstructor(Dependency dependency) {
    this.dependency = dependency;
  }

  public Dependency getDependency() {
    return dependency;
  }

}

class ComponentWithMultiInjectConstructors implements Component {

  @Inject
  public ComponentWithMultiInjectConstructors(String name, Integer age) {
  }

  @Inject
  public ComponentWithMultiInjectConstructors(String name) {
  }
}

class ComponentWithNoInjectNorDefaultConstructor implements Component {

  public ComponentWithNoInjectNorDefaultConstructor(String name) {
  }
}

class DependencyDependOnComponent implements Dependency {

  private Component component;

  @Inject
  public DependencyDependOnComponent(Component component) {
    this.component = component;
  }
}

class DependencyDependOnAnotherDependency implements Dependency {

  private AnotherDependency anotherDependency;

  @Inject
  public DependencyDependOnAnotherDependency(AnotherDependency anotherDependency) {
    this.anotherDependency = anotherDependency;
  }
}

interface AnotherDependency {

}

class AnotherDependencyDependOnComponent implements AnotherDependency {

  private Component component;

  @Inject
  public AnotherDependencyDependOnComponent(Component component) {
    this.component = component;
  }
}


