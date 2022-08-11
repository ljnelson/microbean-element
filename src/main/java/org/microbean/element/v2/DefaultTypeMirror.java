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

import java.lang.annotation.Annotation;

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

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

class DefaultTypeMirror implements ArrayType, ErrorType, ExecutableType, IntersectionType, NoType, NullType, PrimitiveType, TypeVariable, UnionType, WildcardType {

  private static final DefaultTypeMirror NONE = new DefaultTypeMirror(TypeKind.NONE);

  private static final DefaultTypeMirror NULL = new DefaultTypeMirror(TypeKind.NULL);

  private static final DefaultTypeMirror JAVA_LANG_OBJECT = new DefaultTypeMirror(TypeKind.DECLARED);
  
  private final List<? extends TypeMirror> typeList;

  private final List<? extends TypeMirror> thrownTypes; // for executable types only

  private final List<? extends TypeVariable> typeVariables; // for executable types only
  
  private Element element; // for declared types and type variables only

  private final TypeKind kind;

  private final List<? extends AnnotationMirror> annotations;

  private final TypeMirror extraType0; // component type, enclosing type, receiver type, extends/upper bound

  private final TypeMirror extraType1; // return type, super/lower bound

  private DefaultTypeMirror(final TypeKind kind) {
    this(kind, List.of(), null, null, List.of(), List.of(), List.of());
  }

  private DefaultTypeMirror(final TypeKind kind,
                            final List<? extends AnnotationMirror> annotations,
                            final TypeMirror extraType0, // component type, enclosing type, receiver type
                            final TypeMirror extraType1, // return type, lower bound, super bound
                            final List<? extends TypeMirror> typeList, /* bounds, alternatives, parameter types, type arguments */
                            final List<? extends TypeMirror> thrownTypes, /* thrown types */
                            final List<? extends TypeVariable> typeVariables) {
    super();
    this.kind = Objects.requireNonNull(kind, "kind");
    this.annotations = annotations == null ? List.of() : List.copyOf(annotations);
    switch (kind) {
    case ARRAY:
      this.extraType0 = extraType0; // componentType
      this.extraType1 = null;
      this.typeList = List.of();
      this.thrownTypes = List.of();
      this.typeVariables = List.of();
      break;
    case DECLARED:
      this.extraType0 = extraType0 == null ? NONE : extraType0; // enclosingType
      this.extraType1 = null;
      this.typeList = List.of();
      this.thrownTypes = List.of();
      this.typeVariables = List.of();
      break;
    case EXECUTABLE:
      this.extraType0 = extraType0; // receiverType
      this.extraType1 = extraType1; // returnType
      this.typeList = typeList == null ? List.of() : List.copyOf(typeList); // parameter types
      this.thrownTypes = thrownTypes == null ? List.of() : List.copyOf(thrownTypes); // thrown types
      this.typeVariables = typeVariables == null ? List.of() : List.copyOf(typeVariables);
      break;
    case INTERSECTION:
      this.extraType0 = null;
      this.extraType1 = null;
      this.typeList = typeList == null ? List.of() : List.copyOf(typeList); // bounds
      this.thrownTypes = List.of();
      this.typeVariables = List.of();
      break;
    case UNION:
      this.extraType0 = null;
      this.extraType1 = null;
      this.typeList = typeList == null ? List.of() : List.copyOf(typeList); // alternatives
      this.thrownTypes = List.of();
      this.typeVariables = List.of();
      break;
    case TYPEVAR:
      this.extraType0 = extraType0 == null ? JAVA_LANG_OBJECT : extraType0; // upper bound
      this.extraType1 = extraType1 == null ? NULL : extraType1; // lower bound
      this.typeList = List.of();
      this.thrownTypes = List.of();
      this.typeVariables = List.of();
      break;
    case WILDCARD:
      this.extraType0 = extraType0; // extendsBound
      this.extraType1 = extraType1; // superBound
      this.typeList = List.of();
      this.thrownTypes = List.of();
      this.typeVariables = List.of();
      break;
    default:
      this.extraType0 = null;
      this.extraType1 = null;
      this.typeList = List.of();
      this.thrownTypes = List.of();
      this.typeVariables = List.of();
      break;
    }
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

  @Override // DeclaredType, TypeVariable
  public Element asElement() {
    switch (this.getKind()) {
    case DECLARED:
    case TYPEVAR:
      return this.element;
    default:
      throw new IllegalStateException();
    }
  }

  public final void element(final Element e) {
    final Element old = this.asElement();
    if (old != null) {
      throw new IllegalStateException();
    }
    switch (this.getKind()) {
    case DECLARED:
    case TYPEVAR:
      this.element = e;
      break;
    default:
      throw new IllegalStateException();
    }
  }

  @Override // UnionType
  public final List<? extends TypeMirror> getAlternatives() {
    switch (this.getKind()) {
    case UNION:
      return this.typeList;
    default:
      throw new IllegalStateException();
    }
  }

  @Override // TypeMirror
  public final <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
    return null; // TODO
  }

