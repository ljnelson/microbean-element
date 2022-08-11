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
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public final class DefaultTypeParameterElement extends AbstractElement implements TypeParameterElement {

  private Element genericElement;
  
  public DefaultTypeParameterElement(final AnnotatedName simpleName, final TypeVariable type) {
    this(simpleName, type, Set.of());
  }
  
  public DefaultTypeParameterElement(final AnnotatedName simpleName,
                                     final TypeVariable typeVariable,
                                     final Set<? extends Modifier> modifiers) {
    super(simpleName,
          ElementKind.TYPE_PARAMETER,
          validateTypeVariable(typeVariable),
          modifiers,
          null,
          List.of());
    if (typeVariable instanceof DefaultTypeVariable dtv) {
      dtv.definingElement(this);
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

  @Override // AbstractElement
  public final void setEnclosingElement(final Element enclosingElement) {
    this.setGenericElement(enclosingElement);
  }

  @Override // TypeParameterElement
  public final Element getGenericElement() {
    return this.genericElement;
  }

  private final void setGenericElement(final Element genericElement) {
    final Element old = this.genericElement;
    if (old != null) {
      throw new IllegalStateException();
    }
    this.genericElement = validateGenericElement(genericElement);
  }

  private static final TypeVariable validateTypeVariable(final TypeVariable tv) {
    switch (tv.getKind()) {
    case TYPEVAR:
      return tv;
    default:
      throw new IllegalArgumentException("tv: " + tv);
    }
  }

  private static final List<? extends TypeMirror> boundsFrom(final TypeVariable typeVariable) {
    final TypeMirror upperBound = typeVariable == null ? ObjectConstruct.JAVA_LANG_OBJECT_TYPE : typeVariable.getUpperBound();
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
  
  private static final Element validateGenericElement(final Element element) {
    if (element == null) {
      return null;
    }
    switch (element.getKind()) {
    case CLASS:
    case CONSTRUCTOR:
    case INTERFACE:
    case METHOD:
      return element;
    default:
      throw new IllegalArgumentException("Not a valid generic element: " + element);
    }
  }

}
