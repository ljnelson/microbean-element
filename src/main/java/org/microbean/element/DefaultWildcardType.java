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
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.List;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;

public class DefaultWildcardType extends AbstractTypeMirror implements WildcardType {

  public static final DefaultWildcardType UNBOUNDED = new DefaultWildcardType(null, null, List::of);
  
  private final TypeMirror extendsBound;

  private final TypeMirror superBound;
  
  protected DefaultWildcardType(final TypeMirror extendsBound,
                                final TypeMirror superBound,
                                final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(TypeKind.WILDCARD, annotationMirrorsSupplier);
    if (extendsBound == null) {
      this.extendsBound = null;
    } else {
      switch (extendsBound.getKind()) {
        // See
        // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-WildcardBounds
        // and
        // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-ReferenceType
      case ARRAY:
      case DECLARED:
      case TYPEVAR:
        this.extendsBound = extendsBound;
        break;
      default:
        throw new IllegalArgumentException("extendsBound: " + extendsBound);
      }
    }
    if (superBound == null) {
      this.superBound = null;
    } else {
      switch (superBound.getKind()) {
      // See
      // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-WildcardBounds
      // and
      // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-ReferenceType
      case ARRAY:
      case DECLARED:
      case TYPEVAR:
        this.superBound = superBound;
        break;
      default:
        throw new IllegalArgumentException("superBound: " + superBound);
      }
    }
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitWildcard(this, p);
  }
  
  @Override // WildcardType
  public final TypeMirror getExtendsBound() {
    return this.extendsBound;
  }

  @Override // WildcardType
  public final TypeMirror getSuperBound() {
    return this.superBound;
  }

  public static DefaultWildcardType upperBoundedWildcardType(final TypeMirror extendsBound) {
    return upperBoundedWildcardType(extendsBound, null);
  }
  
  public static DefaultWildcardType upperBoundedWildcardType(final TypeMirror extendsBound,
                                                             final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    if (extendsBound == null && annotationMirrorsSupplier == null) {
      return UNBOUNDED;
    }
    return new DefaultWildcardType(extendsBound, null, annotationMirrorsSupplier);
  }

  public static DefaultWildcardType lowerBoundedWildcardType(final TypeMirror superBound) {
    return lowerBoundedWildcardType(superBound, null);
  }
  
  public static DefaultWildcardType lowerBoundedWildcardType(final TypeMirror superBound,
                                                             final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    if (superBound == null && annotationMirrorsSupplier == null) {
      return UNBOUNDED;
    }
    return new DefaultWildcardType(null, superBound, annotationMirrorsSupplier);
  }

  public static DefaultWildcardType unboundedWildcardType() {
    return UNBOUNDED;
  }
  
  public static DefaultWildcardType unboundedWildcardType(final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    return annotationMirrorsSupplier == null ? UNBOUNDED : new DefaultWildcardType(null, null, annotationMirrorsSupplier);
  }

  public static DefaultWildcardType of(final AnnotatedWildcardType w) {
    final AnnotatedType[] lowerBounds = w.getAnnotatedLowerBounds();
    if (lowerBounds.length > 0) {
      return lowerBoundedWildcardType(AbstractTypeMirror.of(lowerBounds[0]));
    } else {
      final AnnotatedType[] upperBounds = w.getAnnotatedUpperBounds();
      final AnnotatedType soleUpperBound = upperBounds[0];
      if (soleUpperBound.getType() == Object.class) {
        // Unbounded.
        return unboundedWildcardType(); // TODO: annotations
      } else {
        return upperBoundedWildcardType(AbstractTypeMirror.of(soleUpperBound));
      }
    }
  }
  
}
