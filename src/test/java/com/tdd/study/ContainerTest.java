package com.tdd.study;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Nested;

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









