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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.Element;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

// Basically done
//
// https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2165-L2221
final class AsSuperVisitor extends SimpleTypeVisitor14<TypeMirror, Element> {

  private final Set<DefaultElement> seenTypes; // in the compiler, the field is called seenTypes and stores Symbols (Elements).

  private final Types2 types2;

  private final SupertypeVisitor supertypeVisitor;

  private final InterfacesVisitor interfacesVisitor;

  private final SubtypeVisitor subtypeVisitor;

  AsSuperVisitor(final Types2 types2,
                 final SupertypeVisitor supertypeVisitor,
                 final InterfacesVisitor interfacesVisitor,
                 final SubtypeVisitor subtypeVisitor) {
    super();
    this.seenTypes = new HashSet<>();
    this.types2 = Objects.requireNonNull(types2, "types2");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    this.interfacesVisitor = Objects.requireNonNull(interfacesVisitor, "interfacesVisitor");
    this.subtypeVisitor = Objects.requireNonNull(subtypeVisitor, "subtypeVisitor");
  }

  @Override
  public final TypeMirror visitArray(final ArrayType t, final Element sym) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeMirror x = sym.asType();
    return this.subtypeVisitor.withCapture(true).visit(t, x) ? x : null;
  }

  @Override
  public final TypeMirror visitDeclared(final DeclaredType t, final Element sym) {
    assert t.getKind() == TypeKind.DECLARED;
    return this.visitDeclaredOrIntersection(t, sym);
  }

  private final TypeMirror visitDeclaredOrIntersection(final TypeMirror t, final Element sym) {
    assert t.getKind() == TypeKind.DECLARED || t.getKind() == TypeKind.INTERSECTION;
    final Element te = this.types2.asElement(t, true);
    if (te != null) {
      if (Equality.equals(te, sym, true)) {
        return t;
      }
      final DefaultElement c = DefaultElement.of(te);
      if (this.seenTypes.add(c)) {
        try {
          final TypeMirror st = this.supertypeVisitor.visit(t);
          switch (st.getKind()) {
          case DECLARED:
          case INTERSECTION:
          case TYPEVAR:
            final TypeMirror x = this.visit(st, sym);
            if (x != null) {
              return x;
            }
            break;
          default:
            break;
          }
          if (sym.getKind().isInterface()) {
            for (final TypeMirror iface : this.interfacesVisitor.visit(t)) {
              final TypeMirror x = this.visit(iface, sym);
              if (x != null) {
                return x;
              }
            }
          }
        } finally {
          this.seenTypes.remove(c);
        }
      }
    }
    return null;
  }

  @Override
  public final TypeMirror visitError(final ErrorType t, final Element sym) {
    assert t.getKind() == TypeKind.ERROR;
    return t;
  }

  @Override
  public final TypeMirror visitIntersection(final IntersectionType t, final Element sym) {
    assert t.getKind() == TypeKind.INTERSECTION;
    return this.visitDeclaredOrIntersection(t, sym);
  }

  @Override
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Element sym) {
    assert t.getKind() == TypeKind.TYPEVAR;
    if (Equality.equals(t.asElement(), sym, true)) {
      return t;
    }
    return this.visit(t.getUpperBound(), sym);
  }




}
