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

import javax.lang.model.element.Element;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class DeclaredTypeBuilder
  extends AbstractAnnotatedConstructBuilder<DeclaredType, DeclaredTypeBuilder> {

  private static final DeclaredType OBJECT = new DefaultDeclaredType();
  
  private TypeMirror enclosingType;
  
  private List<? extends TypeMirror> typeArguments;

  private Element definingElement;
  
  public DeclaredTypeBuilder() {
    super();
    this.enclosingType = new NoTypeBuilder().build();
  }

  public final Element definingElement() {
    return this.definingElement;
  }

  public final DeclaredTypeBuilder withDefiningElement(final Element definingElement) {
    this.definingElement = validateDefiningElement(definingElement);
    return self();
  }

  public final TypeMirror enclosingType() {
    return this.enclosingType;    
  }

  public final DeclaredTypeBuilder withEnclosingType(final TypeMirror enclosingType) {
    this.enclosingType = validateEnclosingType(enclosingType);
    return self();
  }
  
  public final List<? extends TypeMirror> typeArguments() {
    return this.typeArguments;
  }

  public final DeclaredTypeBuilder withTypeArguments(final List<? extends TypeMirror> typeArguments) {
    this.typeArguments = typeArguments == null || typeArguments.isEmpty() ? List.of() : List.copyOf(typeArguments);
    return self();
  }

  @Override // AbstractBuilder
  public final DeclaredType build() {
    return DefaultDeclaredType.of(this.enclosingType(), this.typeArguments(), this.annotations());
  }

  public static final DeclaredType object() {
    return OBJECT;
  }
  
  private static final Element validateDefiningElement(final Element definingElement) {
    switch (definingElement.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return definingElement;
    default:
      throw new IllegalArgumentException("definingElement: " + definingElement);
    }
  }
  
  private static final TypeMirror validateEnclosingType(final TypeMirror enclosingType) {
    switch (enclosingType.getKind()) {
    case DECLARED:
    case NONE:
      return enclosingType;
    default:
      throw new IllegalArgumentException("enclosingType: " + enclosingType);
    }
  }
  
}
