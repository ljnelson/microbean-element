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
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.TypeVariable;

public class DefaultExecutableType extends AbstractTypeMirror implements ExecutableType {

  private final List<? extends TypeMirror> parameterTypes;

  private final TypeMirror receiverType;

  private final TypeMirror returnType;
  
  private final List<? extends TypeMirror> thrownTypes;
  
  private final List<? extends TypeVariable> typeVariables;
  
  public DefaultExecutableType(final List<? extends TypeMirror> parameterTypes,
                               final TypeMirror receiverType,
                               final TypeMirror returnType,
                               final List<? extends TypeMirror> thrownTypes,
                               final List<? extends TypeVariable> typeVariables,
                               final List<? extends AnnotationMirror> annotationMirrors) {
    super(TypeKind.EXECUTABLE, annotationMirrors);
    this.parameterTypes = parameterTypes == null || parameterTypes.isEmpty() ? List.of() : List.copyOf(parameterTypes);
    this.receiverType = receiverType == null ? DefaultNoType.NONE : receiverType;
    this.returnType = returnType == null ? DefaultNoType.VOID : returnType;
    this.thrownTypes = thrownTypes == null || thrownTypes.isEmpty() ? List.of() : List.copyOf(thrownTypes);
    this.typeVariables = typeVariables == null || typeVariables.isEmpty() ? List.of() : List.copyOf(typeVariables);
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitExecutable(this, p);
  }

  @Override // ExecutableType
  public List<? extends TypeMirror> getParameterTypes() {
    return this.parameterTypes;
  }
  
  @Override // ExecutableType
  public List<? extends TypeMirror> getThrownTypes() {
    return this.thrownTypes;
  }

  @Override // ExecutableType
  public List<? extends TypeVariable> getTypeVariables() {
    return this.typeVariables;
  }
  
  @Override // ExecutableType
  public final TypeMirror getReceiverType() {
    return this.receiverType;
  }

  @Override // ExecutableType
  public final TypeMirror getReturnType() {
    return this.returnType;
  }

  public static DefaultExecutableType of(final ExecutableType e) {
    if (e instanceof DefaultExecutableType de) {
      return de;
    }
    return
      new DefaultExecutableType(e.getParameterTypes(),
                                e.getReceiverType(),
                                e.getReturnType(),
                                e.getThrownTypes(),
                                e.getTypeVariables(),
                                e.getAnnotationMirrors());
  }

  public static DefaultExecutableType of(final List<? extends TypeMirror> parameterTypes,
                                         final TypeMirror receiverType,
                                         final TypeMirror returnType,
                                         final List<? extends TypeMirror> thrownTypes,
                                         final List<? extends TypeVariable> typeVariables,
                                         final List<? extends AnnotationMirror> annotationMirrors) {
    return new DefaultExecutableType(parameterTypes,
                                     receiverType,
                                     returnType,
                                     thrownTypes,
                                     typeVariables,
                                     annotationMirrors);
  }
  
}
