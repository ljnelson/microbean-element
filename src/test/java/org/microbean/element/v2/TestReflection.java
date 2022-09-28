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

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AnnotationProcessingInterceptor.class)
final class TestReflection {

  private org.microbean.element.v2.Reflection reflection;

  private TestReflection() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.reflection = new Reflection();
  }

  @Test
  final void testReflection() throws IllegalAccessException, InvocationTargetException {
    final DefaultTypeElement string = this.reflection.elementStubFrom(String.class);
    assertTrue(string.getQualifiedName().contentEquals("java.lang.String"));
    assertSame(string, this.reflection.elementStubFrom(String.class));
    assertSame(string.asType(), this.reflection.typeStubFrom(String.class));
  }

  @Test
  final void testEquality(final ProcessingEnvironment env) throws IllegalAccessException, InvocationTargetException {
    final javax.lang.model.util.Elements elements = env.getElementUtils();
    final javax.lang.model.util.Types javacModelTypes = env.getTypeUtils();

    final TypeElement comparableElement = elements.getTypeElement("java.lang.Comparable");
    final TypeElement myComparableElement = this.reflection.elementStubFrom(Comparable.class);
    assertTrue(Equality.equalsIncludingAnnotations(comparableElement, myComparableElement));

    final TypeMirror comparableElementAsType = comparableElement.asType();
    final TypeMirror myComparableElementAsType = myComparableElement.asType();
    assertTrue(Equality.equalsIncludingAnnotations(comparableElementAsType, myComparableElementAsType));

    final DeclaredType comparableRawType = javacModelTypes.getDeclaredType(comparableElement);
    final DefaultDeclaredType myComparableRawType = new DefaultDeclaredType().definedBy(myComparableElement);
    assertTrue(Equality.equalsIncludingAnnotations(comparableRawType, myComparableRawType));

  }

}
