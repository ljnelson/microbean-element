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
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.TypeMirror;

public abstract class AbstractParameterizableElement extends AbstractElement implements Parameterizable {

  private final List<? extends TypeParameterElement> typeParameters;
  
  protected
    <T extends Element & Encloseable, P extends TypeParameterElement & Encloseable>
    AbstractParameterizableElement(final AnnotatedName name,
                                   final ElementKind kind,
                                   final TypeMirror type,
                                   final Set<? extends Modifier> modifiers,
                                   final Element enclosingElement,
                                   final List<? extends T> enclosedElements,
                                   final List<? extends P> typeParameters) {
    super(name, kind, type, modifiers, enclosingElement, enclosedElements);
    if (typeParameters == null || typeParameters.isEmpty()) {
      this.typeParameters = List.of();
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

}
