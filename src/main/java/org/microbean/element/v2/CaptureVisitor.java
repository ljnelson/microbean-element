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
import java.util.Collections;
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

// javac's capture implementation is not visitor-based.
// https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4388-L4456
final class CaptureVisitor extends SimpleTypeVisitor14<TypeMirror, Void> {

  private final Types2 types2;

  private final SubstituteVisitor substituteVisitorPrototype;
  
  MemberTypeVisitor memberTypeVisitor;
  
  CaptureVisitor(final Types2 types2, final SupertypeVisitor supertypeVisitor) {
    super();
    this.types2 = Objects.requireNonNull(types2, "types2");
    this.substituteVisitorPrototype =
      new SubstituteVisitor(supertypeVisitor, List.of(), List.of());
  }

  @Override
  protected final TypeMirror defaultAction(final TypeMirror t, final Void x) {
    return t;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final TypeMirror visitDeclared(DeclaredType t, final Void x) {
    assert t.getKind() == TypeKind.DECLARED;

    TypeMirror enclosingType = t.getEnclosingType();
    if (enclosingType.getKind() != TypeKind.NONE) {
      assert enclosingType.getKind() == TypeKind.DECLARED;
      final TypeMirror capturedEnclosingType = this.visitDeclared((DeclaredType)enclosingType, x); // RECURSIVE
      if (capturedEnclosingType != enclosingType) {
        final Element element = t.asElement();
        final TypeMirror memberType = this.memberTypeVisitor.visit(capturedEnclosingType, element);
        t =
          (DeclaredType)this.substituteVisitorPrototype.with((List<? extends TypeVariable>)((DeclaredType)element.asType()).getTypeArguments(),
                                                             t.getTypeArguments())
          .visit(memberType, x);
        assert t.getKind() == TypeKind.DECLARED;
        enclosingType = t.getEnclosingType();
      }
    }

    if (this.types2.raw(t) || !this.types2.parameterized(t)) {
      return t;
    }

    final DeclaredType G = this.types2.declaredTypeMirror(t);
    @SuppressWarnings("unchecked")
    final List<? extends TypeVariable> A = (List<? extends TypeVariable>)G.getTypeArguments();
    final List<? extends TypeMirror> T = t.getTypeArguments();
    final List<? extends TypeMirror> S = withFreshCapturedTypeVariables(T);

    assert A.size() == T.size();
    assert A.size() == S.size();
    boolean captured = false;
    for (int i = 0; i < A.size(); i++) {
      final TypeVariable currentAHead = A.get(i);
      assert currentAHead.getKind() == TypeKind.TYPEVAR;
      final TypeMirror currentSHead = S.get(i);
      final TypeMirror currentTHead = T.get(i);
      if (currentSHead != currentTHead) {
        captured = true;
        TypeMirror Ui = currentAHead.getUpperBound();
        if (Ui == null) {
          Ui = ObjectConstruct.JAVA_LANG_OBJECT_TYPE;
        }
        final SyntheticCapturedType Si = (SyntheticCapturedType)currentSHead;
        final WildcardType Ti = (WildcardType)currentTHead;
        Si.setLowerBound(Ti.getSuperBound());
        final TypeMirror TiExtendsBound = Ti.getExtendsBound();
        if (TiExtendsBound == null) {
          // subst(Ui, A, S)
          Si.setUpperBound(this.substituteVisitorPrototype.with(A, S).visit(Ui));
        } else {
          // subst(Ui, A, S)
          Si.setUpperBound(glb(TiExtendsBound, this.substituteVisitorPrototype.with(A, S).visit(Ui)));
        }
      }
    }
    if (captured) {
      return syntheticDeclaredType(t, S);
    }
    
    throw new UnsupportedOperationException();
  }

  private static final List<? extends TypeMirror> withFreshCapturedTypeVariables(final List<? extends TypeMirror> typeArguments) {
    if (typeArguments.isEmpty()) {
      return List.of();
    }
    final List<TypeMirror> list = new ArrayList<>(typeArguments.size());
    for (final TypeMirror typeArgument : typeArguments) {
      switch (typeArgument.getKind()) {
      case WILDCARD:
        list.add(new SyntheticCapturedType((WildcardType)typeArgument));
        break;
      default:
        list.add(typeArgument);
        break;
      case ERROR:
      case UNION:
        throw new IllegalArgumentException("typeArguments: " + typeArguments);
      }
    }
    return Collections.unmodifiableList(list);
  }

  private static final DeclaredType syntheticDeclaredType(final DeclaredType canonicalType,
                                                          final List<? extends TypeMirror> typeArguments) {
    final DefaultDeclaredType t = new DefaultDeclaredType(canonicalType.getEnclosingType(), typeArguments, List.of());
    t.setDefiningElement(canonicalType.asElement());
    assert t.asElement().asType() == canonicalType;
    return t;
  }

  private static final TypeMirror glb(final TypeMirror t, final TypeMirror s) {
    throw new UnsupportedOperationException();
  }
  
}
