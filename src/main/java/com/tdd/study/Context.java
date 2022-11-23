package com.tdd.study;

import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {
  Optional getType(Type type);
}
