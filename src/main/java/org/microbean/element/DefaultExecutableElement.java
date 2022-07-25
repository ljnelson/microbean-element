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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

public class DefaultExecutableElement extends AbstractParameterizableElement implements ExecutableElement {

  private final List<VariableElement> mutableParameters;
  
  private final List<? extends VariableElement> parameters;

  private final boolean isDefault;

  private final boolean varArgs;

  private final AnnotationValue defaultValue;

  public DefaultExecutableElement(final AnnotatedName simpleName,
                                  final ElementKind kind,
                                  final ExecutableType type,
                                  final Set<? extends Modifier> modifiers,
                                  final boolean varArgs,
                                  final boolean isDefault,
                                  final AnnotationValue defaultValue) {
    super(kind == ElementKind.CONSTRUCTOR ? AnnotatedName.of(DefaultName.of("<init>")) :
          (kind == ElementKind.STATIC_INIT ? AnnotatedName.of(DefaultName.of("<clinit>")) :
           (kind == ElementKind.INSTANCE_INIT ? AnnotatedName.of(DefaultName.EMPTY) : simpleName)),
          kind,
          validate(type),
          modifiers,
          null,
          List::of);
    switch (kind) {
    case CONSTRUCTOR:
    case INSTANCE_INIT:
    case METHOD:
    case STATIC_INIT:
      break;
    default:
      throw new IllegalArgumentException("Not an executable kind: " + kind);
    }
    this.mutableParameters = new CopyOnWriteArrayList<>();
    this.parameters = Collections.unmodifiableList(this.mutableParameters);
    this.varArgs = varArgs;
    this.isDefault = isDefault;
    this.defaultValue = defaultValue;
  }

  @Override // Element
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitExecutable(this, p);
  }

  @Override // ExecutableElement
  public final ExecutableType asType() {
    return (ExecutableType)super.asType();
  }

  @Override // ExecutableElement
  public final boolean isDefault() {
    return this.isDefault;
  }

  @Override // ExecutableElement
  public final boolean isVarArgs() {
    return this.varArgs;
  }

  @Override // ExecutableElement
  public final AnnotationValue getDefaultValue() {
    return this.defaultValue;
  }

  @Override // ExecutableElement
  public final List<? extends VariableElement> getParameters() {
    return this.parameters;
  }

  public final <T extends VariableElement & Encloseable> void addParameter(final T p) {
    switch (p.getKind()) {
    case PARAMETER:
      this.mutableParameters.add(p);
      p.setEnclosingElement(this);
      break;
    default:
      throw new IllegalArgumentException();
    }
  }
  
  @Override // ExecutableElement
  public final List<? extends TypeMirror> getThrownTypes() {
    return this.asType().getThrownTypes();
  }

  @Override // ExecutableElement
  public final TypeMirror getReceiverType() {
    return this.asType().getReceiverType();
  }

  @Override // ExecutableElement
  public final TypeMirror getReturnType() {
    return this.asType().getReturnType();
  }


  /*
   * Static methods.
   */

  
  private static final ExecutableType validate(final ExecutableType t) {
    switch (t.getKind()) {
    case EXECUTABLE:
      return t;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  public static final DefaultExecutableElement of(final ExecutableElement e) {
    if (e instanceof DefaultExecutableElement dee) {
      return dee;
    }
    return of(AnnotatedName.of(e.getAnnotationMirrors(), e.getSimpleName()),
              e.getKind(),
              (ExecutableType)e.asType(),
              e.getModifiers(),
              e.isVarArgs(),
              e.isDefault(),
              e.getDefaultValue());
  }
  
  public static final DefaultExecutableElement of(final AnnotatedName simpleName,
                                                  final ElementKind kind,
                                                  final ExecutableType type,
                                                  final Set<? extends Modifier> modifiers,
                                                  final boolean varArgs,
                                                  final boolean isDefault,
                                                  final AnnotationValue defaultValue) {
    return new DefaultExecutableElement(simpleName,
                                        kind,
                                        type,
                                        modifiers,
                                        varArgs,
                                        isDefault,
                                        defaultValue);
  }
  
  public static final DefaultExecutableElement of(final Executable e) {
    final AnnotatedName simpleName = AnnotatedName.of(DefaultName.of(e.getName()));
    final ElementKind kind = e instanceof Method ? ElementKind.METHOD : ElementKind.CONSTRUCTOR;
    final ExecutableType type = DefaultExecutableType.of(e);
    final Collection<Modifier> modifierSet = new HashSet<>();
    final int modifiers = e.getModifiers();
    if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
      modifierSet.add(Modifier.ABSTRACT);
    }
    final boolean isDefault = e instanceof Method m && m.isDefault();
    if (isDefault) {
      modifierSet.add(Modifier.DEFAULT);
    }
    if (java.lang.reflect.Modifier.isFinal(modifiers)) {
      modifierSet.add(Modifier.FINAL);
    }
    if (java.lang.reflect.Modifier.isNative(modifiers)) {
      modifierSet.add(Modifier.NATIVE);
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
    if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
      modifierSet.add(Modifier.SYNCHRONIZED);
    }
    final EnumSet<Modifier> finalModifiers = EnumSet.copyOf(modifierSet);
    final Class<?> declaringClass = e.getDeclaringClass();

    // TODO: no real way to handle default value

    final boolean varArgs = e.isVarArgs();

    final DefaultExecutableElement returnValue =
      new DefaultExecutableElement(simpleName,
                                   kind,
                                   type,
                                   finalModifiers,
                                   varArgs,
                                   isDefault,
                                   null);

    for (final java.lang.reflect.TypeVariable<?> t : e.getTypeParameters()) {
      returnValue.addTypeParameter(DefaultTypeParameterElement.of(t));
    }

    for (final Parameter p : e.getParameters()) {
      final DefaultVariableElement element = DefaultVariableElement.of(p);
      returnValue.addParameter(element);
    }

    return returnValue;
  }


  
}
