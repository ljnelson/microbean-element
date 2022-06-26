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

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.GenericArrayType;

import java.util.ArrayList;
import java.util.List;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

public class DefaultTypeVariable extends AbstractTypeVariable {

  private TypeParameterElement definingElement;

  protected DefaultTypeVariable(final TypeMirror upperBound,
                                final TypeMirror lowerBound,
                                final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(TypeKind.TYPEVAR, upperBound, lowerBound, annotationMirrorsSupplier);
  }
  
  final void element(final TypeParameterElement e) {
    if (this.asElement() != null) {
      throw new IllegalStateException();
    } else if (e.asType() != this || e.getKind() != ElementKind.TYPE_PARAMETER) {
      throw new IllegalArgumentException("e: " + e);
    }
    this.definingElement = e;
  }
  
  @Override // AbstractTypeVariable
  public <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitTypeVariable(this, p);
  }

  @Override // TypeVariable
  public final TypeParameterElement asElement() {
    return this.definingElement;
  }

  public static DefaultTypeVariable of() {
    return of(null, null);
  }
  
  public static DefaultTypeVariable of(final TypeMirror upperBound) {
    return of(upperBound, null);
  }
  
  public static DefaultTypeVariable of(final TypeMirror upperBound,
                                       final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    return new DefaultTypeVariable(upperBound, null, annotationMirrorsSupplier);
  }

  public static DefaultTypeVariable of(final TypeMirror upperBound,
                                       final TypeMirror lowerBound,
                                       final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    return new DefaultTypeVariable(upperBound, lowerBound, annotationMirrorsSupplier);
  }

  // Weirdly, nothing in the JDK actually uses AnnotatedTypeVariable.
  public static DefaultTypeVariable of(final AnnotatedTypeVariable tv) {
    final AnnotatedType[] bounds = tv.getAnnotatedBounds();
    // If a java.lang.reflect.TypeVariable has a
    // java.lang.reflect.TypeVariable as its first bound, it is
    // required that this first bound be its only bound.
    switch (bounds.length) {
    case 0:
      return DefaultTypeVariable.of();
    case 1:
      // Class, interface, or type variable
      final AnnotatedType soleBound = bounds[0];
      if (soleBound instanceof AnnotatedTypeVariable tvBound) {
        return DefaultTypeVariable.of(DefaultTypeVariable.of(tvBound));
      }
      return DefaultTypeVariable.of(DefaultDeclaredType.of(soleBound));
    default:
      final List<TypeMirror> intersectionTypeBounds = new ArrayList<>(bounds.length);
      for (final AnnotatedType bound : bounds) {
        intersectionTypeBounds.add(AbstractTypeMirror.of(bound));
      }
      return DefaultTypeVariable.of(DefaultIntersectionType.of(intersectionTypeBounds));
    }
  }

  public static DefaultTypeVariable of(final java.lang.reflect.TypeVariable<?> tv) {
    final AnnotatedType[] bounds = tv.getAnnotatedBounds();
    // If a java.lang.reflect.TypeVariable has a
    // java.lang.reflect.TypeVariable as its first bound, it is
    // required that this first bound be its only bound.
    switch (bounds.length) {
    case 0:
      return DefaultTypeVariable.of();
    case 1:
      // Class, interface, or type variable
      final AnnotatedType soleBound = bounds[0];
      if (soleBound instanceof AnnotatedTypeVariable tvBound) {
        return DefaultTypeVariable.of(DefaultTypeVariable.of(tvBound));
      }
      return DefaultTypeVariable.of(DefaultDeclaredType.of(soleBound));
    default:
      final List<TypeMirror> intersectionTypeBounds = new ArrayList<>(bounds.length);
      for (final AnnotatedType bound : bounds) {
        intersectionTypeBounds.add(AbstractTypeMirror.of(bound));
      }
      return DefaultTypeVariable.of(DefaultIntersectionType.of(intersectionTypeBounds));
    }
  }

}
