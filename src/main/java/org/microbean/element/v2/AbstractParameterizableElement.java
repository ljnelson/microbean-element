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
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

public abstract sealed class AbstractParameterizableElement extends AbstractElement implements Parameterizable permits DefaultExecutableElement, DefaultTypeElement {

  private final List<? extends TypeParameterElement> typeParameters;

  protected
    <T extends Element & Encloseable, P extends TypeParameterElement & Encloseable>
    AbstractParameterizableElement(final AnnotatedName name,
                                   final ElementKind kind,
                                   final TypeMirror type, // DeclaredType or ExecutableType
                                   final Set<? extends Modifier> modifiers,
                                   final Element enclosingElement, // ExecutableElement or TypeElement
                                   final List<? extends T> enclosedElements,
                                   final List<? extends P> typeParameters) {
    super(name,
          kind, // no need to validate; sealed class; subclasses already validate
          type, // no need to validate; sealed class; subclasses already validate
          modifiers,
          validateEnclosingElement(enclosingElement),
          enclosedElements);
    // assert (type instanceof ExecutableType) || (type instanceof DeclaredType);
    if (typeParameters == null || typeParameters.isEmpty()) {
      this.typeParameters = List.of();
      if (type instanceof ExecutableType et) {
        if (!et.getTypeVariables().isEmpty()) {
          throw new IllegalArgumentException("type: " + type);
        }
      } else {
        if (!((DeclaredType)type).getTypeArguments().isEmpty()) {
          throw new IllegalArgumentException("type: " + type);
        }
      }
    } else {
      final List<P> tps = List.copyOf(typeParameters);
      for (final P tp : tps) {
        if (tp.getKind() != ElementKind.TYPE_PARAMETER) {
          throw new IllegalArgumentException("typeParameters: " + typeParameters);
        }
        tp.setEnclosingElement(this);
      }
      this.typeParameters = tps;
    }
  }

  @Override // Parameterizable
  public List<? extends TypeParameterElement> getTypeParameters() {
    return this.typeParameters;
  }

  private static final ElementKind validateKind(final ElementKind kind) {
    switch (kind) {
    case ANNOTATION_TYPE:
    case CLASS:
    case CONSTRUCTOR:
    case ENUM:
    case INSTANCE_INIT:
    case INTERFACE:
    case METHOD:
    case RECORD:
    case STATIC_INIT:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);

    }
  }
  
  private static final Element validateEnclosingElement(final Element enclosingElement) {
    if (enclosingElement == null) {
      return null;
    }
    switch (enclosingElement.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case CONSTRUCTOR:
    case ENUM:
    case INSTANCE_INIT:
    case INTERFACE:
    case METHOD:
    case RECORD:
    case STATIC_INIT:
      return enclosingElement;
    default:
      throw new IllegalArgumentException("enclosingElement: " + enclosingElement);
    }
  }

}
