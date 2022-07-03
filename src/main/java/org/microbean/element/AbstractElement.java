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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.function.Supplier;

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

  private static final VarHandle ENCLOSED_ELEMENTS;

  private static final VarHandle ENCLOSING_ELEMENT;

  static {
    final Lookup lookup = MethodHandles.lookup();
    try {
      ENCLOSED_ELEMENTS = lookup.findVarHandle(AbstractElement.class, "enclosedElements", List.class);
      ENCLOSING_ELEMENT = lookup.findVarHandle(AbstractElement.class, "enclosingElement", Element.class);
    } catch (final NoSuchFieldException | IllegalAccessException reflectiveOperationException) {
      throw new ExceptionInInitializerError(reflectiveOperationException);
    }
  }
  
  private final Name name;

  private final ElementKind kind;

  private final TypeMirror type;

  private final Set<Modifier> modifiers;

  private volatile Element enclosingElement;

  private volatile List<Element> enclosedElements;

  private volatile List<Element> readOnlyEnclosedElements;

  protected AbstractElement(final Name name,
                            final ElementKind kind,
                            final TypeMirror type,
                            final Set<? extends Modifier> modifiers,
                            final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    this(name, kind, type, modifiers, null, annotationMirrorsSupplier);
  }

  protected AbstractElement(final Name name,
                            final ElementKind kind,
                            final TypeMirror type,
                            final Set<? extends Modifier> modifiers,
                            final Supplier<List<? extends Element>> enclosedElementsSupplier,
                            final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(annotationMirrorsSupplier);
    if (enclosedElementsSupplier == null) {
      this.enclosedElements = new CopyOnWriteArrayList<>();
      this.readOnlyEnclosedElements = Collections.unmodifiableList(this.enclosedElements);
    }
    this.name = name == null ? DefaultName.EMPTY : DefaultName.of(name);
    this.kind = Objects.requireNonNull(kind, "kind");
    this.type = type == null ? DefaultNoType.NONE : type;
    this.modifiers = modifiers == null || modifiers.isEmpty() ? Set.of() : Set.copyOf(modifiers);    
  }

  @Override
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

    case OTHER:
    default:
      return v.visitUnknown(this, p);

    }
  }

  public final <E extends Element & Encloseable> void addEnclosedElement(final E e) {
    this.enclosedElements.add(e);
    e.setEnclosingElement(this);
  }

  @Override // Element
  public final List<? extends Element> getEnclosedElements() {
    return this.readOnlyEnclosedElements == null ? List.of() : this.readOnlyEnclosedElements;
  }

  @Override // Element, Encloseable
  public Element getEnclosingElement() {
    return this.enclosingElement;
  }

  @Override // Encloseable
  public void setEnclosingElement(final Element enclosingElement) {
    if (!ENCLOSING_ELEMENT.compareAndSet(this, null, enclosingElement)) {
      throw new IllegalStateException();
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
    return this.name;
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

}
