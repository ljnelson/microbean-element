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
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public final class DefaultTypeParameterElement extends AbstractElement implements TypeParameterElement {

  private Element genericElement;

  private final List<? extends AnnotationMirror> annotationMirrors;
  
  public <T extends DefineableType<? super TypeParameterElement> & TypeVariable>
    DefaultTypeParameterElement(final Name simpleName,
                                final List<? extends AnnotationMirror> annotationMirrors,
                                final T type) {
    this(simpleName,
         annotationMirrors,
         type,
         Set.of());
  }
  
  public <T extends DefineableType<? super TypeParameterElement> & TypeVariable>
    DefaultTypeParameterElement(final Name simpleName,
                                final List<? extends AnnotationMirror> annotationMirrors,
                                final T typeVariable,
                                final Set<? extends Modifier> modifiers) {
    super(simpleName,
          ElementKind.TYPE_PARAMETER,
          validateTypeVariable(typeVariable),
          modifiers,
          null, // enclosingElement. See #setEnclosingElement(Element) below.
          List.of());    
    typeVariable.setDefiningElement(this);
    if (annotationMirrors == null) {
      this.annotationMirrors = List.of();
    } else if (annotationMirrors instanceof DeferredList<? extends AnnotationMirror>) {
      this.annotationMirrors = annotationMirrors;
    } else {
      this.annotationMirrors = List.copyOf(annotationMirrors);
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

  @Override
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.annotationMirrors;
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

  @Override
  public final String toString() {
    return this.getSimpleName() + " " + this.asType();
  }
  
  private final void setGenericElement(final Element genericElement) {
    final Element old = this.genericElement;
    if (old != null) {
      throw new IllegalStateException();
    }
    this.genericElement = validateGenericElement(genericElement);
  }


  /*
   * Static methods.
   */

  
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

  @SuppressWarnings("unchecked")
  public static final <E extends TypeParameterElement & Encloseable> List<? extends E> encloseableTypeParametersOf(final List<? extends TypeParameterElement> list) {
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    final List<E> newList = new ArrayList<>(list.size());
    for (final TypeParameterElement e : list) {
      if (e instanceof Encloseable enc) {
        newList.add((E)enc);
      } else {
        newList.add((E)DefaultTypeParameterElement.of(e));
      }
    }
    return Collections.unmodifiableList(newList);
  }

  public static final DefaultTypeParameterElement of(final TypeParameterElement e) {
    if (e instanceof DefaultTypeParameterElement defaultTypeParameterElement) {
      return defaultTypeParameterElement;
    }
    return
      new DefaultTypeParameterElement(e.getSimpleName(),
                                      e.getAnnotationMirrors(),
                                      DefaultTypeVariable.of((TypeVariable)e.asType()),
                                      e.getModifiers());
  }

}
