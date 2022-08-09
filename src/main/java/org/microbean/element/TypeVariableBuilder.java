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

import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public final class TypeVariableBuilder
  extends AbstractAnnotatedConstructBuilder<TypeVariable, TypeVariableBuilder> {

  private TypeMirror upperBound;

  private TypeMirror lowerBound;

  private TypeParameterElement definingElement;
  
  public TypeVariableBuilder() {
    super();
  }

  public final TypeMirror upperBound() {
    return this.upperBound;
  }

  public final TypeVariableBuilder withUpperBound(final TypeMirror upperBound) {
    this.upperBound = validateUpperBound(upperBound);
    return self();
  }

  public final TypeMirror lowerBound() {
    return this.lowerBound;
  }

  public final TypeVariableBuilder withLowerBound(final TypeMirror lowerBound) {
    this.lowerBound = validateLowerBound(lowerBound);
    return self();
  }

  public final TypeParameterElement definingElement() {
    return this.definingElement;
  }

  public final TypeVariableBuilder withDefiningElement(final TypeParameterElement definingElement) {
    this.definingElement = validateDefiningElement(definingElement);
    return self();
  }

  @Override // AbstractBuilder
  public final TypeVariable build() {
    final DefaultTypeVariable dtv = DefaultTypeVariable.of(this.upperBound(), this.lowerBound(), this.annotations());
    // dtv.element(this.definingElement()); // this is tricky
    return dtv;
  }

  private static final TypeParameterElement validateDefiningElement(final TypeParameterElement definingElement) {
    switch (definingElement.getKind()) {
    case TYPE_PARAMETER:
      return definingElement;
    default:
      throw new IllegalArgumentException("definingElement: " + definingElement);
    }
  }

  private static final TypeMirror validateUpperBound(final TypeMirror upperBound) {
    return upperBound;
  }

  private static final TypeMirror validateLowerBound(final TypeMirror lowerBound) {
    return lowerBound;
  }
  
}
