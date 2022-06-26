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

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.GenericArrayType;

import java.util.List;
import java.util.Objects;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

public class DefaultArrayType extends AbstractReferenceType implements ArrayType {

  private final TypeMirror componentType;
  
  protected DefaultArrayType(final TypeMirror componentType,
                             final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(TypeKind.ARRAY, annotationMirrorsSupplier);
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

  public static DefaultArrayType of(final TypeMirror componentType) {
    return DefaultArrayType.of(componentType, null);
  }
  
  public static DefaultArrayType of(final TypeMirror componentType,
                                    final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    return new DefaultArrayType(componentType, annotationMirrorsSupplier);
  }

  public static DefaultArrayType of(final Class<?> c) {
    final Class<?> ct = c.getComponentType();
    if (ct == null) {
      throw new IllegalArgumentException("c: " + c);
    } else if (ct.isArray()) {
      return DefaultArrayType.of(DefaultArrayType.of(ct));
    } else if (ct.isPrimitive()) {
      return DefaultArrayType.of(DefaultPrimitiveType.of(ct));
    } else {
      return DefaultArrayType.of(DefaultDeclaredType.of(ct));
    }
  }

  public static DefaultArrayType of(final GenericArrayType t) {
    return of(DefaultDeclaredType.of(t.getGenericComponentType()));
  }
  
  public static DefaultArrayType of(final AnnotatedArrayType t) {
    return of(DefaultDeclaredType.of(t.getAnnotatedGenericComponentType()));
  }
  
}
