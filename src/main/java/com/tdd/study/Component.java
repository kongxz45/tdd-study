package com.tdd.study;

import java.lang.annotation.Annotation;

public record Component(Class<?> type, Annotation qualifier) {

}
