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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

/*
 * <p>From the documentation of {@code
 * com.sun.tools.javac.code.Types#containsType(Type, Type)}:</p>
 * 
 * <blockquote><p>Check if {@code t} contains {@code s}.</p>
 *
 * <p>{@code T} contains {@code S} if:</p>
 *
 * <p>{@code L(T) <: L(S) && U(S) <: U(T)}</p>
 *
 * <p>This relation is only used by {@code ClassType.isSubtype()} [in
 * fact this is not true], that is,</p>
 *
 * <p>{@code C<S> <: C<T> if T contains S.}</p>
 *
 * <p>Because of F-bounds, this relation can lead to infinite
 * recursion.  Thus we must somehow break that recursion.  Notice that
 * containsType() is only called from ClassType.isSubtype() [not
 * true].  Since the arguments have already been checked against their
 * bounds [not true], we know:</p>
 *
 * <p>{@code U(S) <: U(T) if T is "super" bound (U(T) *is* the
 * bound)}</p>
 *
 * <p>{@code L(T) <: L(S) if T is "extends" bound (L(T) is
 * bottom)}</p></blockquote>
 *
 * @see <a
 * href="https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-4.5.1">Type
 * Arguments of Parameterized Types</a>
 *
 * @see <a
 * href="https://en.wikipedia.org/wiki/Bounded_quantification#F-bounded_quantification">F-bounded
 * quantification</a>
 */
// https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1562-L1611
final class ContainsTypeVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  IsSameTypeVisitor isSameTypeVisitor;

  SubtypeVisitor subtypeVisitor;

  ContainsTypeVisitor() {
    super(Boolean.FALSE);
  }
  
  ContainsTypeVisitor(final IsSameTypeVisitor isSameTypeVisitor) {
    super(Boolean.FALSE);
    this.isSameTypeVisitor = Objects.requireNonNull(isSameTypeVisitor);
  }

  final boolean visit(final List<? extends TypeMirror> t, final List<? extends TypeMirror> s) {
    final Iterator<? extends TypeMirror> tIterator = t.iterator();
    final Iterator<? extends TypeMirror> sIterator = s.iterator();
    while (tIterator.hasNext() && sIterator.hasNext() && this.visit(tIterator.next(), sIterator.next())) {
      // do nothing
    }
    if (tIterator.hasNext() || sIterator.hasNext()) {
      return false;
    }
    return true;
  }
  
  @Override
  protected final Boolean defaultAction(final TypeMirror t, final TypeMirror s) {
    return this.isSameTypeVisitor().visit(t, s);
  }

  @Override
  public final Boolean visitError(final ErrorType e, final TypeMirror s) {
    assert e.getKind() == TypeKind.ERROR;
    return Boolean.TRUE;
  }

  @Override
  public final Boolean visitWildcard(final WildcardType w, final TypeMirror s) {    
    assert w.getKind() == TypeKind.WILDCARD;
    if (isSameTypeVisitor().visitWildcard(w, s) || captures(s, w)) {
      return true;
    }
    // TODO: subtype visitor with capture turned off
    throw new UnsupportedOperationException();
  }

  // Does tv capture w?  Is tv a capture of w?
  //
  // https://github.com/openjdk/jdk/blob/jdk-20+12/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1613-L1617
  private final boolean captures(final TypeMirror tv, final WildcardType w) {
    assert w.getKind() == TypeKind.WILDCARD;
    return
      tv.getKind() == TypeKind.TYPEVAR &&
      tv instanceof SyntheticCapturedType ct &&
      isSameTypeVisitor().visit(w, ct.getWildcardType());
  }

  private final IsSameTypeVisitor isSameTypeVisitor() {
    final IsSameTypeVisitor isSameTypeVisitor = this.isSameTypeVisitor;
    if (isSameTypeVisitor == null) {
      throw new IllegalStateException("this.isSameTypeVisitor == null");
    }
    return isSameTypeVisitor;
  }

  
}
