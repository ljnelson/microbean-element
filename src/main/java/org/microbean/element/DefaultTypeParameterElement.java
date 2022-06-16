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

  private final TypeElement genericElement;
  
  public DefaultTypeParameterElement(final Name simpleName, final TypeVariable type) {
    this(simpleName, type, Set.of(), null, List.of());
  }
  
  public DefaultTypeParameterElement(final Name simpleName,
                                     final TypeVariable type,
                                     final Set<? extends Modifier> modifiers,
                                     final TypeElement genericElement,
                                     final List<? extends AnnotationMirror> annotationMirrors) {
    super(simpleName,
          ElementKind.TYPE_PARAMETER,
          type,
          modifiers,
          null,
          annotationMirrors);
    if (type instanceof DefaultTypeVariable dtv) {
      dtv.element(this);
    }
    this.genericElement = validate(genericElement);
    if (genericElement instanceof DefaultTypeElement dte) {
      dte.addTypeParameter(this);
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
  public final TypeElement getEnclosingElement() {
    return this.getGenericElement();
  }

  @Override // TypeParameterElement
  public final TypeElement getGenericElement() {
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
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case ERROR:
    case EXECUTABLE:
    case FLOAT:
    case INT:
    case LONG:
    case MODULE:
    case NONE:
    case NULL:
    case OTHER:
    case PACKAGE:
    case SHORT:
    case UNION:
    case VOID:
    case WILDCARD:
      throw new IllegalArgumentException("typeVariable: " + typeVariable);      
    default:
      throw new AssertionError();
    }
  }
  
  private static final <E extends TypeElement> E validate(final E element) {
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
  
}
