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

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import javax.lang.model.util.SimpleTypeVisitor14;

// isSameType() in javac's Types.java
final class IsSameTypeVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  private final ContainsTypeVisitor containsVisitor;
  
  IsSameTypeVisitor() {
    super(Boolean.FALSE);
    this.containsVisitor = new ContainsTypeVisitor(this);
  }

  IsSameTypeVisitor(final ContainsTypeVisitor containsVisitor) {
    super(Boolean.FALSE);
    this.containsVisitor = containsVisitor;
    containsVisitor.isSameTypeVisitor = this;
  }

  @Override
  public final Boolean visitArray(final ArrayType t, final TypeMirror s) {
    switch (s.getKind()) {
    case ARRAY:
      return this.visitArray(t, (ArrayType)s);
    default:
      return Boolean.FALSE;
    }
  }

  private final boolean visitArray(final ArrayType t, final ArrayType s) {
    assert t.getKind() == TypeKind.ARRAY;
    assert s.getKind() == TypeKind.ARRAY;
    if (t == s) {
      return true;
    }
    final TypeMirror tct = t.getComponentType();
    final TypeMirror sct = s.getComponentType();
    return
      this.visit(tct, sct) ||
      this.containsVisitor.visit(tct, sct) && this.containsVisitor.visit(sct, tct);
  }
  
}