  @Override // TypeMirror
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.annotations;
  }

  @Override // TypeMirror
  public final <A extends Annotation> A[] getAnnotationsByType(final Class<A> annotationType) {
    return null; // TODO
  }

  @Override // IntersectionType
  public final List<? extends TypeMirror> getBounds() {
    switch (this.getKind()) {
    case INTERSECTION:
      return this.typeList;
    default:
      throw new IllegalStateException();
    }
  }

  @Override // ArrayType
  public final TypeMirror getComponentType() {
    switch (this.getKind()) {
    case ARRAY:
      return this.extraType0; // componentType
    default:
      throw new IllegalStateException();
    }
  }

  @Override // DeclaredType
  public final TypeMirror getEnclosingType() {
    switch(this.getKind()) {
    case DECLARED:
      return this.extraType0; // enclosingType
    default:
      throw new IllegalStateException();
    }
  }

  @Override // WildcardType
  public final TypeMirror getExtendsBound() {
    switch (this.getKind()) {
    case WILDCARD:
      return this.extraType0; // extends/upper bound
    default:
      throw new IllegalStateException();
    }
  }

  @Override // TypeVariable
  public final TypeMirror getLowerBound() {
    switch (this.getKind()) {
    case TYPEVAR:
      return this.extraType1; // super/lower bound
    default:
      throw new IllegalStateException();
    }
  }

  @Override // TypeVariable
  public final TypeMirror getUpperBound() {
    switch (this.getKind()) {
    case TYPEVAR:
      return this.extraType0; // extends/upper bound
    default:
      throw new IllegalStateException();
    }
  }

  @Override // ExecutableType
  public final List<? extends TypeMirror> getParameterTypes() {
    switch (this.getKind()) {
    case EXECUTABLE:
      return this.typeList; // parameter types
    default:
      throw new IllegalStateException();
    }
  }

  @Override // ExecutableType
  public final TypeMirror getReceiverType() {
    switch (this.getKind()) {
    case EXECUTABLE:
      return this.extraType0; // receiverType
    default:
      throw new IllegalStateException();
    }
  }

  @Override // ExecutableType
  public final TypeMirror getReturnType() {
    switch (this.getKind()) {
    case EXECUTABLE:
      return this.extraType1; // returnType;
    default:
      throw new IllegalStateException();
    }
  }

  @Override // WildcardType
  public final TypeMirror getSuperBound() {
    switch (this.getKind()) {
    case WILDCARD:
      return this.extraType1; // super/lower bound
    default:
      throw new IllegalStateException();
    }
  }

  @Override // ExecutableType
  public final List<? extends TypeMirror> getThrownTypes() {
    switch (this.getKind()) {
    case EXECUTABLE:
      return this.thrownTypes;
    default:
      throw new IllegalStateException();
    }
  }

  @Override // DeclaredType
  public final List<? extends TypeMirror> getTypeArguments() {
    switch (this.getKind()) {
    case DECLARED:
      return this.typeList;
    default:
      throw new IllegalStateException();
    }
  }

  @Override // ExecutableType
  public final List<? extends TypeVariable> getTypeVariables() {
    switch (this.getKind()) {
    case EXECUTABLE:
      return this.typeVariables;
    default:
      throw new IllegalStateException();
    }
  }

  @Override // TypeMirror
  public final int hashCode() {
    return org.microbean.element.Equality.hashCode(this, true);
  }

  @Override // TypeMirror
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof TypeMirror t) { // instanceof on purpose
      return org.microbean.element.Equality.equals(this, t, true);
    } else {
      return false;
    }
  }


  /*
   * Static methods.
   */


  /*
  public static final DefaultTypeMirror of(final TypeMirror t) {
    if (t instanceof DefaultTypeMirror d) {
      return d;
    }
    return new DefaultTypeMirror(t);
  }
  */

  
}
