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

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

public class DefaultArrayType extends AbstractReferenceType implements ArrayType {

  private final TypeMirror componentType;
  
  private DefaultArrayType(final TypeMirror componentType,
                           final List<? extends AnnotationMirror> annotationMirrors) {
    super(TypeKind.ARRAY, annotationMirrors);
    this.componentType = Objects.requireNonNull(componentType);
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitArray(this, p);
  }
  
  @Override // ArrayType
  public final TypeMirror getComponentType() {
    return this.componentType;
  }

  public static final DefaultArrayType of(final TypeMirror componentType) {
    return of(componentType, List.of());
  }

  public static final DefaultArrayType of(final TypeMirror componentType, final List<? extends AnnotationMirror> annotationMirrors) {
    return new DefaultArrayType(componentType, annotationMirrors);
  }
  
}
