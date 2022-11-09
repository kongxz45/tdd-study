package com.tdd.study;

import java.util.Optional;

public interface Context {

  <Type> Optional<Type> get(Class<Type> type);

}
