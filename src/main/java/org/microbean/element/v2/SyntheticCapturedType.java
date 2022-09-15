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

final class SyntheticCapturedType extends AbstractTypeMirror implements DefineableType<TypeParameterElement>, TypeVariable {

  private TypeMirror upperBound;

  private TypeMirror lowerBound;
  
  private TypeParameterElement definingElement;
  
  private final WildcardType wildcardType;

  // In the compiler (javac):
  //
  // constructor takes a Name, a Symbol, a Type for upper, a Type for
  // lower and the Wildcard
  // (https://github.com/openjdk/jdk/blob/41daa88dcc89e509f21d1685c436874d6479cf62/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1729-L1733).
  //
  // new CapturedType(name, owner /* Symbol */, upper, lower, wildcard)
  //
  // calls TypeVar constructor and assembles what we have here as an Element:
  //
  // new TypeVariableSymbol(0 /* flags */, name, this /* type */, owner /* Symbol */);
  //
  // TypeVariableSymbol --> DefaultTypeParameterElement

  SyntheticCapturedType(final WildcardType wildcardType) {
    this(AnnotatedName.of(DefaultName.of("<captured wildcard>")), wildcardType);
  }
  
  SyntheticCapturedType(final AnnotatedName name, final WildcardType wildcardType) {
    super(TypeKind.TYPEVAR, List.of());    
    this.wildcardType = wildcardType;
    this.setDefiningElement(new DefaultTypeParameterElement(name, this, Set.of()));
  }
  
  @Override // TypeVariable
  public final TypeParameterElement asElement() {
    return this.definingElement;
  }

  @Override
  public final void setDefiningElement(final TypeParameterElement e) {
    if (this.definingElement != null) {
      throw new IllegalStateException();
    } else if (e != null &&
               (e.asType() != this ||
                e.getKind() != ElementKind.TYPE_PARAMETER)) {
      throw new IllegalArgumentException("e: " + e);
    }
    this.definingElement = e;
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
