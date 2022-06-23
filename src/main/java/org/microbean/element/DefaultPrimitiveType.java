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

import java.lang.reflect.Type;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

public class DefaultPrimitiveType extends AbstractTypeMirror implements PrimitiveType {

  public static final DefaultPrimitiveType BOOLEAN = new DefaultPrimitiveType(TypeKind.BOOLEAN, null);

  public static final DefaultPrimitiveType BYTE = new DefaultPrimitiveType(TypeKind.BYTE, null);

  public static final DefaultPrimitiveType CHAR = new DefaultPrimitiveType(TypeKind.CHAR, null);

  public static final DefaultPrimitiveType DOUBLE = new DefaultPrimitiveType(TypeKind.DOUBLE, null);

  public static final DefaultPrimitiveType FLOAT = new DefaultPrimitiveType(TypeKind.FLOAT, null);

  public static final DefaultPrimitiveType INT = new DefaultPrimitiveType(TypeKind.INT, null);

  public static final DefaultPrimitiveType LONG = new DefaultPrimitiveType(TypeKind.LONG, null);

  public static final DefaultPrimitiveType SHORT = new DefaultPrimitiveType(TypeKind.SHORT, null);

  private static final Map<Type, DefaultPrimitiveType> defaultPrimitiveTypes =
    Map.of(boolean.class, BOOLEAN,
           byte.class, BYTE,
           char.class, CHAR,
           double.class, DOUBLE,
           float.class, FLOAT,
           int.class, INT,
           long.class, LONG,
           short.class, SHORT);
  
  public DefaultPrimitiveType(final TypeKind kind) {
    this(kind, null);
  }
  
  public DefaultPrimitiveType(final TypeKind kind,
                              final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(kind, annotationMirrorsSupplier);
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
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitPrimitive(this, p);
  }

  public static DefaultPrimitiveType of(final Class<?> c) {
    final DefaultPrimitiveType returnValue = defaultPrimitiveTypes.get(c);
    if (returnValue == null) {
      throw new IllegalArgumentException("c: " + c);
    }
    return returnValue;
  }
  
}
