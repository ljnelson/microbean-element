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

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

public sealed class DefaultDeclaredType extends DefineableType<TypeElement> implements DeclaredType permits DefaultErrorType {


  /*
   * Instance fields.
   */


  private TypeMirror enclosingType;

  // ArrayType, DeclaredType, ErrorType, TypeVariable, WildcardType
  private final List<? extends TypeMirror> typeArguments;

  // See
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1197-L1200
  private final boolean erased;


  /*
   * Constructors.
   */


  public DefaultDeclaredType() {
    this(TypeKind.DECLARED, null, List.of(), false, List.of());
  }

  public DefaultDeclaredType(final TypeMirror enclosingType) {
    this(TypeKind.DECLARED, enclosingType, List.of(), false, List.of());
  }

  public DefaultDeclaredType(final List<? extends TypeMirror> typeArguments) {
    this(TypeKind.DECLARED, null, typeArguments, false, List.of());
  }

  public DefaultDeclaredType(final TypeMirror enclosingType,
                             final List<? extends TypeMirror> typeArguments) {
    this(TypeKind.DECLARED, enclosingType, typeArguments, false, List.of());
  }

  public DefaultDeclaredType(final TypeMirror enclosingType,
                             final List<? extends TypeMirror> typeArguments,
                             final List<? extends AnnotationMirror> annotationMirrors) {
    this(TypeKind.DECLARED, enclosingType, typeArguments, false, annotationMirrors);
  }

  DefaultDeclaredType(final TypeKind kind) { // kind could be ERROR
    this(kind, null, List.of(), false, List.of());
  }

  DefaultDeclaredType(final TypeMirror enclosingType,
                      final List<? extends TypeMirror> typeArguments,
                      final boolean erased,
                      final List<? extends AnnotationMirror> annotationMirrors) {
    this(TypeKind.DECLARED, enclosingType, typeArguments, erased, annotationMirrors);
  }

  DefaultDeclaredType(final TypeElement definingElement,
                      final List<? extends TypeMirror> typeArguments) {
    this(definingElement, null, typeArguments, List.of());
  }
  
  DefaultDeclaredType(final TypeElement definingElement,
                      final TypeMirror enclosingType,
                      final List<? extends TypeMirror> typeArguments,
                      final List<? extends AnnotationMirror> annotationMirrors) {
    this(TypeKind.DECLARED, enclosingType, typeArguments, false, annotationMirrors);
    this.setDefiningElement(definingElement);
  }

  private DefaultDeclaredType(final TypeKind kind,
                              final TypeMirror enclosingType,
                              final List<? extends TypeMirror> typeArguments,
                              final boolean erased,
                              final List<? extends AnnotationMirror> annotationMirrors) {
    super(validateKind(kind), erased ? List.of() : annotationMirrors);
    this.erased = erased;
    this.enclosingType = validateEnclosingType(enclosingType == null ? DefaultNoType.NONE : enclosingType);
    this.typeArguments = validateTypeArguments(erased || typeArguments == null || typeArguments.isEmpty() ? List.of() : List.copyOf(typeArguments));
  }


  /*
   * Instance methods.
   */


  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitDeclared(this, p);
  }

  @Override // DefineableType<TypeElement>
  DefaultDeclaredType definedBy(final TypeElement e) {
    return (DefaultDeclaredType)super.definedBy(e);
  }
  
  @Override // DefineableType<TypeElement>
  final TypeElement validateDefiningElement(final TypeElement e) {
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      break;
    default:
      throw new IllegalArgumentException("e: " + e);
    }
    final DeclaredType elementType = (DeclaredType)e.asType();
    assert elementType != null : "null elementType for e: " + e;
    if (this != elementType) {
      // We are a parameterized type, i.e. a type usage,
      // i.e. the-type-denoted-by-Set<String>
      // vs. the-type-denoted-by-Set<E>
      final int size = this.getTypeArguments().size();
      if (size > 0 && size != elementType.getTypeArguments().size()) {
        // We aren't a raw type (size > 0) and our type arguments
        // aren't of the required size, so someone passed a bad
        // defining element.
        throw new IllegalArgumentException("e: " + e);
      }
    }
    return e;
  }

  final boolean erased() {
    return this.erased;
  }

  @Override // DeclaredType
  public final TypeMirror getEnclosingType() {
    return this.enclosingType;
  }

  final DefaultDeclaredType withEnclosingType(final TypeMirror enclosingType) {
    return withEnclosingType(this, enclosingType);
  }

  final DefaultDeclaredType withTypeArguments(final List<? extends TypeMirror> typeArguments) {
    return withTypeArguments(this, typeArguments);
  }

  @Override // DeclaredType
  public final List<? extends TypeMirror> getTypeArguments() {
    return this.typeArguments;
  }


  /*
   * Static methods.
   */


  private static final TypeKind validateKind(final TypeKind kind) {
    switch (kind) {
    case DECLARED:
    case ERROR:
      return kind;
    default:
      throw new IllegalArgumentException("kind: " + kind);
    }
  }

  private static final <T extends TypeMirror> T validateEnclosingType(final T t) {
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

  public static final DefaultDeclaredType withTypeArguments(final DeclaredType t,
                                                            final List<? extends TypeMirror> typeArguments) {
    return
      new DefaultDeclaredType(t.getEnclosingType(),
                              validateTypeArguments(typeArguments),
                              t.getAnnotationMirrors())
      .definedBy((TypeElement)t.asElement());      
  }
  
  public static final DefaultDeclaredType withEnclosingType(final DeclaredType t,
                                                            final TypeMirror enclosingType) {
    return
      new DefaultDeclaredType(validateEnclosingType(enclosingType),
                              t.getTypeArguments(),
                              t.getAnnotationMirrors())
      .definedBy((TypeElement)t.asElement());
  }

}
