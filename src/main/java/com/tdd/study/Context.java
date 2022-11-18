package com.tdd.study;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

public interface Context {

  <Type> Optional<Type> get(Class<Type> type);

  <Type> Optional<Type> get(ParameterizedType type);
}
