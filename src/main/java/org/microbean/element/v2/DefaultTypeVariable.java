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
    this.upperBound = upperBound == null ? ObjectConstruct.JAVA_LANG_OBJECT_TYPE : upperBound;
    this.lowerBound = lowerBound;
  }

  final DefaultTypeVariable withUpperBound(final TypeMirror upperBound) {
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
  final TypeParameterElement validateDefiningElement(final TypeParameterElement e) {
    switch (e.getKind()) {
    case TYPE_PARAMETER:
      break;
    default:
      throw new IllegalArgumentException("e: " + e);
    }
    final TypeVariable elementType = (TypeVariable)e.asType();
    assert elementType != null : "null elementType for e: " + e;
    if (this != elementType) {
      // TODO: not clear whether this is a possible use case or not
      throw new IllegalArgumentException("e: " + e + "; this != e.asType()");
    }
    return e;
  }

  @Override // AbstractTypeVariable
  public <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitTypeVariable(this, p);
  }

  @Override // Object
  public final String toString() {
    return "extends " + this.getUpperBound();
  }


  /*
   * Static methods.
   */


  public static DefaultTypeVariable of(final TypeVariable tv) {
    if (tv instanceof DefaultTypeVariable dtv) {
      return dtv;
    }
    return new DefaultTypeVariable(tv.getUpperBound(), tv.getLowerBound(), tv.getAnnotationMirrors());
  }

  public static DefaultTypeVariable withUpperBound(final TypeVariable tv, final TypeMirror upperBound) {
    final DefaultTypeVariable r = new DefaultTypeVariable(upperBound, tv.getLowerBound(), tv.getAnnotationMirrors());
    r.setDefiningElement((TypeParameterElement)tv.asElement());
    assert r.asElement().asType() == tv;
    return r;
  }

}
