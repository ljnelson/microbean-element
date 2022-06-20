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

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

public abstract class AbstractTypeVariable extends AbstractReferenceType implements TypeVariable {

  private TypeMirror lowerBound;

  private TypeMirror upperBound;

  protected AbstractTypeVariable(final TypeKind kind,
                                 final TypeMirror upperBound,
                                 final TypeMirror lowerBound, // only relevant for capture conversion
                                 final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(kind, annotationMirrorsSupplier);
    this.lowerBound = lowerBound == null ? DefaultNullType.INSTANCE : lowerBound;
    this.upperBound = upperBound == null ? DefaultDeclaredType.JAVA_LANG_OBJECT : upperBound;
  }

  @Override // TypeMirror
  public abstract <R, P> R accept(final TypeVisitor<R, P> v, final P p);

  @Override // TypeVariable
  public final TypeMirror getLowerBound() {
    return this.lowerBound;
  }

  public final void setLowerBound(final TypeMirror type) {
    if (type == null) {
      this.lowerBound = DefaultNullType.INSTANCE;
    } else if (type == this) {
      throw new IllegalArgumentException();
    } else {
      this.lowerBound = type;
    }
  }

  @Override // TypeVariable
  public final TypeMirror getUpperBound() {
    return this.upperBound;
  }

  public final void setUpperBound(final TypeMirror type) {
    if (type == null) {
      this.upperBound = DefaultDeclaredType.JAVA_LANG_OBJECT;
    } else if (type == this) {
      throw new IllegalArgumentException();
    } else {
      this.upperBound = type;
    }
  }
  
}

