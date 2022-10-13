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

import java.lang.annotation.Documented;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

  @AfterEach
  final void teardown() {
    this.reflection.clear();
  }

  @Test
  final void testCompilerViewOfDocumented(final ProcessingEnvironment env) {
    // This is all a little batty. java.lang.annotation.Documented
    // annotates itself.
    final TypeElement documentedElement = env.getElementUtils().getTypeElement("java.lang.annotation.Documented");
    assertSame(documentedElement,
               documentedElement.getAnnotationMirrors().get(0).getAnnotationType().asElement());
  }

  // @Disabled
  @Test
  final void testDocumented() throws IllegalAccessException, InvocationTargetException {
    // Nice edge case: java.lang.annotation.Documented annotates
    // itself.
    final DefaultTypeElement documented = this.reflection.elementStubFrom(Documented.class);
  }

  // @Disabled
  @Test
  final void testEnclosedElements() throws IllegalAccessException, InvocationTargetException {
    final DefaultTypeElement string = this.reflection.elementStubFrom(String.class);
    final List<? extends Element> elements = string.getEnclosedElements();
    System.out.println("*** elements.size(): " + elements.size());
    System.out.println("*** elements: " + elements);
    for (final Element e : elements) {
      assertSame(string, e.getEnclosingElement());
    }
  }

  @Test
  final void testEnclosedElementsCompilerViewpoint(final ProcessingEnvironment env) throws IllegalAccessException, InvocationTargetException {
    final TypeElement string = env.getElementUtils().getTypeElement("java.lang.String");
    final List<? extends Element> elements = string.getEnclosedElements();
    System.out.println("*** elements.size(): " + elements.size());
    System.out.println("*** elements: " + elements);
    for (final Element e : elements) {
      assertSame(string, e.getEnclosingElement());
    }
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
