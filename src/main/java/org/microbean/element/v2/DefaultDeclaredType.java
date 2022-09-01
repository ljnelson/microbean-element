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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

public class DefaultDeclaredType extends AbstractTypeMirror implements DefineableType {

  private TypeMirror enclosingType;

  private Element definingElement;

  private final List<? extends TypeMirror> typeArguments;

  // See
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1197-L1200
  private final boolean erased;

  public DefaultDeclaredType() {
    this(null, List.of(), false, List.of());
  }
  
  public DefaultDeclaredType(final TypeMirror enclosingType,
                             final List<? extends TypeMirror> typeArguments,
                             final List<? extends AnnotationMirror> annotationMirrors) {
    this(enclosingType, typeArguments, false, annotationMirrors);
  }

  DefaultDeclaredType(final TypeMirror enclosingType,
                      final List<? extends TypeMirror> typeArguments,
                      final boolean erased,
                      final List<? extends AnnotationMirror> annotationMirrors) {
    super(TypeKind.DECLARED, erased ? List.of() : annotationMirrors);
    this.erased = erased;
    this.enclosingType = validateEnclosingType(enclosingType);
    this.typeArguments = validateTypeArguments(erased || typeArguments == null || typeArguments.isEmpty() ? List.of() : List.copyOf(typeArguments));
  }

  final DefaultDeclaredType withEnclosingType(final TypeMirror enclosingType) {
    return withEnclosingType(this, enclosingType);
  }
  
  final boolean erased() {
    return this.erased;
  }
  
  @Override // DefineableType
  public final void setDefiningElement(final Element e) {
    if (this.asElement() != null) {
      throw new IllegalStateException();
    }
    if (e != null) {
      switch (e.getKind()) {
      case ANNOTATION_TYPE:
      case CLASS:
      case ENUM:
      case INTERFACE:
      case RECORD:
        this.definingElement = e;
        break;
      default:
        throw new IllegalArgumentException("e: " + e);
      }
    }
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitDeclared(this, p);
  }

  @Override // DeclaredType
  public final Element asElement() {
    return this.definingElement;
  }

  @Override // DeclaredType
  public final TypeMirror getEnclosingType() {
    return this.enclosingType;
  }

  @Override // DeclaredType
  public final List<? extends TypeMirror> getTypeArguments() {
    return this.typeArguments;
  }


  /*
   * Static methods.
   */


  private static final <T extends TypeMirror> T validateEnclosingType(final T t) {
    if (t == null) {
      return null;
    }
    switch (t.getKind()) {
    case DECLARED:
    case NONE:
      return t;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final List<? extends TypeMirror> validateTypeArguments(final List<? extends TypeMirror> typeArguments) {
    for (final TypeMirror t : typeArguments) {
      switch (t.getKind()) {
      case ARRAY:
      case DECLARED:
      // case INTERSECTION: // JLS says reference types and wildcards only
      case TYPEVAR:
      case WILDCARD:
        break;
      default:
        throw new IllegalArgumentException("typeArguments: " + typeArguments + "; invalid type argument: " + t);
      }
    }
    return typeArguments;
  }

  public static final DefaultDeclaredType of(final DeclaredType t) {
    if (t instanceof DefaultDeclaredType defaultDeclaredType) {
      return defaultDeclaredType;
    }
    return
      new DefaultDeclaredType(t.getEnclosingType(),
                              t.getTypeArguments(),
                              t.getAnnotationMirrors());
  }

  public static final DefaultDeclaredType withEnclosingType(final DeclaredType t,
                                                            final TypeMirror enclosingType) {
    final DefaultDeclaredType r =
      new DefaultDeclaredType(enclosingType,
                              t.getTypeArguments(),
                              t.getAnnotationMirrors());
    r.setDefiningElement(t.asElement());
    return r;
  }

}
