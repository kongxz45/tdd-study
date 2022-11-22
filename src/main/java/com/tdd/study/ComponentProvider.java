package com.tdd.study;

import static java.util.List.of;

import java.lang.reflect.Type;
import java.util.List;

interface ComponentProvider<T> {

  T get(Context context);

  default List<Class<?>> getDependencies() {
    return of();
  }

  default List<Type> getDependencyTypes() {
    return of();
  }
}
