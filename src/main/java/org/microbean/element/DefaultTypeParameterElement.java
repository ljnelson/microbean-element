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

import java.lang.reflect.Executable;
import java.lang.reflect.GenericDeclaration;

import java.util.List;
import java.util.Set;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public final class DefaultTypeParameterElement extends AbstractElement implements TypeParameterElement {

  private final Element genericElement;
  
  public DefaultTypeParameterElement(final Name simpleName, final TypeVariable type) {
    this(simpleName, type, Set.of(), null, null);
  }
  
  public DefaultTypeParameterElement(final Name simpleName,
                                     final TypeVariable type,
                                     final Set<? extends Modifier> modifiers,
                                     final Element genericElement,
                                     final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(simpleName,
          ElementKind.TYPE_PARAMETER,
          type,
          modifiers,
          null,
          annotationMirrorsSupplier);
    if (type instanceof DefaultTypeVariable dtv) {
      dtv.element(this);
    }
    this.genericElement = validate(genericElement);
    if (genericElement instanceof DefaultTypeElement dte) {
      dte.addTypeParameter(this);
    } else if (genericElement instanceof DefaultExecutableElement dee) {
      dee.addTypeParameter(this);
    }
  }
  
  @Override // AbstractElement
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitTypeParameter(this, p);
  }
  
  @Override // TypeParameterElement
  public final TypeVariable asType() {
    return (TypeVariable)super.asType();
  }
  
  @Override // TypeParameterElement
  public final List<? extends TypeMirror> getBounds() {
    return boundsFrom(this.asType());
  }

  @Override // AbstractElement
  public final Element getEnclosingElement() {
    return this.getGenericElement();
  }

  @Override // TypeParameterElement
  public final Element getGenericElement() {
    return this.genericElement;
  }

  private static final List<? extends TypeMirror> boundsFrom(final TypeVariable typeVariable) {
    final TypeMirror upperBound = typeVariable == null ? DefaultTypeElement.JAVA_LANG_OBJECT.asType() : typeVariable.getUpperBound();
    switch (upperBound.getKind()) {
    case INTERSECTION:
      return ((IntersectionType)upperBound).getBounds();
    case ARRAY:
    case DECLARED:
    case TYPEVAR:
      return List.of(upperBound);
    default:
      throw new IllegalArgumentException("typeVariable: " + typeVariable);      
    }
  }
  
  private static final <E extends Element> E validate(final E element) {
    if (element != null) {
      switch (element.getKind()) {
      case CLASS:
      case CONSTRUCTOR:
      case INTERFACE:
      case METHOD:
        break;
      default:
        throw new IllegalArgumentException("Not a valid enclosing element: " + element);
      }
    }
    return element;
  }

  public static DefaultTypeParameterElement of(final Element genericElement, final java.lang.reflect.TypeVariable<?> tv) {
    final Name simpleName = DefaultName.of(tv.getName());
    final TypeVariable type = DefaultTypeVariable.of(tv);
    // TODO: verify: what modifiers could possibly exist on a TypeParameterElement?
    return new DefaultTypeParameterElement(simpleName, type, Set.of(), genericElement, null);
  }

}
