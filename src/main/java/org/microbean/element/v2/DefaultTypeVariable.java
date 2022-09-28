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
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

public final class DefaultTypeVariable extends DefineableType<TypeParameterElement> implements TypeVariable {

  private final TypeMirror upperBound;

  private final TypeMirror lowerBound;

  public DefaultTypeVariable(final TypeMirror upperBound,
                             final TypeMirror lowerBound,
                             final List<? extends AnnotationMirror> annotationMirrors) {
    super(TypeKind.TYPEVAR, annotationMirrors);
    this.upperBound = validateUpperBound(Objects.requireNonNull(upperBound, "upperBound"));
    this.lowerBound = lowerBound == null ? null : validateLowerBound(lowerBound);
  }

  public final DefaultTypeVariable withUpperBound(final TypeMirror upperBound) {
    return withUpperBound(this, upperBound);
  }

  @Override // TypeVariable
  public final TypeMirror getLowerBound() {
    return this.lowerBound;
  }

  @Override // TypeVariable
  public final TypeMirror getUpperBound() {
    return this.upperBound;
  }

  @Override // DefineableType<TypeParameterElement>
  final DefaultTypeVariable definedBy(final TypeParameterElement e) {
    return (DefaultTypeVariable)super.definedBy(e);
  }
  
  @Override // DefineableType<TypeParameterElement>
  final TypeParameterElement validateDefiningElement(final TypeParameterElement e) {
    if (e.getKind() != ElementKind.TYPE_PARAMETER) {
      throw new IllegalArgumentException("e: " + e);
    } else if (this != e.asType()) {
      throw new IllegalArgumentException("e: " + e + "; this (" + this + ") != e.asType() (" + e.asType() + ")");
    }
    return e;
  }

  @Override // AbstractTypeVariable
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitTypeVariable(this, p);
  }

  @Override // Object
  public final String toString() {
    return super.toString() + " extends " + this.getUpperBound();
  }


  /*
   * Static methods.
   */


  public static final DefaultTypeVariable of(final TypeVariable tv) {
    if (tv instanceof DefaultTypeVariable dtv) {
      return dtv;
    }
    final DefaultTypeVariable r = new DefaultTypeVariable(tv.getUpperBound(), tv.getLowerBound(), tv.getAnnotationMirrors());
    r.setDefiningElement((TypeParameterElement)tv.asElement());
    return r;
  }

  public static final DefaultTypeVariable withUpperBound(final TypeVariable tv, final TypeMirror upperBound) {
    final DefaultTypeVariable r = new DefaultTypeVariable(upperBound, tv.getLowerBound(), tv.getAnnotationMirrors());
    r.setDefiningElement((TypeParameterElement)tv.asElement());
    return r;
  }

  private static final TypeMirror validateUpperBound(final TypeMirror upperBound) {
    switch (upperBound.getKind()) {
    case DECLARED:
    case INTERSECTION:
    case TYPEVAR:
      return upperBound;
    default:
      throw new IllegalArgumentException("upperBound: " + upperBound);
    }
  }

  private static final TypeMirror validateLowerBound(final TypeMirror lowerBound) {
    return lowerBound;
  }

}
