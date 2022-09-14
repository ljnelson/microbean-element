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
package org.microbean.element.v2;

import java.lang.reflect.InvocationTargetException;

import javax.lang.model.type.DeclaredType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class TestReflection {

  private Reflection reflection;
  
  private TestReflection() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.reflection = new Reflection();
  }

  @Test
  final void testReflection() throws IllegalAccessException, InvocationTargetException {
    final DefaultTypeElement string = reflection.elementStubFrom(String.class);
    assertTrue(string.getQualifiedName().contentEquals("java.lang.String"));
    assertSame(string, reflection.elementStubFrom(String.class));
    assertSame(string.asType(), reflection.typeStubFrom(String.class));    
  }

  @Test
  final void testReflectionOnClassWithTypeParameters() throws IllegalAccessException, InvocationTargetException {
    final DefaultTypeElement c = reflection.elementStubFrom(Class.class);
  }

}
