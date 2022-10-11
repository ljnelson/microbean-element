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

import java.util.ArrayList;
import java.util.Collections;
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

public final class DefaultVariableElement extends AbstractElement implements VariableElement {

  private final List<? extends AnnotationMirror> annotationMirrors;
  
  public final Object constantValue;

  public DefaultVariableElement(final Name simpleName,
                                final List<? extends AnnotationMirror> annotationMirrors,
                                final ElementKind kind,
                                final TypeMirror type,
                                final Set<? extends Modifier> modifiers,
                                final Element enclosingElement,
                                final Object constantValue) {
    super(simpleName,
          validateKind(kind),
          validateType(type),
          modifiers,
          enclosingElement,
          List.of());
    if (annotationMirrors == null) {
      this.annotationMirrors = List.of();
    } else if (annotationMirrors instanceof DeferredList<? extends AnnotationMirror>) {
      this.annotationMirrors = annotationMirrors;
    } else {
      this.annotationMirrors = List.copyOf(annotationMirrors);
    }
    this.constantValue = constantValue;
  }

  @Override // AbstractElement
  public final <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitVariable(this, p);
  }

  @Override
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.annotationMirrors;
  }

  @Override // VariableElement
  public final Object getConstantValue() {
    return this.constantValue;
  }


  /*
   * Static methods.
   */


  private static final ElementKind validateKind(final ElementKind kind) {
    switch (kind) {
    case BINDING_VARIABLE:
    case ENUM:
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

  private static final TypeMirror validateType(final TypeMirror type) {
    switch (type.getKind()) {
    case ARRAY:
    case DECLARED:
    case INTERSECTION:
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
    case TYPEVAR:
    case WILDCARD:
      return type;
    default:
      throw new IllegalArgumentException("type: " + type);
    }
  }

  @SuppressWarnings("unchecked")
  public static final <E extends VariableElement & Encloseable> List<? extends E> encloseableParametersOf(final List<? extends VariableElement> list) {
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    final List<E> newList = new ArrayList<>(list.size());
    for (final VariableElement e : list) {
      if (e instanceof Encloseable enc) {
        newList.add((E)enc);
      } else {
        newList.add((E)DefaultVariableElement.of(e));
      }
    }
    return Collections.unmodifiableList(newList);
  }

  
  public static final DefaultVariableElement of(final VariableElement e) {
    if (e instanceof DefaultVariableElement defaultVariableElement) {
      return defaultVariableElement;
    }
    return
      new DefaultVariableElement(e.getSimpleName(),
                                 e.getAnnotationMirrors(),
                                 e.getKind(),
                                 e.asType(),
                                 e.getModifiers(),
                                 e.getEnclosingElement(),
                                 e.getConstantValue());
  }

}
