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

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

final class ContainsVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  IsSameTypeVisitor isSameTypeVisitor;

  SubtypeVisitor subtypeVisitor;

  ContainsVisitor() {
    super(Boolean.FALSE);
  }
  
  ContainsVisitor(final IsSameTypeVisitor isSameTypeVisitor) {
    super(Boolean.FALSE);
    this.isSameTypeVisitor = Objects.requireNonNull(isSameTypeVisitor);
  }

  @Override
  public final Boolean visitWildcard(final WildcardType w, final TypeMirror s) {    
    assert w.getKind() == TypeKind.WILDCARD;
    if (isSameTypeVisitor().visit(w, s) || captures(s, w)) {
      // if is w is same wildcard as s or s captures w
      return true;
    }
    
    return false;
  }

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
