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
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

public abstract class AbstractElement extends AbstractAnnotatedConstruct implements Element, Encloseable {


  /*
   * Instance fields.
   */


  private final Name simpleName;

  private final ElementKind kind;

  private final TypeMirror type;

  private final Set<Modifier> modifiers;

  private Element enclosingElement;

  private final List<? extends Element> enclosedElements;


  /*
   * Constructors.
   */


  protected <T extends Element & Encloseable> AbstractElement(final AnnotatedName simpleName,
                                                              final ElementKind kind,
                                                              final TypeMirror type,
                                                              final Set<? extends Modifier> modifiers,
                                                              final Element enclosingElement, // nullable, normally null
                                                              final List<? extends T> enclosedElements) {
    super(simpleName.getAnnotationMirrors());
    this.simpleName = simpleName.getName();
    this.kind = Objects.requireNonNull(kind, "kind");
    this.type = type == null ? DefaultNoType.NONE : type;
    this.modifiers = modifiers == null || modifiers.isEmpty() ? Set.of() : Set.copyOf(modifiers);
    this.enclosingElement = enclosingElement;
    if (enclosedElements == null || enclosedElements.isEmpty()) {
      this.enclosedElements = List.of();
    } else {
      final List<T> newEnclosedElements = List.copyOf(enclosedElements);
      for (final T e : newEnclosedElements) {
        if (this.canEnclose(e)) {
          e.setEnclosingElement(this);
        }
      }
      this.enclosedElements = newEnclosedElements;
    }
  }


  /*
   * Instance methods.
   */


  @Override // Element
  public TypeMirror asType() {
    return this.type;
  }

  @Override // Element
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    switch (this.getKind()) {

    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return v.visitType((TypeElement)this, p);

    case TYPE_PARAMETER:
      return v.visitTypeParameter((TypeParameterElement)this, p);

    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return v.visitVariable((VariableElement)this, p);

    case RECORD_COMPONENT:
      return v.visitRecordComponent((RecordComponentElement)this, p);

    case CONSTRUCTOR:
    case INSTANCE_INIT:
    case METHOD:
    case STATIC_INIT:
      return v.visitExecutable((ExecutableElement)this, p);

    case PACKAGE:
      return v.visitPackage((PackageElement)this, p);

    case MODULE:
      return v.visitModule((ModuleElement)this, p);

    default:
      return v.visitUnknown(this, p);

    }
  }

  private final boolean canEnclose(final Element e) {
    // Make sure e is in the set of things that can potentially be
    // enclosed.
    switch (this.getKind()) {
    case MODULE:
      switch (e.getKind()) {
      case PACKAGE:
        return true;
      default:
        throw new IllegalArgumentException("e: " + e);
      }
    case PACKAGE:
      switch (e.getKind()) {
      case ANNOTATION_TYPE:
      case CLASS:
      case ENUM:
      case INTERFACE:
      case RECORD:
        return true;
      default:
        throw new IllegalArgumentException("e: " + e);
      }
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
      switch (e.getKind()) {
      case ANNOTATION_TYPE:
      case CLASS:
      case CONSTRUCTOR:
      case ENUM:
      case FIELD:
      case INSTANCE_INIT:
      case INTERFACE:
      case METHOD:
      case RECORD:
      case STATIC_INIT:
        return true;
      default:
        throw new IllegalArgumentException("e: " + e);
      }
    case RECORD:
      switch (e.getKind()) {
      case ANNOTATION_TYPE:
      case CLASS:
      case CONSTRUCTOR:
      case ENUM:
      case FIELD:
      case INSTANCE_INIT:
      case INTERFACE:
      case METHOD:
      case RECORD:
      case RECORD_COMPONENT:
      case STATIC_INIT:
        return true;
      default:
        throw new IllegalArgumentException("e: " + e);
      }
    default:
      throw new IllegalArgumentException("e: " + e);
    }
  }

  @Override // Element
  public final List<? extends Element> getEnclosedElements() {
    return this.enclosedElements;
  }

  @Override // Element, Encloseable
  public Element getEnclosingElement() {
    return this.enclosingElement;
  }

  @Override // Encloseable
  public void setEnclosingElement(final Element enclosingElement) {
    final Element old = this.enclosingElement;
    if (old != null) {
      throw new IllegalStateException();
    }
    this.enclosingElement = enclosingElement;
  }

  @Override // Element
  public ElementKind getKind() {
    return this.kind;
  }

  @Override // Element
  public Set<Modifier> getModifiers() {
    return this.modifiers;
  }

  @Override // Element
  public Name getSimpleName() {
    return this.simpleName;
  }

  @Override // Element
  public int hashCode() {
    return org.microbean.element.Equality.hashCode(this, true);
  }

  @Override // Element
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof Element e) { // instanceof on purpose
      return org.microbean.element.Equality.equals(this, e, true);
    } else {
      return false;
    }
  }

}
