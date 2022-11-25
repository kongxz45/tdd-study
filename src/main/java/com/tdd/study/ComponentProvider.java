package com.tdd.study;

import static java.util.List.of;

import java.util.List;

interface ComponentProvider<T> {

  T get(Context context);

  default List<ComponentRef> getDependencies() {
    return of();
  };
}
