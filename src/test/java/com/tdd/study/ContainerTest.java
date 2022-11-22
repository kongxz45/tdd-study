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
  default Object getDependency() {
    return null;
  }
}

interface Dependency {

}









