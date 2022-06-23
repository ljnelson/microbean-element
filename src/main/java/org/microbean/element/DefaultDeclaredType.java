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

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;

import java.util.ArrayList;
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

public class DefaultDeclaredType extends AbstractReferenceType implements DeclaredType {

  static final DefaultDeclaredType JAVA_IO_SERIALIZABLE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_BOOLEAN = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_BYTE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_CHARACTER = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_CLONEABLE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_COMPARABLE_BOOLEAN =
    new DefaultDeclaredType(List.of(JAVA_LANG_BOOLEAN),
                            null); // no annotations

  static final DefaultDeclaredType JAVA_LANG_CONSTANT_CONSTABLE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_CONSTANT_CONSTANTDESC = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_DOUBLE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_FLOAT = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_INTEGER = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_LONG = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_NUMBER = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_OBJECT = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_SHORT = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_VOID = new DefaultDeclaredType();
  
  private final TypeMirror enclosingType;

  private Element definingElement;

  private final List<? extends TypeMirror> typeArguments;

  public DefaultDeclaredType() {
    this(DefaultNoType.NONE, List.of(), null);
  }

  public DefaultDeclaredType(final TypeMirror soleTypeArgument) {
    this(DefaultNoType.NONE, List.of(soleTypeArgument), null);
  }
  
  public DefaultDeclaredType(final List<? extends TypeMirror> typeArguments) {
    this(DefaultNoType.NONE, typeArguments, null);
  }
  
  public DefaultDeclaredType(final List<? extends TypeMirror> typeArguments,
                             final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    this(DefaultNoType.NONE, typeArguments, annotationMirrorsSupplier);
  }

  public DefaultDeclaredType(final TypeMirror enclosingType,
                             final List<? extends TypeMirror> typeArguments,
                             final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(TypeKind.DECLARED, annotationMirrorsSupplier);
    if (enclosingType == null) {
      this.enclosingType = DefaultNoType.NONE;
    } else {
      switch (enclosingType.getKind()) {
      case DECLARED:
      case NONE:
        this.enclosingType = enclosingType;
        break;
      default:
        throw new IllegalArgumentException("enclosingType: " + enclosingType);
      }
    }
    this.typeArguments = typeArguments == null || typeArguments.isEmpty() ? List.of() : List.copyOf(typeArguments);
  }

  final void element(final Element e) {
    if (this.asElement() != null) {
      throw new IllegalStateException();
    } else if (e.asType() != this) {
      // Wildcard capture makes this impossible.
      // throw new IllegalArgumentException("e: " + e);
    }
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

  public static final DefaultDeclaredType of(final AnnotatedType t) {    
    if (t instanceof AnnotatedParameterizedType p) {
      return of(p);
    }
    throw new IllegalArgumentException("t: " + t);
  }
  
  public static final DefaultDeclaredType of(final Class<?> c) {
    if (c == void.class || c.isArray() || c.isPrimitive()) {
      throw new IllegalArgumentException("c: " + c);
    }
    final Class<?> enclosingClass = c.getEnclosingClass();
    final TypeMirror enclosingType = enclosingClass == null ? null : of(enclosingClass);
    final java.lang.reflect.TypeVariable<?>[] typeParameters = c.getTypeParameters();
    final List<TypeVariable> typeArguments;
    if (typeParameters.length <= 0) {
      typeArguments = List.of();
    } else {
      typeArguments = new ArrayList<>(typeParameters.length);
      for (final java.lang.reflect.TypeVariable<?> t : typeParameters) {
        typeArguments.add(DefaultTypeVariable.of(t));
      }
    }
    return new DefaultDeclaredType(enclosingType, typeArguments, null);
  }

  public static final DefaultDeclaredType of(final AnnotatedParameterizedType p) {
    final AnnotatedType ownerType = p.getAnnotatedOwnerType();
    final TypeMirror enclosingType = ownerType == null ? null : of(ownerType);
    final List<TypeMirror> typeArguments;
    final AnnotatedType[] actualTypeArguments = p.getAnnotatedActualTypeArguments();
    if (actualTypeArguments.length <= 0) {
      typeArguments = List.of();
    } else {
      typeArguments = new ArrayList<>(actualTypeArguments.length);
      for (final AnnotatedType t : actualTypeArguments) {
        typeArguments.add(AbstractTypeMirror.of(t));
      }
    }
    return new DefaultDeclaredType(enclosingType, typeArguments, null);
  }

}
