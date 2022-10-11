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
import java.util.Objects;
import java.util.Set;

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

abstract sealed class AbstractElement
  extends AbstractAnnotatedConstruct
  implements Element, Encloseable
  permits AbstractParameterizableElement,
          DefaultModuleElement,
          DefaultPackageElement,
          DefaultRecordComponentElement,
          DefaultTypeParameterElement,
          DefaultVariableElement,
          SyntheticArrayElement,
          SyntheticElement,
          SyntheticPrimitiveElement,
          SyntheticWildcardElement {


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


  protected <E extends Element & Encloseable> AbstractElement(final Name simpleName,
                                                              final ElementKind kind,
                                                              final TypeMirror type,
                                                              final Set<? extends Modifier> modifiers,
                                                              final Element enclosingElement, // nullable, normally null
                                                              final List<? extends E> enclosedElements) {
    super();
    this.simpleName = Objects.requireNonNull(simpleName, "simpleName");
    this.kind = validateKind(kind);
    this.type = validateType(type);
    this.modifiers = modifiers == null || modifiers.isEmpty() ? Set.of() : Set.copyOf(modifiers);
    this.enclosingElement = enclosingElement;
    if (enclosedElements == null) {
      this.enclosedElements = List.of();
    } else if (enclosedElements instanceof DeferredList<? extends E> dl) {
      this.enclosedElements = dl.withTransformation(e -> {
          if (this.canEnclose(e)) {
            e.setEnclosingElement(this);
          }
        });
    } else {
      final List<E> newEnclosedElements = List.copyOf(enclosedElements);
      for (final E e : newEnclosedElements) {
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
    if (old != enclosingElement) {
      if (enclosingElement == null) {
        throw new NullPointerException("enclosingElement");
      } else if (old == null) {
        this.enclosingElement = enclosingElement;
      } else {
        throw new IllegalStateException("this.enclosingElement != null: " + old + "; enclosingElement: " + enclosingElement);
      }
    }
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
    return Equality.hashCode(this, true);
  }

  @Override // Element
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof Element e) { // instanceof on purpose
      return Equality.equals(this, e, true);
    } else {
      return false;
    }
  }


  /*
   * Static methods.
   */


  private static final ElementKind validateKind(final ElementKind kind) {
    switch (kind) {
    case ANNOTATION_TYPE:
    case BINDING_VARIABLE:
    case CLASS:
    case CONSTRUCTOR:
    case ENUM:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case INSTANCE_INIT:
    case INTERFACE:
    case LOCAL_VARIABLE:
    case METHOD:
    case MODULE:
    case OTHER:
    case PACKAGE:
    case PARAMETER:
    case RECORD:
    case RECORD_COMPONENT:
    case RESOURCE_VARIABLE:
    case STATIC_INIT:
    case TYPE_PARAMETER:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  private static final TypeMirror validateType(final TypeMirror type) {
    if (type == null) {
      return DefaultNoType.NONE;
    }
    switch (type.getKind()) {
    case ARRAY:
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DECLARED:
    case DOUBLE:
    case ERROR:
    case EXECUTABLE:
    case FLOAT:
    case INT:
    case INTERSECTION:
    case LONG:
    case MODULE:
    case NONE:
    case NULL:
    case OTHER:
    case PACKAGE:
    case SHORT:
    case TYPEVAR:
    case UNION:
    case VOID:
    case WILDCARD:
      return type;
    default:
      throw new IllegalArgumentException("type: " + type);
    }
  }

  @SuppressWarnings("unchecked")
  public static final <E extends Element & Encloseable> List<? extends E> encloseablesOf(final List<? extends Element> list) {
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    final List<E> newList = new ArrayList<>(list.size());
    for (final Element e : list) {
      switch (e) {
      case Encloseable enc -> newList.add((E)enc);
      case ExecutableElement ee -> newList.add((E)DefaultExecutableElement.of(ee));
      case PackageElement pe -> newList.add((E)DefaultPackageElement.of(pe));
      case RecordComponentElement rce -> newList.add((E)DefaultRecordComponentElement.of(rce));
      case TypeElement te -> newList.add((E)DefaultTypeElement.of(te));
      case TypeParameterElement tpe -> newList.add((E)DefaultTypeParameterElement.of(tpe));
      case VariableElement ve -> newList.add((E)DefaultVariableElement.of(ve));
      default -> throw new IllegalArgumentException("list: " + list);
      }
    }
    return Collections.unmodifiableList(newList);
  }

}
