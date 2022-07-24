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
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

public class AbstractTypeMirror extends AbstractAnnotatedConstruct implements TypeMirror {

  private final TypeKind kind;

  // com.sun.tools.javac.code -> javax.lang.model.type
  // "Symbol" --> "Element"
  // "Tag" --> "TypeKind"
  // "Type" --> "TypeMirror"
  // "TypeMapping" --> "TypeVisitor"
  
  public AbstractTypeMirror(final TypeKind kind, final List<? extends AnnotationMirror> annotationMirrors) {
    super(annotationMirrors);
    this.kind = Objects.requireNonNull(kind, "kind");
  }

  @Override // TypeMirror
  public final TypeKind getKind() {
    return this.kind;
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    switch (this.getKind()) {

    case ARRAY:
      return v.visitArray((ArrayType)this, p);

    case DECLARED:
      return v.visitDeclared((DeclaredType)this, p);

    case ERROR:
      return v.visitError((ErrorType)this, p);

    case EXECUTABLE:
      return v.visitExecutable((ExecutableType)this, p);

    case INTERSECTION:
      return v.visitIntersection((IntersectionType)this, p);

    case MODULE:
    case NONE:
    case PACKAGE:
    case VOID:
      return v.visitNoType((NoType)this, p);

    case NULL:
      return v.visitNull((NullType)this, p);

    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      return v.visitPrimitive((PrimitiveType)this, p);

    case TYPEVAR:
      return v.visitTypeVariable((TypeVariable)this, p);

    case UNION:
      return v.visitUnion((UnionType)this, p);

    case WILDCARD:
      return v.visitWildcard((WildcardType)this, p);

    case OTHER:
    default:
      return v.visitUnknown(this, p);

    }
  }

  @Override // TypeMirror
  public int hashCode() {
    return Equality.hashCode(this, true);
  }
  
  @Override // TypeMirror
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof TypeMirror tm) { // instanceof on purpose
      return Equality.equals(this, tm, true);
    } else {
      return false;
    }
  }

  public static AbstractTypeMirror of(final TypeMirror t) {
    if (t == null) {
      return null;
    } else if (t instanceof AbstractTypeMirror atm) {
      return atm;
    }
    switch (t.getKind()) {
    case ARRAY:
      final ArrayType at = (ArrayType)t;
      return
        DefaultArrayType.of(at.getComponentType(),
                            at.getAnnotationMirrors());
    case DECLARED:
      final DeclaredType dt = (DeclaredType)t;
      return
        DefaultDeclaredType.of(dt.getEnclosingType(),
                               dt.getTypeArguments(),
                               dt.getAnnotationMirrors());
    case EXECUTABLE:
      final ExecutableType et = (ExecutableType)t;
      return
        DefaultExecutableType.of(et.getParameterTypes(),
                                 et.getReceiverType(),
                                 et.getReturnType(),
                                 et.getThrownTypes(),
                                 et.getTypeVariables(),
                                 et.getAnnotationMirrors());
    case INTERSECTION:
      final IntersectionType it = (IntersectionType)t;
      return
        DefaultIntersectionType.of(it.getBounds());

    case MODULE:
        return DefaultNoType.MODULE;
    case NONE:
      return DefaultNoType.NONE;
    case PACKAGE:
      return DefaultNoType.PACKAGE;
    case VOID:
      return DefaultNoType.VOID;

    case NULL:
      return DefaultNullType.INSTANCE;

    case BOOLEAN:
      return DefaultPrimitiveType.BOOLEAN;
    case BYTE:
      return DefaultPrimitiveType.BYTE;
    case CHAR:
      return DefaultPrimitiveType.CHAR;
    case DOUBLE:
      return DefaultPrimitiveType.DOUBLE;
    case FLOAT:
      return DefaultPrimitiveType.FLOAT;
    case INT:
      return DefaultPrimitiveType.INT;
    case LONG:
      return DefaultPrimitiveType.LONG;
    case SHORT:
      return DefaultPrimitiveType.SHORT;

    case TYPEVAR:
      final TypeVariable tv = (TypeVariable)t;
      return
        DefaultTypeVariable.of(tv.getUpperBound(),
                               tv.getLowerBound(),
                               tv.getAnnotationMirrors());
    case WILDCARD:
      final WildcardType w = (WildcardType)t;
      return
        DefaultWildcardType.of(w.getExtendsBound(),
                               w.getSuperBound(),
                               w.getAnnotationMirrors());

    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }
  
  public static AbstractTypeMirror of(final Type t) {
    if (t == null) {
      return null;
    } else if (t instanceof Class<?> c) {
      return AbstractTypeMirror.of(c);
    } else if (t instanceof ParameterizedType p) {
      return DefaultDeclaredType.of(p);
    } else if (t instanceof GenericArrayType g) {
      return DefaultArrayType.of(g);
    } else if (t instanceof java.lang.reflect.TypeVariable<?> tv) {
      return DefaultTypeVariable.of(tv);
    } else if (t instanceof java.lang.reflect.WildcardType w) {
      return DefaultWildcardType.of(w);
    } else {
      throw new IllegalArgumentException("t: " + t);
    }
  }
  
  public static AbstractTypeMirror of(final Class<?> c) {
    if (c == null) {
      return null;
    } else if (c == void.class) {
      return DefaultNoType.VOID;
    } else if (c.isArray()) {
      return DefaultArrayType.of(c);
    } else if (c.isPrimitive()) {
      return DefaultPrimitiveType.of(c);
    } else {
      return DefaultDeclaredType.of(c);
    }
  }
  
  public static AbstractTypeMirror of(final AnnotatedType t) {
    if (t == null) {
      return null;
    } else if (t instanceof AnnotatedParameterizedType p) {
      return DefaultDeclaredType.of(p);
    } else if (t instanceof AnnotatedArrayType a) {
      return DefaultArrayType.of(a);
    } else if (t instanceof AnnotatedTypeVariable tv) {
      return DefaultTypeVariable.of(tv);
    } else if (t instanceof AnnotatedWildcardType w) {
      return DefaultWildcardType.of(w);
    } else if (t.getType() instanceof Class<?> c) {
      return AbstractTypeMirror.of(c);
    } else {
      throw new IllegalArgumentException("t: " + t);
    }
  }

}
