package com.tdd.study;

import org.junit.jupiter.api.Nested;

public class ContainerTest {

  @Nested
  public class DependenciesSelection {
  }

  @Nested
  public class LifeCycleManagement {
  }

}

interface TestComponent {
  default Object getDependency() {
    return null;
  }
}

interface Dependency {

}









