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

public final class DefaultExecutableElement extends AbstractParameterizableElement implements ExecutableElement {

  private final List<? extends VariableElement> parameters;

  private final boolean isDefault;

  private final boolean varArgs;

  private final AnnotationValue defaultValue;

  public
    <TP extends TypeParameterElement & Encloseable, P extends VariableElement & Encloseable>
    DefaultExecutableElement(final AnnotatedName simpleName,
                             final ElementKind kind,
                             final ExecutableType type,
                             final Set<? extends Modifier> modifiers,
                             final List<? extends TP> typeParameters,
                             final List<? extends P> parameters,
                             final boolean varArgs,
                             final boolean isDefault,
                             final AnnotationValue defaultValue) {
    super(kind == ElementKind.CONSTRUCTOR ? AnnotatedName.of(DefaultName.of("<init>")) :
          (kind == ElementKind.STATIC_INIT ? AnnotatedName.of(DefaultName.of("<clinit>")) :
           (kind == ElementKind.INSTANCE_INIT ? AnnotatedName.of(DefaultName.of()) : simpleName)),
          validateKind(kind),
          validateType(type),
          modifiers,
          null,
          List.of(),
          typeParameters);
    if (parameters == null || parameters.isEmpty()) {
      this.parameters = List.of();
    } else {
      final List<P> newParameters = List.copyOf(parameters);
      for (final P p : newParameters) {
        if (p.getKind() != ElementKind.PARAMETER) {
          throw new IllegalArgumentException("parameters: " + parameters);
        }
        p.setEnclosingElement(this);
      }
      this.parameters = newParameters;
    }
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


  private static final ExecutableType validateType(final ExecutableType t) {
    switch (t.getKind()) {
    case EXECUTABLE:
      return t;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final ElementKind validateKind(final ElementKind kind) {
    switch (kind) {
    case CONSTRUCTOR:
    case INSTANCE_INIT:
    case METHOD:
    case STATIC_INIT:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  public static final DefaultExecutableElement of(final ExecutableElement e) {
    if (e instanceof DefaultExecutableElement defaultExecutableElement) {
      return defaultExecutableElement;
    }
    return
      new DefaultExecutableElement(AnnotatedName.of(e.getAnnotationMirrors(), e.getSimpleName()),
                                   e.getKind(),
                                   (ExecutableType)e.asType(),
                                   e.getModifiers(),
                                   DefaultTypeParameterElement.encloseableTypeParametersOf(e.getTypeParameters()),
                                   DefaultVariableElement.encloseableParametersOf(e.getParameters()),
                                   e.isVarArgs(),
                                   e.isDefault(),
                                   e.getDefaultValue());                                   
  }

}
