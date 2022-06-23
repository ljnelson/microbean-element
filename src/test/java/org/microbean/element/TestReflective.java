/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.element;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestReflective {

  private TestReflective() {
    super();
  }

  @Test
  final void testReflective() throws ReflectiveOperationException {
    AnnotatedType returnType = this.getClass().getDeclaredMethod("strings").getAnnotatedReturnType();
    assertTrue(returnType instanceof AnnotatedArrayType);
    returnType = this.getClass().getDeclaredMethod("number").getAnnotatedReturnType();
    assertNotNull(returnType);
    assertFalse(returnType instanceof AnnotatedArrayType);
    assertFalse(returnType instanceof AnnotatedParameterizedType);
    assertFalse(returnType instanceof AnnotatedTypeVariable);
    assertFalse(returnType instanceof AnnotatedWildcardType);
    assertTrue(returnType.getType() instanceof Class<?>);
  }

  private static final String[] strings() {
    return new String[0];
  }

  private static final @Gorp int number() {
    return 42;
  }

  @Target(ElementType.TYPE_USE)
  @interface Gorp {

  }
  
}
