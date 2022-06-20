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
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;

public class DefaultCapturedType extends AbstractTypeVariable implements CapturedType {

  private final DefaultTypeParameterElement definingElement;
  
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
  // TypeVariableSymbol --> DefaultTYpeParameterElement

  public DefaultCapturedType(final WildcardType wildcardType) {
    this(DefaultName.of("<captured wildcard>"), wildcardType);
  }
  
  public DefaultCapturedType(final Name name, final WildcardType wildcardType) {
    super(TypeKind.TYPEVAR,
          wildcardType.getExtendsBound() == null ? DefaultTypeElement.JAVA_LANG_OBJECT.asType() : wildcardType.getExtendsBound(),
          null,
          null);
    this.wildcardType = wildcardType;
    this.definingElement = new DefaultTypeParameterElement(name, this, Set.of(), null, null);
  }
  
  @Override // TypeVariable
  public final DefaultTypeParameterElement asElement() {
    return this.definingElement;
  }
  
  @Override // AbstractTypeVariable
  public <R, P> R accept(final TypeVisitor<R, P> v, final P p) {
    return v.visitTypeVariable(this, p);
  }
  
  @Override // CapturedType
  public final WildcardType getWildcardType() {
    return this.wildcardType;
  }

}
