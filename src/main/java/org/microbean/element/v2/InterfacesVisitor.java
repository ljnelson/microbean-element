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
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import javax.lang.model.util.SimpleTypeVisitor14;

// Basically done
final class InterfacesVisitor extends SimpleTypeVisitor14<List<? extends TypeMirror>, Void> {

  private final Types2 types2;

  private final EraseVisitor eraseVisitor;

  private final SupertypeVisitor supertypeVisitor;
  
  InterfacesVisitor(final Types2 types2,
                    final EraseVisitor eraseVisitor,
                    final SupertypeVisitor supertypeVisitor) {
    super(List.of());
    this.types2 = Objects.requireNonNull(types2, "types2");
    this.eraseVisitor = Objects.requireNonNull(eraseVisitor, "eraseVisitor");
    this.supertypeVisitor = Objects.requireNonNull(supertypeVisitor, "supertypeVisitor");
  }

  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2599-L2633
  @Override
  public final List<? extends TypeMirror> visitDeclared(final DeclaredType t, final Void x) {
    assert t.getKind() == TypeKind.DECLARED;
    final Element e = t.asElement();
    if (e == null) {
      return List.of();
    }
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      final List<? extends TypeMirror> interfaces = ((TypeElement)e).getInterfaces();
      if (this.types2.raw(t)) {
        return this.eraseVisitor.visit(interfaces, true);
      }
      @SuppressWarnings("unchecked")
      final List<? extends TypeVariable> formals = (List<? extends TypeVariable>)this.types2.allTypeArguments(this.types2.declaredTypeMirror(t));
      if (formals.isEmpty()) {
        return interfaces;
      }
      return new SubstituteVisitor(this.supertypeVisitor, this, formals, this.types2.allTypeArguments(t)).visit(interfaces, x);
    default:
      return List.of();
    }
  }

  @Override
  public final List<? extends TypeMirror> visitIntersection(final IntersectionType t, final Void x) {
    assert t.getKind() == TypeKind.INTERSECTION;
    // Here the porting is a little trickier.  It turns out that an
    // intersection type caches its supertype and its interfaces at
    // construction time, and there's only one place where
    // intersection types are created.  In the lang model, that means
    // that an IntersectionType's bounds are its supertype followed by
    // its interfaces.  So we will hand-tool this.
    final List<? extends TypeMirror> bounds = t.getBounds();
    final int size = bounds.size();
    switch (size) {
    case 0:
      // (Technically an illegal state.)
      return List.of();
    case 1:
      return Types2.isInterface(bounds.get(0)) ? bounds : List.of();
    default:
      return Types2.isInterface(bounds.get(0)) ? bounds : bounds.subList(1, size);
    }
  }

  @Override
  public final List<? extends TypeMirror> visitTypeVariable(final TypeVariable t, final Void x) {
    assert t.getKind() == TypeKind.TYPEVAR;
    final TypeMirror upperBound = t.getUpperBound();
    switch (upperBound.getKind()) {
    case DECLARED:
      return ((DeclaredType)upperBound).asElement().getKind().isInterface() ? List.of(upperBound) : List.of();
    case INTERSECTION:
      return this.visitIntersection((IntersectionType)upperBound, x);
    default:
      return List.of();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }
  
}
