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

import java.util.List;

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
                                final List<? extends AnnotationMirror> annotationMirrors) {
    super(TypeKind.TYPEVAR, upperBound, lowerBound, annotationMirrors);
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

  public static DefaultTypeVariable of(final TypeMirror upperBound) {
    return of(upperBound, List.of());
  }
  
  public static DefaultTypeVariable of(final TypeMirror upperBound,
                                       final List<? extends AnnotationMirror> annotationMirrors) {
    return new DefaultTypeVariable(upperBound, null, annotationMirrors);
  }

}
