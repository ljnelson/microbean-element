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

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public abstract sealed class DefineableType<E extends Element> extends AbstractTypeMirror permits DefaultDeclaredType, DefaultTypeVariable, SyntheticCapturedType {

  private E definingElement;

  DefineableType(final TypeKind kind, final List<? extends AnnotationMirror> annotationMirrors) {
    super(validateKind(kind), annotationMirrors);
  }

  public final E asElement() {
    return this.definingElement;
  }

  public final void setDefiningElement(final E definingElement) {
    final E old = this.asElement();
    if (old == null) {
      this.definingElement = this.validateDefiningElement(Objects.requireNonNull(definingElement, "definingElement"));
    } else if (old != definingElement) {
      throw new IllegalStateException();
    }
  }

  final boolean defined() {
    return this.definingElement != null;
  }

  DefineableType<E> definedBy(final E definingElement) {
    this.setDefiningElement(definingElement);
    return this;
  }

  TypeMirror elementType() {
    final Element e = this.asElement();
    return e == null ? null : e.asType();
  }
  
  abstract E validateDefiningElement(final E e);

  private static final TypeKind validateKind(final TypeKind kind) {
    switch (kind) {
    case DECLARED:
    case ERROR:
    case TYPEVAR:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

}
