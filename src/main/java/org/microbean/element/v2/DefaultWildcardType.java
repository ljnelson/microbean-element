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

import java.util.List;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;

public final class DefaultWildcardType extends AbstractTypeMirror implements WildcardType {

  public static final DefaultWildcardType UNBOUNDED = new DefaultWildcardType(null, null, List.of());


  /*
   * Instance fields.
   */


  private final TypeMirror extendsBound;

  private final TypeMirror superBound;


  /*
   * Constructors.
   */


  public DefaultWildcardType(final TypeMirror extendsBound,
                             final TypeMirror superBound,
                             final List<? extends AnnotationMirror> annotationMirrors) {
    super(TypeKind.WILDCARD, annotationMirrors);
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


  /*
   * Instance methods.
   */


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


  /*
   * Static methods.
   */


  public static DefaultWildcardType upperBoundedWildcardType(final TypeMirror extendsBound) {
    return upperBoundedWildcardType(extendsBound, null);
  }

  public static DefaultWildcardType upperBoundedWildcardType(final TypeMirror extendsBound,
                                                             final List<? extends AnnotationMirror> annotationMirrors) {
    if (extendsBound == null && (annotationMirrors == null || annotationMirrors.isEmpty())) {
      return UNBOUNDED;
    }
    return new DefaultWildcardType(extendsBound, null, annotationMirrors);
  }

  public static DefaultWildcardType lowerBoundedWildcardType(final TypeMirror superBound) {
    return lowerBoundedWildcardType(superBound, null);
  }

  public static DefaultWildcardType lowerBoundedWildcardType(final TypeMirror superBound,
                                                             final List<? extends AnnotationMirror> annotationMirrors) {
    if (superBound == null && (annotationMirrors == null || annotationMirrors.isEmpty())) {
      return UNBOUNDED;
    }
    return new DefaultWildcardType(null, superBound, annotationMirrors);
  }

  public static DefaultWildcardType unboundedWildcardType() {
    return UNBOUNDED;
  }

  public static DefaultWildcardType unboundedWildcardType(final List<? extends AnnotationMirror> annotationMirrors) {
    return (annotationMirrors == null || annotationMirrors.isEmpty()) ? UNBOUNDED : new DefaultWildcardType(null, null, annotationMirrors);
  }

  public static DefaultWildcardType of(final TypeMirror extendsBound,
                                       final TypeMirror superBound,
                                       final List<? extends AnnotationMirror> annotationMirrors) {
    if (extendsBound == null) {
      if (superBound == null) {
        return unboundedWildcardType(annotationMirrors);
      } else {
        return lowerBoundedWildcardType(superBound, annotationMirrors);
      }
    } else if (superBound == null) {
      return upperBoundedWildcardType(extendsBound, annotationMirrors);
    } else {
      throw new IllegalArgumentException("extendsBound: " + extendsBound + "; superBound: " + superBound);
    }
  }

}
