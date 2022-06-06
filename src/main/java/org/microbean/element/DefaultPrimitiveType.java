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

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

public final class DefaultPrimitiveType extends AbstractTypeMirror implements PrimitiveType {

  public static final DefaultPrimitiveType BOOLEAN = new DefaultPrimitiveType(TypeKind.BOOLEAN, List.of());

  public static final DefaultPrimitiveType BYTE = new DefaultPrimitiveType(TypeKind.BYTE, List.of());

  public static final DefaultPrimitiveType CHAR = new DefaultPrimitiveType(TypeKind.CHAR, List.of());

  public static final DefaultPrimitiveType DOUBLE = new DefaultPrimitiveType(TypeKind.DOUBLE, List.of());

  public static final DefaultPrimitiveType FLOAT = new DefaultPrimitiveType(TypeKind.FLOAT, List.of());

  public static final DefaultPrimitiveType INT = new DefaultPrimitiveType(TypeKind.INT, List.of());

  public static final DefaultPrimitiveType LONG = new DefaultPrimitiveType(TypeKind.LONG, List.of());

  public static final DefaultPrimitiveType SHORT = new DefaultPrimitiveType(TypeKind.SHORT, List.of());
  
  public DefaultPrimitiveType(final TypeKind kind,
                              final List<? extends AnnotationMirror> annotationMirrors) {
    super(kind, annotationMirrors);
    switch (kind) {
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      break;
    default:
      throw new IllegalArgumentException("Not a primitive type kind: " + kind);
    }
  }

  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitPrimitive(this, p);
  }
  
}
