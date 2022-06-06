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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

public class DefaultDeclaredType extends AbstractReferenceType implements DeclaredType {

  static final DefaultDeclaredType JAVA_LANG_OBJECT = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_IO_SERIALIZABLE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_CLONEABLE = new DefaultDeclaredType();
  
  private final TypeMirror enclosingType;

  private Element definingElement;

  private final List<? extends TypeMirror> typeArguments;

  public DefaultDeclaredType() {
    this(DefaultNoType.NONE, List.of(), List.of());
  }
  
  public DefaultDeclaredType(final List<? extends TypeMirror> typeArguments,
                             final List<? extends AnnotationMirror> annotationMirrors) {
    this(DefaultNoType.NONE, typeArguments, annotationMirrors);
  }

  public DefaultDeclaredType(final TypeMirror enclosingType,
                             final List<? extends TypeMirror> typeArguments,
                             final List<? extends AnnotationMirror> annotationMirrors) {
    super(TypeKind.DECLARED, annotationMirrors);
    if (enclosingType == null) {
      this.enclosingType = DefaultNoType.NONE;
    } else {
      switch (enclosingType.getKind()) {
      case DECLARED:
      case NONE:
        this.enclosingType = enclosingType;
        break;
      default:
        throw new IllegalArgumentException("enclosingType: " + enclosingType);
      }
    }
    this.typeArguments = typeArguments == null || typeArguments.isEmpty() ? List.of() : List.copyOf(typeArguments);
  }

  final void element(final Element e) {
    if (this.asElement() != null) {
      throw new IllegalStateException();
    } else if (e.asType() != this) {
      // Wildcard capture makes this impossible.
      // throw new IllegalArgumentException("e: " + e);
    }
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      this.definingElement = e;
      break;
    default:
      throw new IllegalArgumentException("e: " + e);
    }
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitDeclared(this, p);
  }
  
  @Override // DeclaredType
  public final Element asElement() {
    return this.definingElement;
  }

  @Override // DeclaredType
  public final TypeMirror getEnclosingType() {
    return this.enclosingType;
  }

  @Override // DeclaredType
  public final List<? extends TypeMirror> getTypeArguments() {
    return this.typeArguments;
  }

}
