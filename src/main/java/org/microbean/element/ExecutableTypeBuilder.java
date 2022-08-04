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

import javax.lang.model.element.Element;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public final class ExecutableTypeBuilder
  extends AbstractAnnotatedConstructBuilder<ExecutableType, ExecutableTypeBuilder> {

  private List<? extends TypeMirror> parameterTypes;

  private TypeMirror receiverType;

  private TypeMirror returnType;
  
  private List<? extends TypeVariable> typeVariables;
  
  private List<? extends TypeMirror> thrownTypes;

  public ExecutableTypeBuilder() {
    super();
    this.parameterTypes = List.of();
    this.returnType = new NoTypeBuilder().withKind(TypeKind.VOID).build();
    this.typeVariables = List.of();
    this.thrownTypes = List.of();
  }

  public final List<? extends TypeMirror> parameterTypes() {
    return this.parameterTypes;
  }

  public final ExecutableTypeBuilder withParameterTypes(final List<? extends TypeMirror> parameterTypes) {
    this.parameterTypes = validateParameterTypes(parameterTypes);
    return self();
  }
  
  public final TypeMirror receiverType() {
    return this.receiverType;
  }

  public final ExecutableTypeBuilder withReceiverType(final TypeMirror receiverType) {
    this.receiverType = validateReceiverType(receiverType);
    return self();
  }

  public final TypeMirror returnType() {
    return this.returnType;
  }

  public final ExecutableTypeBuilder withReturnType(final TypeMirror returnType) {
    this.returnType = validateReturnType(returnType);
    return self();
  }

  public final List<? extends TypeMirror> thrownTypes() {
    return this.thrownTypes;
  }

  public final ExecutableTypeBuilder withThrownTypes(final List<? extends TypeMirror> thrownTypes) {
    this.thrownTypes = validateThrownTypes(thrownTypes);
    return self();
  }
  
  public final List<? extends TypeVariable> typeVariables() {
    return this.typeVariables;
  }

  public final ExecutableTypeBuilder withTypeVariables(final List<? extends TypeVariable> typeVariables) {
    this.typeVariables = typeVariables == null || typeVariables.isEmpty() ? List.of() : List.copyOf(typeVariables);
    return self();
  }

  @Override // AbstractBuilder
  public final ExecutableType build() {
    return
      DefaultExecutableType.of(this.parameterTypes(),
                               this.receiverType(),
                               this.returnType(),
                               this.thrownTypes(),
                               this.typeVariables(),
                               this.annotations());
  }

  private static final <T extends TypeMirror> List<T> validateParameterTypes(final List<T> parameterTypes) {
    if (parameterTypes == null || parameterTypes.isEmpty()) {
      return List.of();
    }
    // TODO
    return List.copyOf(parameterTypes);
  }
  
  private static final TypeMirror validateReceiverType(final TypeMirror receiverType) {
    if (receiverType == null) {
      return null;
    }
    switch (receiverType.getKind()) {
    // TODO: this is it, right?
    case DECLARED:
      return receiverType;
    default:
      throw new IllegalArgumentException("receiverType: " + receiverType);
    }
  }

  private static final TypeMirror validateReturnType(final TypeMirror returnType) {
    switch (returnType.getKind()) {
    case ARRAY:
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DECLARED:
    case DOUBLE:
    case FLOAT:
    case INT:
    case INTERSECTION:
    case LONG:
    case SHORT:
    case TYPEVAR:
    case VOID:
      return returnType;
    default:
      throw new IllegalArgumentException("returnType: " + returnType);
    }
  }

  private static final <T extends TypeMirror> List<T> validateThrownTypes(final List<T> thrownTypes) {
    if (thrownTypes == null || thrownTypes.isEmpty()) {
      return List.of();
    }
    // TODO
    return List.copyOf(thrownTypes);
  }
  
}
