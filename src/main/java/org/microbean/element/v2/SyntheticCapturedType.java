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
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;

final class SyntheticCapturedType extends DefineableType<TypeParameterElement> implements TypeVariable {

  private TypeMirror upperBound;

  private TypeMirror lowerBound;
  
  private TypeParameterElement definingElement;
  
  private final WildcardType wildcardType;

  SyntheticCapturedType(final WildcardType wildcardType) {
    this(AnnotatedName.of(DefaultName.of("<captured wildcard>")), wildcardType);
  }
  
  SyntheticCapturedType(final AnnotatedName name, final WildcardType wildcardType) {
    super(TypeKind.TYPEVAR, name.getAnnotationMirrors());
    this.wildcardType = wildcardType;
    this.setDefiningElement(new DefaultTypeParameterElement(name, this, Set.of()));
  }

  @Override // DefineableType<TypeParameterElement>
  final TypeParameterElement validateDefiningElement(final TypeParameterElement e) {
    if (e.asType() != this || e.getKind() != ElementKind.TYPE_PARAMETER) {
      throw new IllegalArgumentException("e: " + e);
    }
    return e;
  }
  
  @Override // AbstractTypeVariable
  public final <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitTypeVariable(this, p);
  }

  @Override // TypeVariable
  public final TypeMirror getLowerBound() {
    return this.lowerBound;
  }

  public final void setLowerBound(final TypeMirror lowerBound) {
    this.lowerBound = lowerBound;
  }

  @Override // TypeVariable
  public final TypeMirror getUpperBound() {
    return this.upperBound;
  }

  public final void setUpperBound(final TypeMirror upperBound) {
    this.upperBound = upperBound;
  }
  
  public final WildcardType getWildcardType() {
    return this.wildcardType;
  }

}
