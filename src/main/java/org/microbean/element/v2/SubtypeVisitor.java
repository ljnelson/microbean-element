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
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

// See https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1109-L1238
final class SubtypeVisitor extends SimpleTypeVisitor14<Boolean, TypeMirror> {

  ContainsTypeVisitor containsTypeVisitor;

  InterfacesVisitor interfacesVisitor;

  IsSameTypeVisitor isSameTypeVisitor;

  SubstituteVisitor substituteVisitor;

  private final Types2 types2;

  private final boolean capture;
  
  SubtypeVisitor(final Types2 types2, final boolean capture) {
    super(Boolean.FALSE);
    this.types2 = types2;
    this.capture = capture;
  }

  final SubtypeVisitor withCapture(final boolean capture) {
    final SubtypeVisitor subtypeVisitor = new SubtypeVisitor(this.types2, capture);
    subtypeVisitor.containsTypeVisitor = this.containsTypeVisitor;
    subtypeVisitor.interfacesVisitor = this.interfacesVisitor;
    subtypeVisitor.isSameTypeVisitor = this.isSameTypeVisitor;
    subtypeVisitor.substituteVisitor = this.substituteVisitor;
    return subtypeVisitor;
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
  public final Boolean visitError(final ErrorType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.ERROR;
    return true;
  }

  @Override
  public final Boolean visitIntersection(final IntersectionType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.INTERSECTION;
    // See
    // https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1191-L1192
    // and
    // https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1088-L1094
    throw new UnsupportedOperationException();
  }

  @Override
  public final Boolean visitNoType(final NoType t, final TypeMirror s) {
    final TypeKind kind = t.getKind();
    return kind == TypeKind.VOID && kind == s.getKind();
  }

  @Override
  public final Boolean visitNull(final NullType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.NULL;
    final TypeKind sKind = s.getKind();
    switch (sKind) {
    case ARRAY:
    case DECLARED:
    case NULL:
    case TYPEVAR:
      return true;
    default:
      return false;
    }
  }

  // See https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-4.10.1
  @Override
  public final Boolean visitPrimitive(final PrimitiveType t, final TypeMirror s) {
    final TypeKind tKind = t.getKind();
    final TypeKind sKind = s.getKind();
    switch (tKind) {
    case BOOLEAN:
    case DOUBLE:
      return tKind == sKind;
    case BYTE:
      switch (sKind) {
      case BYTE:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;
      default:
        return false;
      }
    case CHAR:
      switch (sKind) {
      case CHAR:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
        return true;
      default:
        return false;
      }
    case FLOAT:
      switch (sKind) {
      case DOUBLE:
      case FLOAT:
        return true;
      default:
        return false;
      }
    case INT:
      switch (sKind) {
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
        return true;
      }
    case LONG:
      switch (sKind) {
      case DOUBLE:
      case FLOAT:
      case LONG:
        return true;
      default:
        return false;
      }
    case SHORT:
      switch (sKind) {
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;
      default:
        return false;
      }
    default:
      return false;
    }
  }

  @Override
  public final Boolean visitTypeVariable(final TypeVariable t, final TypeMirror s) {
    assert t.getKind() == TypeKind.TYPEVAR;
    return this.withCapture(false).visit(t.getUpperBound(), s);
  }

  @Override
  public final Boolean visitWildcard(final WildcardType t, final TypeMirror s) {
    assert t.getKind() == TypeKind.WILDCARD;
    // See
    // https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L1129
    // and
    // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7034495
    // and
    // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7034922
    return false;
  }

  /*
  // "supers" here in the sense of a wildcard's super bound
  private final TypeMirror rewriteSupers(final TypeMirror t) {
    if (!this.types2.parameterized(t)) {
      return t;
    }
    List<TypeMirror> from = new ArrayList<>();
    List<TypeMirror> to = new ArrayList<>();
    adaptSelf(t, from, to);
    if (!from.isEmpty()) {
      final List<TypeMirror> rewrite = new ArrayList<>();
      boolean changed = false;
      for (final TypeMirror orig : to) {
        TypeMirror s = rewriteLowerBoundedWildcardTypes(orig); // RECURSIVE
        switch (s.getKind()) {
        case WILDCARD:
          if (((WildcardType)s).getSuperBound() != null) {
            // TODO: maybe need to somehow ensure this shows up as
            // non-canonical/synthetic
            s = unboundedWildcardType(s.getAnnotationMirrors());
            changed = true;
          }
          break;
        default:
          if (s != orig) { // Don't need Equality.equals() here
            // TODO: maybe need to somehow ensure this shows up as
            // non-canonical/synthetic
            s = upperBoundedWildcardType(wildcardExtendsBound(s), s.getAnnotationMirrors());
            changed = true;
          }
          break;
        case ERROR:
        case UNION:
          throw new IllegalStateException("s: " + s);
        }
        rewrite.add(s);
      }
      if (changed) {
        return subst(canonicalType(t), from, rewrite);
      }
    }
  }
  */
  
}
