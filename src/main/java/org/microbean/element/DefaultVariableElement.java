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

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public final class DefaultVariableElement extends AbstractElement implements VariableElement {

  public final Object constantValue;

  public DefaultVariableElement(final AnnotatedName simpleName,
                                final ElementKind kind,
                                final TypeMirror type,
                                final Set<? extends Modifier> modifiers,
                                final Element enclosingElement,
                                final Object constantValue) {
    super(simpleName,
          validate(kind),
          validate(type),
          modifiers,
          enclosingElement,
          List::of);
    this.constantValue = constantValue;
  }

  @Override // AbstractElement
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitVariable(this, p);
  }

  @Override // VariableElement
  public final Object getConstantValue() {
    return this.constantValue;
  }

  @Override // AbstractElement
  public final void setEnclosingElement(final Element enclosingElement) {
    super.setEnclosingElement(validate(enclosingElement));
  }


  /*
   * Static methods.
   */


  private static final ElementKind validate(final ElementKind kind) {
    switch (kind) {
    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return kind;
    default:
      throw new IllegalArgumentException("Not a valid variable element kind: " + kind);
    }
  }

  private static final <E extends Element> E validate(final E enclosingElement) {
    // TODO: implement
    return enclosingElement;
  }

  private static final <T extends TypeMirror> T validate(final T type) {
    return type;
  }

  public static final DefaultVariableElement of(final Field f) {
    final AnnotatedName simpleName = AnnotatedName.of(DefaultName.of(f.getName()));
    final TypeMirror type = AbstractTypeMirror.of(f.getAnnotatedType());
    final Collection<Modifier> modifierSet = new HashSet<>();
    final int modifiers = f.getModifiers();
    if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
      modifierSet.add(Modifier.ABSTRACT);
    }
    if (java.lang.reflect.Modifier.isFinal(modifiers)) {
      modifierSet.add(Modifier.FINAL);
    }
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      modifierSet.add(Modifier.PRIVATE);
    }
    if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      modifierSet.add(Modifier.PROTECTED);
    }
    if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      modifierSet.add(Modifier.PUBLIC);
    }
    if (java.lang.reflect.Modifier.isStatic(modifiers)) {
      modifierSet.add(Modifier.STATIC);
    }
    if (java.lang.reflect.Modifier.isStrict(modifiers)) {
      modifierSet.add(Modifier.STRICTFP);
    }
    if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
      modifierSet.add(Modifier.SYNCHRONIZED);
    }
    if (java.lang.reflect.Modifier.isTransient(modifiers)) {
      modifierSet.add(Modifier.TRANSIENT);
    }
    if (java.lang.reflect.Modifier.isVolatile(modifiers)) {
      modifierSet.add(Modifier.VOLATILE);
    }
    final EnumSet<Modifier> finalModifiers = EnumSet.copyOf(modifierSet);
    
    return new DefaultVariableElement(simpleName, ElementKind.FIELD, type, finalModifiers, null, null);
  }

  public static final DefaultVariableElement of(final Parameter p) {
    final AnnotatedName simpleName = AnnotatedName.of(DefaultName.of(p.getName()));
    final TypeMirror type = AbstractTypeMirror.of(p.getParameterizedType());
    return new DefaultVariableElement(simpleName, ElementKind.PARAMETER, type, Set.of(), null, null);
  }

}
