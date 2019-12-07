// Copyright 2017 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that describes the java equivalent of a block.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Block {
  Class[] classes() default {};

  String[] methodName() default {};

  String[] fieldName() default {};

  boolean constructor() default false;

  boolean exclusiveToBlocks() default false;
}
