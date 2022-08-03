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

import java.util.function.Consumer;
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

  private static final VarHandle ENCLOSING_ELEMENT;

  private static final VarHandle READ_ONLY_ENCLOSED_ELEMENTS;

  static {
    final Lookup lookup = MethodHandles.lookup();
    try {
      ENCLOSING_ELEMENT = lookup.findVarHandle(AbstractElement.class, "enclosingElement", Element.class);
      READ_ONLY_ENCLOSED_ELEMENTS = lookup.findVarHandle(AbstractElement.class, "readOnlyEnclosedElements", List.class);
    } catch (final NoSuchFieldException | IllegalAccessException reflectiveOperationException) {
      throw new ExceptionInInitializerError(reflectiveOperationException);
    }
  }

  private final Name name;

  private final ElementKind kind;

  private final TypeMirror type;

  private final Set<Modifier> modifiers;

  private volatile Element enclosingElement;

  private final Supplier<List<? extends Element>> enclosedElementsSupplier;

  private final Consumer<Element> enclosedElementsAdder;

  private volatile List<Element> readOnlyEnclosedElements;

  protected AbstractElement(final AnnotatedName name,
                            final ElementKind kind,
                            final TypeMirror type,
                            final Set<? extends Modifier> modifiers,
                            final Element enclosingElement, // nullable, normally null
                            final Supplier<List<? extends Element>> enclosedElementsSupplier) {
    super(name.getAnnotationMirrors());
    this.name = name == null ? DefaultName.EMPTY : name.getName();
    this.kind = Objects.requireNonNull(kind, "kind");
    this.type = type == null ? DefaultNoType.NONE : type;
    this.modifiers = modifiers == null || modifiers.isEmpty() ? Set.of() : Set.copyOf(modifiers);
    this.enclosingElement = enclosingElement;
    this.enclosedElementsSupplier = enclosedElementsSupplier;
    if (enclosedElementsSupplier == null) {
      final List<Element> enclosedElements = new CopyOnWriteArrayList<>();
      this.enclosedElementsAdder = e -> enclosedElements.add(e);
      this.readOnlyEnclosedElements = Collections.unmodifiableList(enclosedElements);
    } else {
      this.enclosedElementsAdder = e -> { throw new IllegalStateException(); };
    }

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


  /*
   * Enclosed elements.
   *
   *
   * ModuleElement:     encloses PackageElements
   * PackageElement:    encloses TypeElements
   * TypeElement:       encloses TypeElements (nested classes),
   *                    ExecutableElements (methods), VariableElements
   *                    (fields), RecordComponentElements
   * ExecutableElement: encloses NOTHING
   */


  <E extends Element & Encloseable> void addEnclosedElement0(final E e) {
    // Make sure e is in the set of things that can potentially be
    // enclosed.
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case CONSTRUCTOR:
    case ENUM:
    case INSTANCE_INIT:
    case INTERFACE:
    case METHOD:
    case PACKAGE:
    case RECORD:
    case RECORD_COMPONENT:
    case STATIC_INIT:
      break;
    default:
      throw new IllegalArgumentException("e: " + e);
    }
    this.enclosedElementsAdder.accept(e);
    if (e.getEnclosingElement() == null) {
      // TODO: this needs refining due to the strange relationship
      // between getEnclosedElements() and getEnclosingElement().
      //
      // For example, a VariableElement representing a method
      // parameter will have an ExecutableElement as its enclosing
      // element, but that ExecutableElement will not contain the
      // VariableElement among its enclosed elements.
      e.setEnclosingElement(this);
    }
  }

  @Override // Element
  public final List<? extends Element> getEnclosedElements() {
    List<Element> readOnlyEnclosedElements = this.readOnlyEnclosedElements; // volatile read
    if (readOnlyEnclosedElements == null) {
      readOnlyEnclosedElements = List.copyOf(this.enclosedElementsSupplier.get());
      if (READ_ONLY_ENCLOSED_ELEMENTS.compareAndSet(this, null, readOnlyEnclosedElements)) { // volatile write
        for (final Element enclosedElement : readOnlyEnclosedElements) {
          // TODO: the following test needs refining.
          //
          // The enclosed/enclosing relationship is extremely weird
          // and poorly defined.
          //
          // For example, a VariableElement representing a method
          // parameter will have an ExecutableElement as its enclosing
          // element, but that ExecutableElement will not contain the
          // VariableElement among its enclosed elements.
          if (enclosedElement.getEnclosingElement() == null && enclosedElement instanceof Encloseable e) {
            // We use nullness as a cheap proxy: if the existing
            // enclosing element is already set, we honor it.  This
            // means, of course, it's possible for illegal states to
            // occur: a member class being added as an enclosed
            // element of class A could claim its enclosing element is
            // actually class B and this would be permitted.  But this does let things like a TypeElement enclosing
            e.setEnclosingElement(this);
          }
        }
      } else {
        return this.readOnlyEnclosedElements; // volatile read
      }
    }
    return readOnlyEnclosedElements;
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
