package com.tdd.study;

interface ScopeProvider<T> {

  ComponentProvider<T> create(ComponentProvider<?> provider);
}
