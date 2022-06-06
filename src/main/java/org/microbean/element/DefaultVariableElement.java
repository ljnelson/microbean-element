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
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public final class DefaultVariableElement extends AbstractElement implements VariableElement {

  public final Object constantValue;

  public DefaultVariableElement(final Name simpleName,
                                final ElementKind kind,
                                final TypeMirror type,
                                final Set<? extends Modifier> modifiers,
                                final AbstractElement enclosingElement,
                                final Object constantValue,
                                final List<? extends AnnotationMirror> annotationMirrors) {
    super(simpleName,
          validate(kind),
          validate(type),
          modifiers,
          validate(enclosingElement),
          annotationMirrors);
    this.constantValue = constantValue;
  }

  @Override // AbstractElement
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitVariable(this, p);
  }

  @Override // VariableElement
  public final Object getConstantValue() {
    return this.constantValue;
  }
  
  private static final ElementKind validate(final ElementKind kind) {
    switch (kind) {
    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return kind;
    default:
      throw new IllegalArgumentException("Not a valid variable element kind: " + kind);
    }
  }

  private static final AbstractElement validate(final AbstractElement enclosingElement) {
    // TODO: implement
    return enclosingElement;
  }

  private static final TypeMirror validate(final TypeMirror type) {
    return type;
  }
  
}
