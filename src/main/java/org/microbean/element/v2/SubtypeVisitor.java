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

import javax.lang.model.element.Name;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import javax.lang.model.util.SimpleTypeVisitor14;

// See https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1109
final class SubtypeVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  IsSameTypeVisitor isSameTypeVisitor;

  private final Types2 types2;

  private final boolean capture;
  
  SubtypeVisitor(final Types2 types2, final boolean capture) {
    super(Boolean.FALSE);
    this.types2 = types2;
    this.capture = capture;
  }

  // Is t a subtype of s?
  //
  // See
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1200-L1217
  //
  // See also:
  // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1082-L1107
  @Override
  public final Boolean visitArray(final ArrayType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ARRAY;
    final TypeKind sKind = s.getKind();
    switch (sKind) {
    case ARRAY:
      final TypeMirror tct = t.getComponentType();
      final TypeMirror sct = ((ArrayType)s).getComponentType();      
      if (tct.getKind().isPrimitive()) {
        return isSameTypeVisitor.visit(tct, sct);
      } else if (this.capture) {
        return new SubtypeVisitor(this.types2, false).visit(tct, sct);
      } else {
        return this.visit(tct, sct);
      }
    case DECLARED:
      // See
      // https://github.com/openjdk/jdk/blob/jdk-20+11/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1211-L1213
      // for better or for worse
      final Name sName = ((QualifiedNameable)((DeclaredType)s).asElement()).getQualifiedName();
      return
        sName.contentEquals("java.lang.Object") ||
        sName.contentEquals("java.lang.Cloneable") ||
        sName.contentEquals("java.io.Serializable");
    default:
      return false;
    }
  }

  @Override
  public final Boolean visitIntersection(final IntersectionType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.INTERSECTION;
    return false;
  }

}
