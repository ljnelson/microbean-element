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
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

public class DefaultExecutableElement extends AbstractElement implements ExecutableElement {

  private final List<? extends TypeParameterElement> typeParameters;

  private final List<? extends VariableElement> parameters;

  private final boolean isDefault;

  private final boolean varArgs;

  private final AnnotationValue defaultValue;
  
  public DefaultExecutableElement(final Name simpleName,
                                  final ElementKind kind,
                                  final ExecutableType type,
                                  final Set<? extends Modifier> modifiers,
                                  final AbstractElement enclosingElement,
                                  final List<? extends TypeParameterElement> typeParameters,
                                  final List<? extends VariableElement> parameters,
                                  final boolean varArgs,
                                  final boolean isDefault,
                                  final AnnotationValue defaultValue,
                                  final List<? extends AnnotationMirror> annotationMirrors) {
    super(kind == ElementKind.CONSTRUCTOR ? DefaultName.of("<init>") :
          (kind == ElementKind.STATIC_INIT ? DefaultName.of("<clinit>") :
           (kind == ElementKind.INSTANCE_INIT ? DefaultName.EMPTY : simpleName)),
          kind,
          validate(type),
          modifiers,
          enclosingElement,
          annotationMirrors);
    switch (kind) {
    case CONSTRUCTOR:
    case INSTANCE_INIT:
    case METHOD:
    case STATIC_INIT:
      break;
    default:
      throw new IllegalArgumentException("Not an executable kind: " + kind);
    }
    this.typeParameters = typeParameters == null || typeParameters.isEmpty() ? List.of() : List.copyOf(typeParameters);
    this.parameters = parameters == null || parameters.isEmpty() ? List.of() : List.copyOf(parameters);
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

  @Override // Parameterizable
  public final List<? extends TypeParameterElement> getTypeParameters() {
    return this.typeParameters;
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

  private static final ExecutableType validate(final ExecutableType t) {
    switch (t.getKind()) {
    case EXECUTABLE:
      return t;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }
  
  public static final DefaultExecutableElement of(final ExecutableElement e) {
    if (e instanceof DefaultExecutableElement de) {
      return de;
    }
    return
      new DefaultExecutableElement(e.getSimpleName(),
                                   e.getKind(),
                                   (ExecutableType)e.asType(),
                                   e.getModifiers(),
                                   (AbstractElement)e.getEnclosingElement(),
                                   e.getTypeParameters(),
                                   e.getParameters(),
                                   e.isVarArgs(),
                                   e.isDefault(),
                                   e.getDefaultValue(),
                                   e.getAnnotationMirrors());
  }
  
}
