package com.tdd.study;

import java.util.List;

interface ComponentProvider<T> {

  T get(Context context);

  List<Class<?>> getDependencies();
}
