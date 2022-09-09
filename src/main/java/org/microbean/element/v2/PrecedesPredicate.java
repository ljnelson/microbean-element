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

import java.util.Objects;

import java.util.function.BiPredicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

// https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java#L829-L849
final class PrecedesPredicate implements BiPredicate<Element, Element> {

  private final SupertypeVisitor supertypeVisitor;

  private final SubtypeVisitor subtypeVisitor;

  PrecedesPredicate(final SupertypeVisitor supertypeVisitor,
                    final SubtypeVisitor subtypeVisitor) {
    super();
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
    this.subtypeVisitor = Objects.requireNonNull(subtypeVisitor, "subtypeVisitor");
  }

  // Does e precede f?
  @Override
  public final boolean test(final Element e, final Element f) {
    if (e == f) {
      // Optimization
      return false;
    }
    final TypeMirror t = e.asType();
    final TypeMirror s = f.asType();
    switch (t.getKind()) {
    case DECLARED:
      switch (s.getKind()) {
      case DECLARED:
        if (Equality.equals(e, f, true)) {
          // Both are DeclaredTypes; can't say which comes first.
          return false;
        }
        final int rt = this.rank(t);
        final int rs = this.rank(s);
        return
          rs < rt ||
          rs == rt &&
          CharSequence.compare(((TypeElement)f).getQualifiedName(),
                               ((TypeElement)e).getQualifiedName()) < 0;
      default:
        return false;
      }
    case TYPEVAR:
      switch (s.getKind()) {
      case TYPEVAR:
        if (Equality.equals(e, f, true)) {
          // Both are TypeVariables; can't say which comes first.
          return false;
        }
        return this.subtypeVisitor.withCapture(true).visit(t, s);
      default:
        // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Symbol.java#L833:
        // "Type variables always precede other kinds of symbols."
        // (Note that a type variable is not a symbol; I think javac
        // means "type variables always precede other kinds of
        // types".)
        return true;
      }
    default:
      return false;
    }
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3576-L3621
  @SuppressWarnings("fallthrough")
  private final int rank(final TypeMirror t) {
    switch (t.getKind()) {
    case DECLARED:
      if (((TypeElement)((DeclaredType)t).asElement()).getQualifiedName().contentEquals("java.lang.Object")) {
        return 0;
      }
      // fall through
    case INTERSECTION:
    case TYPEVAR:
      int r = this.rank(this.supertypeVisitor.visit(t)); // RECURSIVE
      for (final TypeMirror iface : this.supertypeVisitor.interfacesVisitor().visit(t)) {
        r = Math.max(r, this.rank(iface)); // RECURSIVE
      }
      return r + 1;
    case ERROR:
    case NONE:
      return 0;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

}
