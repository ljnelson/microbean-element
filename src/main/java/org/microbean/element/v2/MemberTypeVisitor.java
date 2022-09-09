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
import java.util.Objects;

import javax.lang.model.element.Element;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

// https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L2294-L2340
final class MemberTypeVisitor extends SimpleTypeVisitor14<TypeMirror, Element> {

  private final Types2 types2;
  
  MemberTypeVisitor(final Types2 types2) {
    super();
    this.types2 = Objects.requireNonNull(types2, "types2");
  }

  @Override
  public final TypeMirror visitArray(final ArrayType t, final Element e) {
    assert t.getKind() == TypeKind.ARRAY;
    throw new UnsupportedOperationException();
  }

  @Override
  public final TypeMirror visitDeclared(final DeclaredType t, final Element e) {
    assert t.getKind() == TypeKind.DECLARED;
    throw new UnsupportedOperationException();
  }

  @Override
  public final TypeMirror visitError(final ErrorType t, final Element e) {
    assert t.getKind() == TypeKind.ERROR;
    return t;
  }

  @Override
  public final TypeMirror visitIntersection(final IntersectionType t, final Element e) {
    assert t.getKind() == TypeKind.INTERSECTION;
    throw new UnsupportedOperationException();
  }

  @Override
  public final TypeMirror visitTypeVariable(final TypeVariable t, final Element e) {
    assert t.getKind() == TypeKind.TYPEVAR;
    throw new UnsupportedOperationException();
  }

  @Override
  public final TypeMirror visitWildcard(final WildcardType t, final Element e) {
    assert t.getKind() == TypeKind.WILDCARD;
    throw new UnsupportedOperationException();
  }


  
}
