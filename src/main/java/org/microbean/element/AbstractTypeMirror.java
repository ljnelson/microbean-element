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

}
