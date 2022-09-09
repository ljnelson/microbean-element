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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.Element;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

final class ClosureVisitor extends SimpleTypeVisitor14<List<TypeMirror>, List<TypeMirror>> {

  private final Types2 types2;

  private final SupertypeVisitor supertypeVisitor;

  private final PrecedesPredicate precedesPredicate;

  ClosureVisitor(final Types2 types2,
                 final SupertypeVisitor supertypeVisitor,
                 final PrecedesPredicate precedesPredicate) {
    super();
    this.types2 = Objects.requireNonNull(types2, "types2");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    this.precedesPredicate = Objects.requireNonNull(precedesPredicate, "precedesPredicate");
  }

  @Override
  protected final List<TypeMirror> defaultAction(final TypeMirror t, List<TypeMirror> rv) {
    return rv == null ? new ArrayList<>() : rv;
  }

  @Override
  public final List<TypeMirror> visitDeclared(final DeclaredType t, List<TypeMirror> rv) {
    assert t.getKind() == TypeKind.DECLARED;
    if (rv == null) {
      rv = new ArrayList<>();
    }

    return rv;
  }

  private final List<TypeMirror> visitReferenceType(final ReferenceType t, List<TypeMirror> rv) {
    final TypeMirror st;
    switch (t.getKind()) {
    case DECLARED:
    case TYPEVAR:
      st = this.supertypeVisitor.visit(t);
      switch (st.getKind()) {
      case DECLARED:
        // (Yes, it is OK that INTERSECTION is not present as a case.)
        if (rv == null) {
          rv = new ArrayList<>();
        }
        throw new UnsupportedOperationException();
      case TYPEVAR:
        throw new UnsupportedOperationException();
      default:
        throw new IllegalArgumentException("t: " + t);
      }
    case INTERSECTION:
      st = this.supertypeVisitor.visit(t);
      return this.visit(st, rv);
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  /*
  private final List<TypeMirror> visitReferenceType(final ReferenceType t) {
    final TypeMirror st = this.supertypeVisitor.visit(t);
    switch (t.getKind()) {
    case INTERSECTION:
      this.visitIntersection((IntersectionType)st, null); // RECURSIVE
      break;
    case DECLARED:
    case TYPEVAR:
      switch (st.getKind()) {
      case DECLARED:
        // (Yes, it is OK that INTERSECTION is not present as a case.)
        cl = closureInsert(closure(st), t); // RECURSIVE
        break;
      case TYPEVAR:
        cl = new ArrayList<>();
        cl.add(t);
        cl.addAll(closure(st)); // RECURSIVE
        break;
      case ERROR:
      case UNION:
      default:
        throw new IllegalArgumentException("t: " + t);
      }
      break;
    case ERROR:
    case UNION:
    default:
      throw new IllegalArgumentException("t: " + t);
    }
    for (final TypeMirror iface : interfaces(t)) {
      cl = closureUnion(cl, closure(iface)); // RECURSIVE
    }
    return cl;
  }
  */
  
}
